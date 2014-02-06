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

SUPPORTED_PROTO = ['tcp']

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

#Create calls

def pa_createNetwork(args, cmd):
    usage = "%s <protocol> <controller_urls> <ip_network> <ip_mask>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def buildControllerList(ctrls):
    l = ctrls.split(',')
    controllerUrls = []
    for ctrl in l:
        parts = ctrl.split(":")
        if len(parts) < 3:
            print "%s is not a valid controller url" % ctrl
            sys.exit()
        if parts[0] not in SUPPORTED_PROTO:
            print "%s in %s is not a supported protocol" % (parts[0], ctrl)
            sys.exit()
        try:
            int(parts[2])
        except:
            print "%s in %s is not a valid port number" % (parts[2], ctrl)
            sys.exit()
        controllerUrls.append(ctrl)
    return controllerUrls
        

def do_createNetwork(gopts, opts, args):
    if len(args) != 3:
        print "createNetwork : Must specify controllerUrls, network_ip, network_mask"
        sys.exit()
    req = { "controllerUrls" : buildControllerList(args[0]), \
                 "networkAddress" : args[1], "mask" : int(args[2]) }
    network_id = connect(gopts, "createNetwork", data=req, passwd=getPasswd(gopts))
    if network_id:
        print "Virtual network has been created (network_id %s)." % str(network_id)

def pa_createSwitch(args, cmd):
    usage = "%s [options] <tenant_id> <physical_dpids>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    parser.add_option("-d", "--dpid", dest="dpid", type="str", default="0",
            help="Specify the DPID for this switch")
    return parser.parse_args(args)

def do_createSwitch(gopts, opts, args):
    if len(args) != 2:
        print ("createSwitch : must specify: " +
        "virtual tenant_id and a comma separated list of physical dpids " +
        "(e.g. 00:00:00:00:00:00:00:01) which will be associated to the virtual switch")
        sys.exit()
    dpids = [int(dpid.replace(":", ""), 16) for dpid in args[1].split(',')]
    req = { "tenantId" : int(args[0]), "dpids" : dpids, "dpid" : int(opts.dpid.replace(":", ""), 16) }
    reply = connect(gopts, "createSwitch", data=req, passwd=getPasswd(gopts))
    switchId = reply.get('vdpid')
    if switchId:
        switch_name = '00:' + ':'.join([("%x" % switchId)[i:i+2] for i in range(0, len(("%x" % switchId)), 2)])
        print "Virtual switch has been created (tenant_id %s, switch_id %s)"  % (args[0], switch_name)

def pa_createPort(args, cmd):
    usage = "%s <tenant_id> <physical_dpid> <physical_port>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_createPort(gopts, opts, args):
    if len(args) != 3:
        print ("createPort : must specify: " +
        "virtual tenant_id, physical dpid " +
        "(e.g. 00:00:00:00:00:00:00:01) and physical port")
        sys.exit()
    req = { "tenantId" : int(args[0]), "dpid" : int(args[1].replace(":", ""), 16), "port" : int(args[2]) }
    reply = connect(gopts, "createPort", data=req, passwd=getPasswd(gopts))
    
    switchId = reply.get('vdpid')
    portId = reply.get('vport')
    if switchId and portId:
        switch_name = '00:' + ':'.join([("%x" %int(switchId))[i:i+2] for i in range(0, len(("%x" %int(switchId))), 2)])
        print "Virtual port has been created (tenant_id %s, switch_id %s, port_id %s)" % (args[0], switch_name, portId)

def pa_setInternalRouting(args, cmd):
    usage = "%s <tenant_id> <virtual_dpid> <routing_algorithm> <backup_routes_num>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_setInternalRouting(gopts, opts, args):
    if len(args) != 4:
        print ("setInternalRouting : Must specify virtual tenant_id, virtual switch_id, " +
        "algorithm (spf, manual) and number of backup routes")
        sys.exit()
    req = { "tenantId" : int(args[0]), "vdpid" : int(args[1].replace(":", ""), 16), 
           "algorithm" : args[2], "backup_num" : int(args[3]) } 
    reply = connect(gopts, "setInternalRouting", data=req, passwd=getPasswd(gopts))

    tenantId = reply.get('tenantId')
    switchId = reply.get('vdpid')
    if tenantId and switchId:
        print "Routing has be set for big switch (tenant_id %s, switch_id %s)" % (switchId, tenantId)

