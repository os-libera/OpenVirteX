#!/usr/bin/env python

from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler
import sys
import json
import threading
import logging as log
import urllib2
from argparse import ArgumentParser
import subprocess
import time

CLONE_VM = '/usr/bin/VBoxManage clonevm OVX --snapshot Master --mode machine --options link --name %s --register'
GET_IP_VM = '/usr/bin/VBoxManage guestcontrol %s execute --image /home/ovx/get-ip.sh --wait-exit --username ovx --password ovx --wait-stdout -- eth0'
START_VM = '/usr/bin/VBoxManage startvm %s --type headless'
#START_VM = '/usr/bin/VBoxManage startvm %s'
STOP_VM = '/usr/bin/VBoxManage controlvm %s poweroff'
UNREGISTER_VM = '/usr/bin/VBoxManage unregistervm %s --delete'

class ERROR_CODE:
  PARSE_ERROR = -32700          # Invalid JSON was received by the server.
  INVALID_REQ = -32600          # The JSON sent is not a valid Request object.
  METHOD_NOT_FOUND = -32601     # The method does not exist / is not available.
  INVALID_PARAMS = -32602       # Invalid method parameter(s).
  INTERNAL_ERROR = -32603	      # Internal JSON-RPC error.
  INVALID_NETWORK_TYPE = 1

def parseDpid(mac):
  """Return int value of the string mac, which may be given as long value or as hex string"""
  if isinstance(mac, int):
    return mac
  if not ':' in mac:
    return int(mac)
  else:
    return int(mac.replace(':', ''), 16)

class Routing():
  def __init__(self, topology):
    try:
      self.nodes = topology['switches']
      self.links = {}
      for link in topology['links']:
        src = link['src']
        dst = link['dst']
        self.links[(src['dpid'], src['port'])] = (dst['dpid'], dst['port'])
      self.SP = {}
    except:
      pass
    
  def _neighbours(self, node):
    """Returns list of nodes that are neighbour to node.
      
    Assumes nodes are connected on at most one port, i.e., multigraphs are not supported (should
    delete duplicate entries from result.
    
    """
    return [dst_node for (src_node,src_port),(dst_node,dst_port) in self.links.iteritems() if src_node == node]

  def _shortestPath(self, src, dst):
    """Calculates shortest path between src and dst switches and stores it in the SP dict.
    
    Assumes graph is connected.
    
    """
    distance = {}
    previous = {}
    for node in self.nodes:
      distance[node] = sys.maxint
      distance[src] = 0
      # Sort Q according to distance
      Q = sorted(distance, key=distance.get)
      
    while Q:
      current = Q.pop(0)
      if distance[current] == sys.maxint:
        log.error("Graph is disconnected")
        # TODO: raise expection
        break
      for neighbour in self._neighbours(current):
        alt = distance[current] + 1
        if alt < distance[neighbour]:
          distance[neighbour] = alt
          previous[neighbour] = current
          # TODO: really should use a heap instead of resorting every time
          Q = sorted(distance, key=distance.get)
      # Path is between current and src (first iteration of outer while: current == src, previous[current] undefined)
      x = current
      path = []
      while previous.get(x) >= 0:
        path.append(x)
        x = previous[x]
      path.append(src)
      path.reverse()
      self.SP[(src, current)] = path

  def _findPorts(self, dpid1, dpid2):
    """Returns tuple (port_out, port_in) with port_out on dpid1 and port_in on dpid2, None if switches are not connected."""
    # Iterates over all links in worst case!
    for (dpid_out, port_out), (dpid_in, port_in) in self.links.iteritems():
      if (dpid1 == dpid_out) and (dpid2 == dpid_in):
        return (port_out, port_in)
    return None

  def _findOutPort(self, dpid1, dpid2):
    """Returns output port on dpid1 that connects to dpid2, None if switches are not connected."""
    return self._findPorts(dpid1, dpid2)[0]

  def _findInPort(self, dpid1, dpid2):
    """Returns input port on dpid2 that is connected to dpid1, None if switches are not connected."""
    return self._findPorts(dpid1, dpid2)[1]

  def getRoute(self, dpid_in, dpid_out):
    """Find route between dpid_in and dpid_out.

    Route is of form [ dpid ]

    """
    # Catch trivial path
    if dpid_in == dpid_out:
      return [ (dpid_in) ]

    # Calculate path
    if (dpid_in, dpid_out) not in self.SP.keys():
      self._shortestPath(dpid_in, dpid_out)

    route = self.SP[(dpid_in, dpid_out)]

    return route

  def parseRoute(self, route):
    """Parse route specified and return OVX-type path string.
    
    Input route is of form [ dpid ], while return path is of form dpid1/port1-dpid2/port2,...
    
    """
    path = ''
    for index in xrange(0, len(route) - 1):
      outPort = self._findOutPort(route[index], route[index + 1])
      inPort = self._findInPort(route[index], route[index + 1])
      path += "%s/%s-%s/%s," % (parseDpid(route[index]), outPort, parseDpid(route[index + 1]), inPort)
    # Remove final comma
    return path[:-1]
  
