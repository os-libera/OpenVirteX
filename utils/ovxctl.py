#!/usr/bin/env python

# OpenVirteX control script
# Heavily based on FlowVisor's fvctl


#import python utilities to parse arguments
import sys
from optparse import OptionParser
import urllib2
import json
import getpass

VERSION = '0.1'

def getUrl(opts):
    return URL % (opts.host, opts.port)

def buildRequest(data, url, cmd):
    j = { "id" : "ovxctl",  "method" : cmd , "jsonrpc" : "2.0" }
    h = {"Content-Type" : "application/json-rpc"}  
    if data is not None:
        j['params'] = data
    return urllib2.Request(url, json.dumps(j), h)
 

def pa_none(args, cmd):
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=USAGE.format(cmd), description=ldesc)
    (options, args) = parser.parse_args(args)
    return (options, args)

def pa_createNetwork(args, cmd):
    usage = "%s <mac_address> <primary_controller> <ip_range>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)

    return parser.parse_args(args)

def do_createNetwork(gopts, opts, args):
    if len(args) != 5:
        print "createNetwork : Must specify protocol, controllerIP, controllerPort, networkIP, mask"
        sys.exit()
    req = { "protocol" : args[0], "controllerAddress" : args[1], "controllerPort" : int(args[2]), \
                 "networkAddress" : args[3], "mask" : int(args[4]) }
    network_id = connect(gopts, "createNetwork", data=req, passwd=getPasswd(gopts))
    if network_id:
        print "Network has been created (network_id %s)." % str(network_id)

def pa_vlink(args, cmd):
    usage = "%s <network_id> <dpid> <ports>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)

    return parser.parse_args(args)

def do_createVLink(gopts, opts, args):
    if len(args) != 2:
        print "createVLink : Must specify a (network_id, and a path string of all the physicalLinks that create a virtualLink)"
        sys.exit()
    req = { "tenantId" : int(args[0]), "path" : args[1] }
    linkId = connect(gopts, "createLink", data=req, passwd=getPasswd(gopts))
    if linkId:
        print "Virtual link has been created"

def pa_vswitch(args, cmd):
    usage = "%s <network_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_createVSwitch(gopts, opts, args):
    if len(args) != 2:
        print "createVSwitch : Must specify (network_id and dpid,dpid,... - list of physical dpids which are associated with this dpid)"
        sys.exit()
    dpids = [int(dpid) for dpid in args[1].split(',')]
    req = { "tenantId" : int(args[0]), "dpids" : dpids }  
    dpid = connect(gopts, "createSwitch", data=req, passwd=getPasswd(gopts)) 
    if dpid:
        print "Virtual switch has been created (dpid %s)" % dpid

def pa_vroute(args, cmd):
    usage = "%s <network_id> <dpid> <src_port> <dst_port> <physical_path>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_createVRoute(gopts, opts, args):
    if len(args) != 5:
        print "createRoute : Must specify a tenantId, dpid, port pair and physical path"
        sys.exit()
    req = { "tenantId" : int(args[0]), "dpid" : int(args[1]), "srcPort" : int(args[2]), "dstPort" : int(args[3]), "path" : args[4] }
    routeId = connect(gopts, "createSwitchRoute", data=req, passwd=getPasswd(gopts))
    if routeId:
        print "BigSwitch route has been created"

def pa_connectHost(args, cmd):
    usage = "%s <mac> <dpid> <port>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_connectHost(gopts, opts, args):
    if len(args) != 4:
        print "connectHost : Must specify a tenantId, dpid, port and MAC address"
        sys.exit()
    req = { "tenantId" : int(args[0]), "dpid" : int(args[1]), "port" : int(args[2]), "mac" : args[3] } 
    port = connect(gopts, "connectHost", data=req, passwd=getPasswd(gopts)) 
    if port:
        print "Host has been connected to edge"

def pa_bootNetwork(args, cmd):
    usage = "%s <network_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_bootNetwork(gopts, opts, args):
    if len(args) != 1:
        print "bootNetwork : Must specify a network/tenant ID"
        sys.exit()
    req = { "tenantId" : int(args[0]) }
    result = connect(gopts, "startNetwork", data=req, passwd=getPasswd(gopts)) 
    if result:
        print "Network has been booted"
        
def pa_removeNetwork(args, cmd):
    usage = "%s <network_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_removeNetwork(gopts, opts, args):
    if len(args) != 1:
        print "removeNetwork : Must specify a network/tenant ID"
        sys.exit()
    req = { "tenantId" : int(args[0]) }
    result = connect(gopts, "removeNetwork", data=req, passwd=getPasswd(gopts)) 
    if result:
        print "Network has been removed"        
        