def pa_connectHost(args, cmd):
    usage = "%s <tenant_id> <vitual_dpid> <virtual_port> <host_mac>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_connectHost(gopts, opts, args):
    if len(args) != 4:
        print "connectHost : Must specify virtual tenant_id, virtual switch_id, virtual port_id and host MAC address"
        sys.exit()
    req = { "tenantId" : int(args[0]), "vdpid" : int(args[1].replace(":", ""), 16), 
           "vport" : int(args[2]), "mac" : args[3] } 
    reply = connect(gopts, "connectHost", data=req, passwd=getPasswd(gopts))
    hostId = reply.get('hostId')
    if hostId:
        print "Host (host_id %s) has been connected to virtual port" % (hostId)
        
def pa_connectLink(args, cmd):
    usage = "%s <tenant_id> <src_virtual_dpid> <src_virtual_port> <dst_virtual_dpid> <dst_virtual_port>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)

    return parser.parse_args(args)

def do_connectLink(gopts, opts, args):
    if len(args) != 7:
        print ("connectLink : Must specify tenant_id, src_virtual_dpid, src_virtual_port, dst_virtual_dpid, dst_virtual_port, " 
        + "algorithm (spf, manual), number of backup routes")
        sys.exit()
    req = { "tenantId" : int(args[0]), "srcDpid" : int(args[1].replace(":", ""), 16), 
           "srcPort" : int(args[2]), "dstDpid" : int(args[3].replace(":", ""), 16), 
           "dstPort" : int(args[4]), "algorithm" : args[5], "backup_num" : int(args[6]) }
    reply = connect(gopts, "connectLink", data=req, passwd=getPasswd(gopts))
    linkId = reply.get('linkId')
    if linkId:
        print "Virtual link (link_id %s) has been created" % (linkId)

def pa_setLinkPath(args, cmd):
    usage = "%s <tenant_id> <link_id> <physical_path> <priority>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)

    return parser.parse_args(args)

def do_setLinkPath(gopts, opts, args):
    if len(args) != 4:
        print "setLinkPath : Must specify tenant_id, link_id, the physical path that connect the end-points and the priority [0-255]"
        sys.exit()
    req = { "tenantId" : int(args[0]), "linkId" : int(args[1]), "path" : translate_path(args[2]), "priority" : int(args[3]) }
    reply = connect(gopts, "setLinkPath", data=req, passwd=getPasswd(gopts))
    linkId = reply.get('linkId')
    if linkId:
        print "Virtual link (link_id %s) path has been set" % (linkId)

def pa_connectRoute(args, cmd):
    usage = "%s <tenant_id> <virtual_dpid> <src_virtual_port> <dst_virtual_port> <physical_path> <priority>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_connectRoute(gopts, opts, args):
    if len(args) != 6:
        print ("connectRoute : Must specify tenant_id, virtual_dpid, src_virtual_port, dst_virtual_port, " + 
        "the physical path that connect the end-points and the priority [0-255]")
        sys.exit()
    req = { "tenantId" : int(args[0]), "vdpid" : int(args[1].replace(":", ""), 16), 
           "srcPort" : int(args[2]), "dstPort" : int(args[3]),
           "path" : translate_path(args[4]), "priority" : int(args[5]) }
    reply = connect(gopts, "connectRoute", data=req, passwd=getPasswd(gopts))
    routeId = reply.get('routeId')
    if routeId:
        print "Big-switch internal route (route_id %s) has been created" % (routeId)

#Remove calls

def pa_removeNetwork(args, cmd):
    usage = "%s <tenant_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_removeNetwork(gopts, opts, args):
    if len(args) != 1:
        print "removeNetwork : Must specify a virtual tenant_id"
        sys.exit()
    req = { "tenantId" : int(args[0]) }
    result = connect(gopts, "removeNetwork", data=req, passwd=getPasswd(gopts)) 
    print "Network (tenant_id %s) has been removed" % (args[0])       
        