class OVXClient():
  def __init__(self, host, port, user, password):
    self.host = host
    self.port = port
    self.user = user
    self.password = password
    self.url = "http://%s:%s/tenant" % (self.host, self.port)
  
  def _buildRequest(self, data, cmd):
    j = { "id" : "ovxembedder", "method" : cmd, "jsonrpc" : "2.0" }
    h = {"Content-Type" : "application/json"}
    if data is not None:
      j['params'] = data
    return urllib2.Request(self.url, json.dumps(j), h)

  def _parseResponse(self, data):
    j = json.loads(data)
    if 'error' in j:
      log.error("%s (%s)" % (j['error']['message'], j['error']['code']))
      sys.exit(1)
    return j['result']

  def _connect(self, cmd, data=None):
    try:
      passman = urllib2.HTTPPasswordMgrWithDefaultRealm()
      passman.add_password(None, self.url, self.user, self.password)
      authhandler = urllib2.HTTPBasicAuthHandler(passman)
      opener = urllib2.build_opener(authhandler)
      req = self._buildRequest(data, cmd)
      #ph = urllib2.urlopen(req)
      ph = opener.open(req)
      return self._parseResponse(ph.read())
    except urllib2.URLError as e:
      log.error(e)
      sys.exit(1)
    except urllib2.HTTPError as e:
      if e.code == 401:
        log.error("Authentication failed: invalid password")
        # TODO
        sys.exit(1)
      elif e.code == 504:
        log.error("HTTP Error 504: Gateway timeout")
        # TODO
        sys.exit(1)
      else:
        log.error(e)
    except RuntimeError as e:
      log.error(e)

  def createNetwork(self, protocol, host, port, net_address, net_mask):
    req = {'protocol': protocol, 'controllerAddress': host, 'controllerPort': port,
           'networkAddress': net_address, 'mask': net_mask}
    ret = self._connect("createNetwork", data=req)
    if ret:
        log.info("Network with tenantId %s has been created" % ret)
    return ret

  def createSwitch(self, tenantId, dpids):
    req = {'tenantId': tenantId, 'dpids': dpids}
    ret = self._connect("createSwitch", data=req)
    if ret:
        log.info("Switch with switchId %s has been created" % ret)
    return ret

  def createLink(self, tenantId, path):
    req = {'tenantId': tenantId, 'path': path}
    ret = self._connect("createLink", data=req)
    if ret:
        log.info("Link with linkId %s has been created" % ret)
    return ret

  def connectHost(self, tenantId, dpid, port, mac):
    req = {'tenantId': tenantId, 'dpid': dpid, 'port': port, 'mac': mac}
    ret = self._connect("connectHost", data=req)
    if ret:
        log.info("Host %s connected on port %s" % (mac, ret))
    return ret

  def createSwitchRoute(self, tenantId, switchId, srcPort, dstPort, path):
    req = {'tenantId': tenantId, 'dpid': switchId, 'srcPort': srcPort, 'dstPort': dstPort, 'path': path}
    ret = self._connect("createSwitchRoute", data=req)
    if ret:
      log.info("Route on switch %s between ports (%s,%s) created" % (switchId, srcPort, dstPort))
    return ret

  def startNetwork(self, tenantId):
    req = {'tenantId': tenantId}
    ret = self._connect("startNetwork", data=req)
    if ret:
        log.info("Network with tenantId %s has been started" % tenantId)
    return ret

  def getPhysicalTopology(self):
    ret = self._connect("getPhysicalTopology")
    if ret:
        log.info("Physical network topology received")
    return ret

