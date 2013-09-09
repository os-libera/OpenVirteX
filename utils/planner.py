from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler
import sys
import json
import threading
import logging as log
import copy
import socket
import struct

class ERROR_CODE:
  PARSE_ERROR = -32700          # Invalid JSON was received by the server.
  INVALID_REQ = -32600          # The JSON sent is not a valid Request object.
  METHOD_NOT_FOUND = -32601     # The method does not exist / is not available.
  INVALID_PARAMS = -32602       # Invalid method parameter(s).
  INTERNAL_ERROR = -32603	      # Internal JSON-RPC error.
  INVALID_NETWORK_TYPE = 1

class OVXClient():
  def createNetwork(self, protocol, host, port, subnet):
    pass

  def createSwitch(self, tenantId, dpids):
    pass

  def createLink(self, tenantId, path):
    pass

  def createHost(self, tenantId, dpid, portNumber, mac):
    pass

  def startNetwork(self, tenantId):
    pass

  def getPhysicalTopology(self):
    # get ovx config from db
    # curl to ovx
    # json load result
    pass

class OVXPlannerAPIHandler(BaseHTTPRequestHandler):
  """
  Implementation of JSON-RPC API, defines all API handler methods.
  """
  def __init__(self):
    client = OVXClient()
  
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

  def doBigSwitchNetwork(self, proto, host, port, subnet, hosts):
    # request physical topology
    phyTopo = json.loads(client.getPhysicalTopology())
    # create virtual network
    response = client.createNetwork(proto, host, port, subnet)
    tenantId = json.loads(response)['result']
    # create virtual switch with all physical dpids
    client.createSwitch(tenantId, phyTopo['switches'])
    # add hosts
    for host in hosts:
      client.createHost(tenantId, host['dpid'], host['portNumber'], host['mac'])
    # TODO: calculate routing and configure switch through createVirtualSwitchRoute
    # boot network
    client.startNetwork(tenantId)

  def doPhysicalNetwork(self, proto, host, port, subnet, hosts):
    # request physical topology
    phyTopo = json.loads(client.getPhysicalTopology())
    # create virtual network
    response = client.createNetwork(proto, host, port, subnet)
    tenantId = json.loads(response)['result']
    # create virtual switch per physical dpid
    for dpid in phyTopo['switches']:
      client.createSwitch(tenantId, dpid)
    # create virtual link per physical link
    for link in phyTopo['links']:
      path = link[src][dpid] + '/' + link[src][port] + '-'
      path += link[dst][dpid] + '/' + link[dst][port]
      client.createLink(tenantId, path)
    # add hosts
    for host in hosts:
      client.createHost(tenantId, host['dpid'], host['portNumber'], host['mac'])
    # boot network
    client.startNetwork(tenantId)

  def _exec_createNetwork(self, json_id, params):
    """Handler to create a network"""

    p = params['network']

    # check type
    if p['type'] == 'bigswitch':
      doBigSwitchNetwork()
    elif p['type'] == 'physical':
      doPhysicalNetwork()
    elif p['type'] == 'custom':
      pass
    else:
      msg = 'Unsupported network type'
      log.error(msg)
      err = self._buildError(ERROR_CODE.INVALID_NETWORK_TYPE, msg)
      return self._buildResponse(json_id, error=err)
    
    # Pass information to OVX and call createNetwork
    # network_id = core.NetVisor.createNetwork(pox_mac_address, primary_controller, ip_range)
    # response = self._buildResponse(json_id, result={ 'network_id' : network_id })

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
      log.info(msg)
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

class OVXPlanner (threading.Thread):
  """
  OpenVirteX planner JSON RPC server
  """
  def __init__(self, address="localhost", port="8000"):
    threading.Thread.__init__(self)
    self.httpd = HTTPServer((address, int(port)), OVXPlannerAPIHandler)
    self.setDaemon(True)
    
  # Multi-threaded webserver
  def run(self):
    """
    Main function run by thread
    """
    log.info("JSON RPC handler invoked")
    try:
      self.httpd.serve_forever()
    finally:
      self.httpd.server_close()


log.basicConfig(format='%(asctime)s %(message)s')
address = sys.argv[1]
# TODO: check exception
port = int(sys.argv[2])
planner = OVXPlanner(address, port)
planner.run()