def pa_removeSwitch(args, cmd):
    usage = "%s <tenant_id> <virtual_dpid>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_removeSwitch(gopts, opts, args):
    if len(args) != 2:
        print "removeSwitch : Must specify a virtual tenant_id and a virtual switch_id"
        sys.exit()
    req = { "tenantId" : int(args[0]), "vdpid" : int(args[1].replace(":", ""), 16) }
    result = connect(gopts, "removeSwitch", data=req, passwd=getPasswd(gopts)) 
    print "Switch (switch_id %s) has been removed" % (args[1])      

def pa_removePort(args, cmd):
    usage = "%s <tenant_id> <virtual_dpid> <virtual_port>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_removePort(gopts, opts, args):
    if len(args) != 3:
        print "removePort : Must specify a virtual tenant_id, a virtual switch_id and a virtual port_id"
        sys.exit()
    req = { "tenantId" : int(args[0]), "vdpid" : int(args[1].replace(":", ""), 16), "vport" : int(args[2])}
    result = connect(gopts, "removePort", data=req, passwd=getPasswd(gopts)) 
    print "Port (port_id %s) has been removed from virtual switch (switch_id %s)" % (args[2], args[1]) 

def pa_disconnectHost(args, cmd):
    usage = "%s <tenant_id> <host_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_disconnectHost(gopts, opts, args):
    if len(args) != 2:
        print "disconnectHost : Must specify a a virtual tenant_id and a host_id"
        sys.exit()
    req = { "tenantId" : int(args[0]), "hostId" : int(args[1]) } 
    result = connect(gopts, "disconnectHost", data=req, passwd=getPasswd(gopts)) 
    print "Host (host_id %s) has been disconnected from the virtual network (tenant_id %s)" % (args[1], args[0])

def pa_disconnectLink(args, cmd):
    usage = "%s <tenant_id> <link_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_disconnectLink(gopts, opts, args):
    if len(args) != 2:
        print "disconnectLink : Must specify a a virtual tenant_id and a link_id"
        sys.exit()
    req = { "tenantId" : int(args[0]), "linkId" : int(args[1]) } 
    result = connect(gopts, "disconnectLink", data=req, passwd=getPasswd(gopts)) 
    print "Link (link_id %s) has been disconnected from the virtual network (tenant_id %s)" % (args[1], args[0])

def pa_disconnectRoute(args, cmd):
    usage = "%s <tenant_id> <route_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_disconnectRoute(gopts, opts, args):
    if len(args) != 3:
        print "disconnectRoute : Must specify a virtual tenant_id, switch_id and a route_id"
        sys.exit()
    req = { "tenantId" : int(args[0]), "vdpid" : int(args[1].replace(":", ""), 16) , "routeId" : int(args[2]) } 
    result = connect(gopts, "disconnectRoute", data=req, passwd=getPasswd(gopts))
    print "Route (route_id %s) in virtual big-switch (switch_id %s) has been disconnected from the virtual network (tenant_id %s)" % (args[2], args[1], args[0])

#Runtime operations

def pa_startNetwork(args, cmd):
    usage = "%s <tenant_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_startNetwork(gopts, opts, args):
    if len(args) != 1:
        print "startNetwork : Must specify a tenant_id"
        sys.exit()
    req = { "tenantId" : int(args[0]) }
    result = connect(gopts, "startNetwork", data=req, passwd=getPasswd(gopts)) 
    if result:
        print "Network (tenant_id %s) has been booted" % (args[0])

def pa_startSwitch(args, cmd):
    usage = "%s <tenant_id> <virtual_dpid>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_startSwitch(gopts, opts, args):
    if len(args) != 2:
        print "startSwitch : Must specify a tenant_id and a virtual switch_id"
        sys.exit()
    req = { "tenantId" : int(args[0]), "vdpid" : int(args[1].replace(":", ""), 16)}
    result = connect(gopts, "startSwitch", data=req, passwd=getPasswd(gopts)) 
    if result:
        print "Switch (switch_id %s) has been booted in virtual network (tenant_id %s)" % (args[1], args[0])