class OVXEmbedderHandler(BaseHTTPRequestHandler):
  """
  Implementation of JSON-RPC API, defines all API handler methods.
  """
  
  def _buildResponse(self, json_id, result=None, error=None):
    """Returns JSON 2.0 compliant response"""
    res = {}
    res['jsonrpc'] = '2.0'
    # result and error are mutually exclusive
    if result is not None:
      res['result'] = result
    elif error is not None:
      res['error'] = error
    res['id'] = json_id
    return res

  def _buildError(self, code, message, data=None):
    """Returns JSON RPC 2.0 error object"""
    res = {}
    res['code'] = code
    res['message'] = message
    if data:
      res['data'] = data
    return res

  def doBigSwitchNetwork(self, controller, subnet, hosts):
    client = self.server.client
    # request physical topology
    phyTopo = json.loads(client.getPhysicalTopology())
    # spawn controller if necessary
    # TODO: do proper string comparison
    if controller['type'] == 'default':
      proto = self.server.ctrlProto
      host = self.server._spawnController()
      port = self.server.ctrlPort
    elif controller['type'] == 'custom':
      proto = controller['protocol']
      host = controller['host']
      port = int(controller['port'])
    else:
      log.error('Unsupported controller type')
      sys.exit(1)
    # split subnet in netaddress and netmask
    (net_address, net_mask) = subnet.split('/')
    # create virtual network
    tenantId = client.createNetwork(proto, host, port, net_address, int(net_mask))
    # create virtual switch with all physical dpids
    dpids = [parseDpid(dpid) for dpid in phyTopo['switches']]
    switchId = client.createSwitch(tenantId, dpids)
    # add hosts and save their port numbers
    hostPortMap = {}
    for host in hosts:
      hostPortMap[host['mac']] = client.connectHost(tenantId, parseDpid(host['dpid']), host['port'], host['mac'])
    # calculate routing and configure virtual switch
    routing = Routing(phyTopo)
    for src_index in xrange(0, len(hosts)):
      src = hosts[src_index]
      for dst_index in xrange(src_index + 1, len(hosts)):
        dst = hosts[dst_index]
        route = routing.getRoute(src['dpid'], dst['dpid'])
        path = routing.parseRoute(route)
        srcPort = parseDpid(hostPortMap[src['mac']])
        dstPort = parseDpid(hostPortMap[dst['mac']])
        client.createSwitchRoute(tenantId, switchId, srcPort, dstPort, path)
    # boot network
    client.startNetwork(tenantId)

    return tenantId

  def doPhysicalNetwork(self, controller, subnet, hosts):
    client = self.server.client
    # request physical topology
    phyTopo = json.loads(client.getPhysicalTopology())
    # spawn controller if necessary
    if controller['type'] == 'default':
      proto = self.server.ctrlProto
      host = self.server._spawnController()
      port = self.server.ctrlPort
    elif controller['type'] == 'custom':
      proto = controller['protocol']
      host = controller['host']
      port = int(controller['port'])
    else:
      log.error('Unsupported controller type')
      sys.exit(1)
    # split subnet in netaddress and netmask
    (net_address, net_mask) = subnet.split('/')
    # create virtual network
    tenantId = client.createNetwork(proto, host, port, net_address, int(net_mask))
    # create virtual switch per physical dpid
    for dpid in phyTopo['switches']:
      client.createSwitch(tenantId, [parseDpid(dpid)])
    # add hosts
    for host in hosts:
      client.connectHost(tenantId, parseDpid(host['dpid']), host['port'], host['mac'])
    # create virtual link per physical link
    connected = []
    for link in phyTopo['links']:
      if (link['src']['dpid'], link['src']['port']) not in connected:
        src = "%s/%s" % (parseDpid(link['src']['dpid']), link['src']['port'])
        dst = "%s/%s" % (parseDpid(link['dst']['dpid']), link['dst']['port'])
        path = "%s-%s" % (src, dst)
        client.createLink(tenantId, path)
        connected.append((link['dst']['dpid'], link['dst']['port']))
    # boot network
    client.startNetwork(tenantId)

    return tenantId

  def _exec_createNetwork(self, json_id, params):
    """Handler for automated network creation"""

    p = params['network']
    tenantId = -1
    
    # check type
    if p['type'] == 'bigswitch':
      tenantId = self.doBigSwitchNetwork(p['controller'], p['subnet'], p['hosts'])
    elif p['type'] == 'physical':
      tenantId = self.doPhysicalNetwork(p['controller'], p['subnet'], p['hosts'])
    elif p['type'] == 'custom':
      pass
    else:
      msg = 'Unsupported network type'
      log.error(msg)
      err = self._buildError(ERROR_CODE.INVALID_NETWORK_TYPE, msg)
      return self._buildResponse(json_id, error=err)
    
    response = self._buildResponse(json_id, result={ 'tenantId' : tenantId })
    return response

  def do_POST(self):
    """Handle HTTP POST calls"""

    def reply(response):
      response = json.dumps(response) + '\n'
      self.send_response(200, "OK")
      self.send_header("Content-Type", "application/json")
      self.send_header("Content-Length", len(response))
      self.end_headers()
      self.wfile.write(response)
    
    # Put JSON message in data dict
    l = self.headers.get("Content-Length", "")
    data = ''
    if l == "":
      data = self.rfile.read()
    else:
      data = self.rfile.read(int(l))
    try:
      data = json.loads(data)
    except:
      msg = "Error parsing JSON request"
      log.error(msg)
      err = self._buildError(ERROR_CODE.PARSE_ERROR, msg)
      result = self._buildResponse(None, error=err)
    # Check if JSONRPC 2.0 compliant (correct version and json_id given)
    json_id = data.get('id', None)
    # Setup method to call
    try:
      methodName = "_exec_" + data.get('method')
      method = getattr(self, methodName)
      log.info(methodName)
    except:
      msg = "Method not found"
      log.info(msg)
      err = self._buildError(ERROR_CODE.METHOD_NOT_FOUND, msg)
      result = self._buildResponse(json_id, error=err)
    # Get method parameters
    params = data.get('params', {})
    # Call method
    result = method(json_id, params)

    reply(result)

