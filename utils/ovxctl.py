#!/usr/bin/env python

# OpenVirteX control script
# Heavily based on FlowVisor's fvctl

#import local thrift files
import TenantServer
from ttypes import *

#import Thrift packages to connect to server
from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol

#import python utilities to parse arguments
import sys
from optparse import OptionParser
import urllib2

VERSION = '0.1'

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
    client = create_client(gopts.host, int(gopts.port))
    network_id = client.createVirtualNetwork(args[0], args[1], int(args[2]), args[3], int(args[4]))
    client._iprot.trans.close()
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
    client = create_client(gopts.host, int(gopts.port))
    linkId = client.createVirtualLink(int(args[0]), args[1])
    client._iprot.trans.close()
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
    client = create_client(gopts.host, int(gopts.port))
    dpids = [str(dpid) for dpid in args[1].split(',')]
    dpid = client.createVirtualSwitch(int(args[0]), dpids)
    client._iprot.trans.close()
    if dpid:
        print "Virtual switch has been created (dpid %s)" % dpid

def pa_connectHost(args, cmd):
    usage = "%s <mac> <dpid> <port>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_connectHost(gopts, opts, args):
    if len(args) != 4:
        print "connectHost : Must specify a tenantId, dpid, port and MAC address"
        sys.exit()
    client = create_client(gopts.host, int(gopts.port))
    # takes the tenantid, dpid, port, host mac address
    port = client.createHost(int(args[0]), args[1], int(args[2]), args[3])
    client._iprot.trans.close()
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
    client = create_client(gopts.host, int(gopts.port))
    result = client.startNetwork(int(args[0]))
    client._iprot.trans.close()
    if result:
        print "Network has been booted"
        
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

def printVersion (option, opt, value, parser):
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
    'createVSwitch': (pa_vswitch, do_createVSwitch),
    'createVLink': (pa_vlink, do_createVLink),
    'connectHost': (pa_connectHost, do_connectHost),
    'bootNetwork': (pa_bootNetwork, do_bootNetwork),
    'help' : (pa_help, do_help)
}

DESCS = {
    'createNetwork' : ("Creates a virtual network",
                       ("Creates a virtual network. Input: protocol, controllerIP, controller port, ip address, mask ")),
    'createVLink' : ("Create virtual link",
                  ("Create virtual link. Must specify a network_id and hops in the physical plane. srcDPID/port-dstDPID/port,srcDPID/port-dstDPID/port")),
    'createVSwitch' : ("Create virtual switch",
                     ("Create a virtual switch. Must specify a network_id, and a list of the physicalDPIDs that this contains")),
    'connectHost' : ("Connect host to edge switch",
                     ("Connect host to edge switch. Must specify a network_id, mac, dpid and port.")),
    'bootNetwork' : ("Boot virtual network",
                     ("Boot virtual network. Must specify a network_id.")),
}

USAGE="%prog {}"

URL = "http://%s:%s"

def addCommonOpts (parser):
    parser.add_option("-h", "--hostname", dest="host", default="localhost",
                    help="Specify the OpenVirteX host; default='localhost'")
    parser.add_option("-p", "--port", dest="port", default="8080",
                    help="Specify the OpenVirteX web port; default=8080")
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


def create_client(host, port):
    #Make socket
    transport = TSocket.TSocket(host, port)

    # Buffering is critical. Raw sockets are very slow
    transport = TTransport.TFramedTransport(transport)
    # Wrap in a protocol
    protocol = TBinaryProtocol.TBinaryProtocol(transport)

    # Create a client to use the protocol encoder
    client = TenantServer.Client(protocol)
    # Connect!
    transport.open()

    return client

if __name__ == '__main__':
  try:
    (gopts, rargs, parser) = parse_global_args(sys.argv[1:])

    if len(rargs) < 1:
        raise IndexError
    (parse_args, do_func) = CMDS[rargs[0]]
    (opts, args) = parse_args(rargs[1:], rargs[0])
    do_func(gopts, opts, args)
  except ValueError, e:
    print "the argument types being sent to the function %s are incorrect. Please double check them." % sys.argv[-1]
    function = sys.argv[1]
    if function=='createNetwork':
      print "createNetwork - string, string, short, string, short"
    elif function=='createVSwitch':
      print "createVSwitch - int, list<string>"
    elif function=='connectHost':
      print "connectHost - int, string, short, string"
    elif function=='createVLink':
      print "createVLink - int, string"
    elif function=='bootNetwork':
      pass
  except IndexError, e:
    print "%s is an unknown command" % sys.argv[-1]
    printHelp(None, None, None, parser)