def pa_startPort(args, cmd):
    usage = "%s <tenant_id> <virtual_dpid>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_startPort(gopts, opts, args):
    if len(args) != 3:
        print "startPort : Must specify a tenant_id, a virtual switch_id and a virtual port_id"
        sys.exit()
    req = { "tenantId" : int(args[0]), "vdpid" : int(args[1].replace(":", ""), 16), "vport" : int(args[2])}
    reply = connect(gopts, "startPort", data=req, passwd=getPasswd(gopts))
    tenantId = reply.get('tenantId')
    switchId = reply.get('vdpid')
    portId = reply.get('vport')
    if tenantId and switchId and hostId:
        print "Port (port_id %s) has been started in virtual switch (tenant_id %s, switch_id %s)" % (portId, tenantId, switchId)

def pa_stopNetwork(args, cmd):
    usage = "%s <tenant_id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_stopNetwork(gopts, opts, args):
    if len(args) != 1:
        print "stopNetwork : Must specify a tenant_id"
        sys.exit()
    req = { "tenantId" : int(args[0]) }
    result = connect(gopts, "stopNetwork", data=req, passwd=getPasswd(gopts)) 
    if result:
        print "Network (tenant_id %s) has been shutdown" % (args[0])

def pa_stopSwitch(args, cmd):
    usage = "%s <tenant_id> <virtual_dpid>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_stopSwitch(gopts, opts, args):
    if len(args) != 2:
        print "stopSwitch : Must specify a tenant_id and a virtual switch_id"
        sys.exit()
    req = { "tenantId" : int(args[0]), "vdpid" : int(args[1].replace(":", ""), 16)}
    result = connect(gopts, "stopSwitch", data=req, passwd=getPasswd(gopts)) 
    if result:
        print "Switch (switch_id %s) has been shutdown in virtual network (tenant_id %s)" % (args[1], args[0])

def pa_stopPort(args, cmd):
    usage = "%s <tenant_id> <virtual_dpid>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)    

def do_stopPort(gopts, opts, args):
    if len(args) != 3:
        print "stopPort : Must specify a tenant_id, a virtual switch_id and a virtual port_id"
        sys.exit()
    req = { "tenantId" : int(args[0]), "vdpid" : int(args[1].replace(":", ""), 16), "vport" : int(args[2])}
    result = connect(gopts, "stopPort", data=req, passwd=getPasswd(gopts)) 
    if result:
        print "Port (port_id %s) has been shutdown in virtual switch (tenant_id %s, switch_id %s)" % (args[2], args[0], args[1])
        
# Other methods

def translate_path(path_string):
    hop_list = path_string.split(",")
    path = ""
    for hop in hop_list:
        src, dst = hop.split("-")
        src_dpid, src_port = src.split("/")
        dst_dpid, dst_port = dst.split("/")
        src_long_dpid = int(src_dpid.replace(":", ""), 16)
        dst_long_dpid = int(dst_dpid.replace(":", ""), 16)
        path = path + str(src_long_dpid) + "/" + str(src_port) + "-" + str(dst_long_dpid) + "/" + str(dst_port) + ","
    if len(path) > 0:
        path.rstrip(",")
    return path

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
    print parser.format_help().strip()
    print "\n Available commands are: "
    for x in cmds:
      (sdesc, ldesc) = DESCS[x]
      print "   {0:25}     {1:10}".format(x, sdesc)
    print "\n See '%s help <command>' for more info on a specific command." % sys.argv[0]
    sys.exit()

CMDS = {
    'createNetwork': (pa_createNetwork, do_createNetwork),
    'createSwitch': (pa_createSwitch, do_createSwitch),
    'createPort': (pa_createPort, do_createPort),
    'setInternalRouting': (pa_setInternalRouting, do_setInternalRouting),
    'connectHost': (pa_connectHost, do_connectHost),
    'connectLink': (pa_connectLink, do_connectLink),
    'setLinkPath': (pa_setLinkPath, do_setLinkPath),
    'connectRoute': (pa_connectRoute, do_connectRoute),
    
    'removeNetwork': (pa_removeNetwork, do_removeNetwork),
    'removeSwitch': (pa_removeSwitch, do_removeSwitch),
    'removePort': (pa_removePort, do_removePort),
    'disconnectHost': (pa_disconnectHost, do_disconnectHost),
    'disconnectLink': (pa_disconnectLink, do_disconnectLink),
    'disconnectRoute': (pa_disconnectRoute, do_disconnectRoute),
    
    'startNetwork': (pa_startNetwork, do_startNetwork),
    'startSwitch': (pa_startSwitch, do_startSwitch),
    'startPort': (pa_startPort, do_startPort),
    'stopNetwork': (pa_stopNetwork, do_stopNetwork),
    'stopSwitch': (pa_stopSwitch, do_stopSwitch),
    'stopPort': (pa_stopPort, do_stopPort), 
    
    'help' : (pa_help, do_help)
}