class OVXEmbedderServer(HTTPServer):
  def __init__(self, opts):
    HTTPServer.__init__(self, (opts['host'], opts['port']), OVXEmbedderHandler)
    self.client = OVXClient(opts['ovxhost'], opts['ovxport'], opts['ovxuser'], opts['ovxpass'])
    self.ctrlProto = opts['ctrlproto']
    self.ctrlPort = opts['ctrlport']
    self.controllers = []

  def _spawnController(self):
    ctrl = "OVX-%s" % len(self.controllers)
    devnull = open('/dev/null', 'w')
    log.info("Spawning controller VM %s" % ctrl)
    clone_cmd = CLONE_VM % ctrl
    subprocess.call(clone_cmd.split(), stdout=devnull, stderr=devnull)
    start_cmd = START_VM % ctrl
    subprocess.call(start_cmd.split(), stdout=devnull, stderr=devnull)
    get_ip_cmd = GET_IP_VM % ctrl
    while True:
      try:
        ret = subprocess.check_output(get_ip_cmd.split(), stderr=devnull)
      except subprocess.CalledProcessError:
        time.sleep(1)
        continue
      ip = ret
      break
    self.controllers.append(ctrl)
    log.info("Controller %s ready on %s" % (ctrl, ip))
    return ip
    
  def closeControllers(self):
    for controller in self.controllers:
      stop_cmd = STOP_VM % controller
      subprocess.call(stop_cmd.split())
      del_cmd = UNREGISTER_VM % controller
      subprocess.call(del_cmd.split())
    
class OVXEmbedder(threading.Thread):
  """
  OpenVirteX planner JSON RPC 2.0 server
  """
  def __init__(self, opts):
    threading.Thread.__init__(self)
    self.httpd = OVXEmbedderServer(opts)
    self.setDaemon(True)
    
  # Multi-threaded webserver
  def run(self):
    """
    Main function run by thread
    """
    log.info("JSON RPC server starting")
    try:
      self.httpd.serve_forever()
    finally:
      self.httpd.server_close()
      self.httpd.closeControllers()

if __name__ == '__main__':
  parser = ArgumentParser(description="OpenVirteX network embedding tool.")
  parser.add_argument('--host', default='localhost', help='OpenVirteX embedder host (default="localhost")')
  parser.add_argument('--port', default=8000, type=int, help='OpenVirteX embedder port (default="8000")')
  parser.add_argument('--ovxhost', default='localhost', help='Host where OpenVirteX is running (default="localhost")')
  parser.add_argument('--ovxport', default=8080, type=int, help='Port where OpenVirteX is running (default="8080")')
  parser.add_argument('--ovxuser', default='tenant', help='OpenVirteX user (default="tenant")')
  parser.add_argument('--ovxpass', default='tenant', help='OpenVirteX password (default="tenant")')
  parser.add_argument('--ctrlproto', default='tcp', help='Default controller protocol (default="tcp")')
  parser.add_argument('--ctrlport', default=10001, type=int, help='Default controller port (default="10001")')
  parser.add_argument('--version', action='version', version='%(prog)s 0.1')
  args = parser.parse_args()
  
  log.basicConfig(format='%(asctime)s %(message)s', level=log.INFO)
  embedder = OVXEmbedder(vars(args))
  embedder.run()