def pa_removeSwitch(args, cmd):
    usage = "%s <network_id> <switch_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_removeSwitch(gopts, opts, args):
    if len(args) != 2:
        print "removeSwitch : Must specify a network/tenant ID and a switch ID"
        sys.exit()
    req = { "tenantId" : int(args[0]), "dpid" : int(args[1]) }
    result = connect(gopts, "removeSwitch", data=req, passwd=getPasswd(gopts)) 
    if result:
        print "Switch has been removed"      

def pa_removeLink(args, cmd):
    usage = "%s <network_id> <link_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_removeLink(gopts, opts, args):
    if len(args) != 2:
        print "removeLink : Must specify a network/tenant ID and a link ID"
        sys.exit()
    req = { "tenantId" : int(args[0]), "link" : int(args[1]) }
    result = connect(gopts, "removeLink", data=req, passwd=getPasswd(gopts)) 
    if result:
        print "Link has been removed"  
        
def pa_disconnectHost(args, cmd):
    usage = "%s <mac> <dpid> <port>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_disconnectHost(gopts, opts, args):
    if len(args) != 4:
        print "disconnectHost : Must specify a tenantId, dpid, port and MAC address"
        sys.exit()
    req = { "tenantId" : int(args[0]), "dpid" : int(args[1]), "port" : int(args[2]), "mac" : args[3] } 
    port = connect(gopts, "disconnectHost", data=req, passwd=getPasswd(gopts)) 
    if port:
        print "Host has been disconnected from edge"
        
def pa_removeSwitchRoute(args, cmd):
    usage = "%s <network_id> <route_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_removeSwitchRoute(gopts, opts, args):
    if len(args) != 2:
        print "removeSwitchRoute : Must specify a network/tenant ID and a route ID"
        sys.exit()
    req = { "tenantId" : int(args[0]), "switch_route" : int(args[1]) }
    result = connect(gopts, "removeSwitchRoute", data=req, passwd=getPasswd(gopts)) 
    if result:
        print "Route has been removed"

def pa_stopNetwork(args, cmd):
    usage = "%s <network_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_stopNetwork(gopts, opts, args):
    if len(args) != 1:
        print "stopNetwork : Must specify a network/tenant ID"
        sys.exit()
    req = { "tenantId" : int(args[0]) }
    result = connect(gopts, "stopNetwork", data=req, passwd=getPasswd(gopts)) 
    if result:
        print "Network has been shutdown"
        
def pa_help(args, cmd):
    usage = "%s <cmd>" % USAGE.format(cmd)
    parser = OptionParser(usage=usage)
    return parser.parse_args(args)

def do_help(gopts, opts, args):
    if len(args) != 1:
        raise IndexError
    try:
        (pa, func) = CMDS[args[0]]
        pa(['--help'], args[0])
    except KeyError, e:
        print "Invalid command : %s is an unknown command." % args[0]
        sys.exit()


def connect(opts, cmd, data=None, passwd=None):
    try:
        url = getUrl(opts)
        passman = urllib2.HTTPPasswordMgrWithDefaultRealm()
        passman.add_password(None, url, opts.ovx_user, passwd)
        authhandler = urllib2.HTTPBasicAuthHandler(passman)
        opener = urllib2.build_opener(authhandler)
        req = buildRequest(data, url, cmd)
        #ph = urllib2.urlopen(req)
        ph = opener.open(req)
        return parseResponse(ph.read())
    except urllib2.URLError as e:
        print e
        sys.exit(1)
    except urllib2.HTTPError as e:
        if e.code == 401:
            print "Authentication failed: invalid password"
            sys.exit(1)
        elif e.code == 504:
            print "HTTP Error 504: Gateway timeout"
            sys.exit(1)
        else:
            print e
    except RuntimeError as e:
        print e

def parseResponse(data):
    j = json.loads(data)
    if 'error' in j:
        print j
        sys.exit(1)
    return j['result']



def printVersion(option, opt, value, parser):
    """Print nvctl version and exit"""
    print "nvctl-%s" % VERSION
    sys.exit()

def printHelp (option, opt, value, parser):
    """Print nvctl help and exit"""
    cmds = [x for x in CMDS.iterkeys()]
    cmds.remove('help')
    cmds.sort()
    print(parser.format_help().strip())
    print "\n Available commands are: "
    for x in cmds:
      (sdesc, ldesc) = DESCS[x]
      print "   {0:25}     {1:10}".format(x, sdesc)
    print "\n See '%s help <command>' for more info on a specific command." % sys.argv[0]
    sys.exit()