DESCS = {
    'createNetwork' : ("Creates a virtual network",
                       ("Creates a virtual network. Input: protocol, controllerIP, controller port, ip address, mask. "
                        "\nExample: createNetwork tcp 1.1.1.1 6634 192.168.1.0 24")),
    'createSwitch' : ("Create virtual switch", 
                      ("Create a virtual switch. Must specify a tenant_id, and a list of the physical_dpids that will be part of the virtual switch."
                        "\nExample: createSwitch 1 00:00:00:00:00:00:00:01,00:00:00:00:00:00:00:02")),
    'createPort' : ("Create virtual port", 
                      ("Create a virtual port. Must specify a tenant_id, a physical_dpid and a physical_port."
                        "\nExample: createPort 1 00:00:00:00:00:00:00:01 1")),         
    'setInternalRouting' : ("Set big-switch internal routing mechanism", 
                      ("Set big-switch internal routing mechanism. Must specify a tenant_id, a virtual switch_id, the routing type (spf, manual) " 
                       "and the number (0-255) of the backup paths that have to be computed."
                        "\nExample: setInternalRouting 1 00:00:00:00:00:00:00:01 spf 128")),  
    'connectHost' : ("Connect host to a virtual port", 
                      ("Connect host to a virtual port. Must specify a tenant_id, a virtual switch_id, a virtual port_id and the host MAC address."
                        "\nExample: connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:00:01")),         
    'connectLink' : ("Connect two virtual ports through a virtual link", 
                      ("Connect two virtual ports through a virtual link. Must specify a tenant_id, a virtual src_switch_id, a virtual src_port_id, " 
                       "a virtual dst_switch_id, a virtual dst_port_id, the routing type (spf, manual) and the number (0-255) of the backup paths that have to be computed."
                        "\nExample: connectLink 1 00:a4:23:05:00:00:00:01 1 00:a4:23:05:00:00:00:02 1 spf 1")), 
    'setLinkPath' : ("Set the physical path of a virtual link", 
                      ("Set the physical path of a virtual link. Must specify a tenant_id, a virtual link_id, a physical path and a priority (0-255)."
                        "\nExample: connectLink 1 1 00:00:00:00:00:00:00:01/1-00:00:00:00:00:00:00:02/1,"
                        "00:00:00:00:00:00:00:2/2-00:00:00:00:00:00:00:3/1 128")),
    'connectRoute' : ("Connect two virtual ports inside a virtual big-switch", 
                      ("Connect two virtual ports inside a virtual big-switch. Must specify a tenant_id, a virtual switch_id, a virtual src_port_id, " 
                       "a virtual dst_port_id, a physical path and a priority (0-255)."
                        "\nExample: connectRoute 1 00:a4:23:05:00:00:00:01 1 2 00:00:00:00:00:00:00:01/1-00:00:00:00:00:00:00:02/1,"
                        "00:00:00:00:00:00:00:2/2-00:00:00:00:00:00:00:3/1 128")),           
                  
    'removeNetwork' : ("Remove a virtual network",
                     ("Remove a virtual network. Must specify a tenant_id."
                        "\nExample: removeNetwork 1")),
    'removeSwitch' : ("Remove virtual switch",
                     ("Remove a virtual switch. Must specify a tenant_id and a virtual switch_id."
                        "\nExample: removeSwitch 1 00:a4:23:05:00:00:00:01")),
    'removePort' : ("Remove virtual port",
                     ("Remove a virtual port. Must specify a tenant_id, a virtual switch_id and a virtual port_id."
                        "\nExample: removePort 1 00:a4:23:05:00:00:00:01 1")),
    'disconnectHost' : ("Disconnect host from a virtual port",
                     ("Disconnect host from a virtual port. Must specify a tenant_id and the host_id."
                        "\nExample: disconnectHost 1 1")),
    'disconnectLink' : ("Disconnect link between two virtual ports",
                     ("Disconnect link between two virtual ports. Must specify a tenant_id and the link_id."
                        "\nExample: disconnectLink 1 1")),
    'disconnectRoute' : ("Disconnect big-switch internal route between two virtual ports",
                     ("Disconnect big-switch internal route between two virtual ports. Must specify a tenant_id and the route_id."
                        "\nExample: disconnectRoute 1 00:a4:23:05:00:00:00:01 1")),
         
    'startNetwork' : ("Start a virtual network",
                     ("Start a virtual network. Must specify a tenant_id."
                        "\nExample: startNetwork 1")), 
    'startSwitch' : ("Start a virtual switch",
                     ("Start a virtual switch. Must specify a tenant_id and a virtual switch_id."
                        "\nExample: startSwitch 1 00:a4:23:05:00:00:00:01")),
    'startPort' : ("Start a virtual port",
                     ("Start a virtual port. Must specify a tenant_id, a virtual switch_id and a virtual port_id."
                        "\nExample: startPort 1 00:a4:23:05:00:00:00:01 1")),        
    'stopNetwork' : ("Stop a virtual network",
                     ("Stop a virtual network. Must specify a tenant_id."
                        "\nExample: stopNetwork 1")), 
    'stopSwitch' : ("Shutdown a virtual switch",
                     ("Shutdown a virtual switch. Must specify a tenant_id and a virtual switch_id."
                        "\nExample: stopSwitch 1 00:a4:23:05:00:00:00:01")),
    'stopPort' : ("Shutdown a virtual port",
                     ("Shutdown a virtual port. Must specify a tenant_id, a virtual switch_id and a virtual port_id."
                        "\nExample: stopPort 1 00:a4:23:05:00:00:00:01 1"))
}

