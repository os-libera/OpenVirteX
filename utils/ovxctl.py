#!/usr/bin/env python

# NetVisor control script
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

import ast

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

def do_createNetwork(client, gopts, opts, args):
    if len(args) != 5:
        print "createNetwork : Must specify protocol, controllerIP, controllerPort, networkIP, mask"
        sys.exit()
    network_id = client.createVirtualNetwork(args[0], args[1], int(args[2]), args[3], int(args[4]))

    if network_id:
        print "Network has been created (network_id %s)." % str(network_id)

def pa_vlink(args, cmd):
    usage = "%s <network_id> <dpid> <ports>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)

    return parser.parse_args(args)

def do_createVLink(client, gopts, opts, args):
    if len(args) != 2:
        print "createVLink : Must specify a (network_id, and a path string of all the physicalLinks that create a virtualLink)"
        sys.exit()
    linkId = client.createVirtualLink(int(args[0]), args[1])
    if linkId:
        print "Virtual link has been created"

def pa_vswitch(args, cmd):
    usage = "%s <network_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_createVSwitch(client, gopts, opts, args):
    if len(args) != 2:
        print "createVSwitch : Must specify (network_id and list of physical dpids which are associated with this dpid)"
        sys.exit()
    dpids = [str(dpid) for dpid in ast.literal_eval(args[1])]
    print dpids
    dpid = client.createVirtualSwitch(int(args[0]), dpids)
    if dpid:
        print "Virtual switch has been created (dpid %s)" % dpid

def pa_connectHost( args, cmd):
    usage = "%s <mac> <dpid> <port>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_connectHost(client, gopts, opts, args):
    if len(args) != 4:
        print "connectHost : Must specify a tenantId, dpid, port and MAC address"
        sys.exit()

    # takes the tenantid, dpid, port, host mac address
    port = client.createHost(int(args[0]), args[1], int(args[2]), args[3])
    if port:
        print "Host has been connected to edge"

def pa_bootNetwork(args, cmd):
    usage = "%s <network_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_bootNetwork(client, gopts, opts, args):
    if len(args) != 1:
        print "bootNetwork : Must specify a network/tenant ID"
        sys.exit()

    result = client.startNetwork(int(args[0]))

    if result:
        print "Network has been booted"

def pa_saveConfig(args, cmd):
    usage = "%s <cmd>" % USAGE.format(cmd)
    parser = OptionParser(usage=usage)
    return parser.parse_args(args)

def do_saveConfig(client, gopts, opts, args):
    file = client.saveConfig()
    print file

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
    'saveConfig': (pa_saveConfig, do_saveConfig),
    'help' : (pa_help, do_help)
}

DESCS = {
    'createNetwork' : ("Creates a virtual network",
                       ("Creates a virtual network. MAC addresses are given in a comma-separated list, "
                        "each MAC specified as 6 pairs of hexadecimal digits delimited by colons (e.g., 00:0A:E4:25:6B:B0). "
                        "The controller url is of the form tcp:hostname:port, "
                       "so for example tcp:example.com:12345 is a valid controller url. IP Range is given in <net/mask> format (e.g. 172.16.0.0/24)")),
    'createVLink' : ("Create virtual link",
                  ("Create virtual link. Must specify a network_id and virtual link, which is specified as a list of (dpid, inport, outport) tuples.")),
    'createVSwitch' : ("Create virtual switch",
                     ("Create a virtual switch. Must specify a network_id, returns the dpid of the newly created switch.")),
    'connectHost' : ("Connect host to edge switch",
                     ("Connect host to edge switch. Must specify a network_id, mac, dpid and port.")),
    'bootNetwork' : ("Boot virtual network",
                     ("Boot virtual network. Must specify a network_id.")),
    'saveConfig' : ("Saves the config",
                     ("Saves the configuration file into the given fileName."))
}

USAGE="%prog {}"

URL = "http://%s:%s"

def addCommonOpts (parser):
    parser.add_option("-h", "--hostname", dest="host", default="localhost",
                    help="Specify the NetVisor host; default='localhost'")
    parser.add_option("-p", "--port", dest="port", default="8000",
                    help="Specify the NetVisor web port; default=8000")
    # parser.add_option("-u", "--user", dest="fv_user", default="fvadmin",
    #                 help="FlowVisor admin user; default='fvadmin'")
    # parser.add_option("-f", "--passwd-file", dest="fv_passwdfile", default=None,
    #                 help="Password file; default=none")
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
    #Make socket
    transport = TSocket.TSocket('192.168.56.1', 8080)

    # Buffering is critical. Raw sockets are very slow
    transport = TTransport.TFramedTransport(transport)
    # Wrap in a protocol
    protocol = TBinaryProtocol.TBinaryProtocol(transport)

    # Create a client to use the protocol encoder
    client = TenantServer.Client(protocol)
    # Connect!
    transport.open()

    (gopts, rargs, parser) = parse_global_args(sys.argv[1:])

    if len(rargs) < 1:
        raise IndexError
    (parse_args, do_func) = CMDS[rargs[0]]
    (opts, args) = parse_args(rargs[1:], rargs[0])
    do_func(client, gopts, opts, args)
    transport.close()
  except IndexError, e:
    print "%s is an unknown command" % sys.argv[-1]
    printHelp(None, None, None, parser)