CMDS = {
    'createNetwork': (pa_createNetwork, do_createNetwork),
    'createSwitch': (pa_vswitch, do_createVSwitch),
    'createLink': (pa_vlink, do_createVLink),
    'connectHost': (pa_connectHost, do_connectHost),
    'createSwitchRoute': (pa_vroute, do_createVRoute),
    'startNetwork': (pa_bootNetwork, do_bootNetwork),
    'removeNetwork': (pa_removeNetwork, do_removeNetwork),
    'removeSwitch': (pa_removeSwitch, do_removeSwitch),
    'removeLink': (pa_removeLink, do_removeLink),
    'disconnectHost': (pa_disconnectHost, do_disconnectHost),
    'removeSwitchRoute': (pa_removeSwitchRoute, do_removeSwitchRoute),
    'stopNetwork': (pa_stopNetwork, do_stopNetwork),
    'help' : (pa_help, do_help)
}

DESCS = {
    'createNetwork' : ("Creates a virtual network",
                       ("Creates a virtual network. Input: protocol, controllerIP, controller port, ip address, mask ")),
    'createLink' : ("Create virtual link",
                  ("Create virtual link. Must specify a network_id and hops in the physical plane. srcDPID/port-dstDPID/port,srcDPID/port-dstDPID/port")),
    'createSwitch' : ("Create virtual switch",
                     ("Create a virtual switch. Must specify a network_id, and a list of the physicalDPIDs that this contains")),
    'connectHost' : ("Connect host to edge switch",
                     ("Connect host to edge switch. Must specify a network_id, mac, dpid and port.")),
    'createSwitchRoute': ("Create the route inside a bigswitch", 
		     ("Create a route. Must provide a network_id , a virtual switch_id, the source and destination virtual port_ids and the physical path.")), 
    'startNetwork' : ("Boot virtual network",
                     ("Boot virtual network. Must specify a network_id.")),
    'removeNetwork' : ("Remove a virtual network",
                     ("Remove a virtual network. Must specify a network_id.")),
    'removeSwitch' : ("Remove virtual switch",
                     ("Remove a virtual switch. Must specify a network_id and a virtual switch_id.")),
    'removeLink' : ("Remove virtual link",
                     ("Remove a virtual link. Must specify a network_id and a virtual switch_id.")),
    'disconnectHost' : ("Disconnect host from edge switch",
                     ("Disconnect host from edge switch. Must specify a network_id, a physical switch_id, a physical port_id and a mac_address.")),
    'removeSwitchRoute' : ("Remove a route inside a bigswitch",
                     ("Delete a route. Must provide a network_id , a virtual switch_id, the source and destination virtual port_ids.")),
    'stopNetwork' : ("Stop virtual network",
                     ("Stop virtual network. Must specify a network_id.")),
}

USAGE="%prog {}"

URL = "https://%s:%s/tenant"

def getPasswd(opts):
    if opts.no_passwd:
        return ""
    else:
        return getpass.getpass("Password: ") 

def addCommonOpts (parser):
    parser.add_option("-h", "--hostname", dest="host", default="localhost",
                    help="Specify the OpenVirteX host; default='localhost'")
    parser.add_option("-p", "--port", dest="port", default="8443",
                    help="Specify the OpenVirteX web port; default=8443")
    parser.add_option("-u", "--user", dest="ovx_user", default="admin", 
                    help="OpenVirtex admin user; default='admin'")
    parser.add_option("-n", "--no-passwd", action="store_true",  dest="no_passwd", default=False,
                    help="Run ovxctl with no password; default false") 
    parser.add_option("-v", "--version", action="callback", callback=printVersion)
    parser.add_option("--help", action="callback", callback=printHelp)

def parse_global_args (arglist):
    usage = "%s [options] command [command_args]" % sys.argv[0]
    args = []
    while (len(arglist) != 0 and arglist[0] not in CMDS):
        args.append(arglist[0])
        arglist.pop(0)
    parser = OptionParser(add_help_option=False, usage=usage)
    addCommonOpts(parser)
    (opts, pargs) = parser.parse_args(args)
    return (opts, arglist, parser)


if __name__ == '__main__':
  try:
    (gopts, rargs, parser) = parse_global_args(sys.argv[1:])

    if len(rargs) < 1:
      printHelp(None, None, None, parser)      
    (parse_args, do_func) = CMDS[rargs[0]]
    (opts, args) = parse_args(rargs[1:], rargs[0])
    do_func(gopts, opts, args)
  except ValueError, e:
    print "the argument types being sent to the function %s are incorrect. Please double check them." % sys.argv[1]
    function = sys.argv[1]
    if function=='createNetwork':
      print "createNetwork: string, string, short, string, short"
    elif function=='createSwitch':
      print "createVSwitch: int, list<string>"
    elif function=='connectHost':
      print "connectHost: int, string, short, string"
    elif function=='createLink':
      print "createVLink: int, string"
    elif function=='createRoute':
      print "createVRoute: int, int, int, int, string"
    elif function=='startetwork':
      print "bootNetwork: int"
  except IndexError, e:
    print "%s is an unknown command" % sys.argv[-1]
    printHelp(None, None, None, parser)
  except Exception, e:
    print "uknown error"
    print e