USAGE="%prog {}"

URL = "http://%s:%s/tenant"

def getPasswd(opts):
    if opts.no_passwd:
        return ""
    else:
        return getpass.getpass("Password: ") 

def addCommonOpts (parser):
    parser.add_option("-h", "--hostname", dest="host", default="localhost",
                    help="Specify the OpenVirteX host; default='localhost'")
    parser.add_option("-p", "--port", dest="port", default="8080",
                    help="Specify the OpenVirteX web port; default=8080")
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
      print "createSwitch: int, comma separated list of strings"
    elif function=='createPort':
      print "createPort: int, string, short"
    elif function=='setInternalRouting':
      print "setInternalRouting: int, string, string, byte"
    elif function=='connectHost':
      print "connectHost: int, string, short, string"
    elif function=='connectLink':
      print "connectLink: int, string, short, string, short, string, byte"
    elif function=='connectRoute':
      print "connectRoute: int, string, short, short, string, byte"
      
    elif function=='removeNetwork':
      print "removeNetwork: int"  
    elif function=='removeSwitch':
      print "removeSwitch: int, string"  
    elif function=='removePort':
      print "removePort: int, string , short"  
    elif function=='disconnectHost':
      print "disconnectHost: int, int"        
    elif function=='disconnectLink':
      print "disconnectLink: int, int"    
    elif function=='disconnectRoute':
      print "disconnectRoute: int, int"          
            
    elif function=='startNetwork':
      print "startNetwork: int"  
    elif function=='startSwitch':
      print "startSwitch: int, string"  
    elif function=='startPort':
      print "startPort: int, string , short"             
    elif function=='stopNetwork':
      print "stopNetwork: int"  
    elif function=='stopSwitch':
      print "stopSwitch: int, string"  
    elif function=='stopPort':
      print "stopPort: int, string , short"
      
  except IndexError, e:
    print "%s is an unknown command" % sys.argv[-1]
    printHelp(None, None, None, parser)
  except Exception, e:
    print "uknown error"
    print e
