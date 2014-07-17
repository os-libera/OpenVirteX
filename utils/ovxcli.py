#!/usr/bin/env python


try:
    import readline
except ImportError:
    print("Module readline not available. Tab completion will not be available")
else:
    import rlcompleter
    if 'libedit' in readline.__doc__:
        readline.parse_and_bind("bind ^I rl_complete")
    else:
        readline.parse_and_bind("tab: complete")

# OpenVirteX cli utility

from cmd import Cmd
from optparse import OptionParser
import getpass
import sys
import exceptions
import errno
from cli.utils import *
from cli.handlers import *
from cli.processors import *
from cli.connection import *
import copy


VERSION = "0.1"
SUPPORTED_OVX = "prealpha 0.0.2"



class OVXCmd(Cmd):
    
    def __init__(self):
        pass
    
    def emptyline(self):
        pass
        
    def do_exit (self, arglist):
        """
        Exit this context.
        """
        #As exception is not letting postloop() to run for munually call postloop()
        self.postloop()
        raise SubinterpreterExit()
    
    def do_quit (self, arglist):
        """
        Exit this context.
        """
        self.do_exit(arglist)
     
    def do_EOF (self, arglist):
        """
        Exit this context.
        """
        print
        self.do_exit(arglist)
    
    def preloop(self):
        """
        Initialization before prompting user for commands.
        Before entering in two new Context needs
        Two main things to keep history according to context:
        1- Clear the history.
        2- Load previous history of this context (if any) from file.
        note: Each context stores its now history in precmd()
        """
        Cmd.preloop(self)
        try:
            self.file= open(self.file_name,'r+',0)
        #1
            readline.clear_history()
        #2
            for line in self.file.readlines():
                readline.add_history(line)
        except IOError:
            print 'There is no file named', self.file_name

    def postloop(self):
        """
        Before leaving the context need to close the opened file
        """
        #1
        Cmd.postloop(self)   ## sets up command completion
        self.file.close()
        
    def precmd(self, line):
        """
        Store every command into the file.
        """
        try:
            if(line.strip() != ""):
                self.file.write(line.strip()+"\n")
            return line
        except IOError:
            print "Can't Write to file: ", self.file_name

        
class OVXLinkCmd(OVXCmd):
    
    def __init__(self, tid, link, conn):
        Cmd.__init__(self)
        self.tid = tid
        self.link = link
        self.ovx = conn
        self.type = "link"
        self.name =  int(link['linkId'])
        OVXLinkCmd.prompt = "ovxctl(%s/%s-%s/%s)$ " % (self.link['src']['dpid'], \
                                                       self.link['src']['port'], \
                                                       self.link['dst']['dpid'], \
                                                       self.link['dst']['port'])
        self.file_name= ".history_"+ self.__class__.__name__+".txt"
        self.file= open(self.file_name,'a+',0)
    
    
    def do_drop(self, args):
        """
        Disconnect this virtual link.
        """
        resp = raw_input("Are you sure you want to delete vLink %s? (Yes/no) : " % int(self.link['linkId']))
        if resp == "Yes":
            self.ovx.disconnectLink(self.tid, self.link['linkId'])
            raise SubinterpreterExit(dropped = True)
        
class OVXHostCmd(OVXCmd):
    
    def __init__(self, tid, host, conn):
        Cmd.__init__(self)
        self.tid = tid
        self.host = host
        self.ovx = conn
        self.type = "host"
        self.name =  int(self.host['hostId'])
        OVXHostCmd.prompt = "ovxctl(vn-%s-host-%s)$ " % (self.tid, self.name)
        self.file_name= ".history_"+ self.__class__.__name__+".txt"
        self.file= open(self.file_name,'a+',0)
    
    
    def do_drop(self, args):
        """
        Disconnect this virtual host.
        """
        resp = raw_input("Are you sure you want to delete vHost %s? (Yes/no) : " % int(self.name))
        if resp == "Yes":
            self.ovx.disconnectHost(self.tid, self.name)
            raise SubinterpreterExit(dropped = True)
        

class OVXPortCmd(OVXCmd):
    
    def __init__(self, tid, port, sw, conn):
        Cmd.__init__(self)
        self.tid = tid
        self.sw = sw
        self.port = port
        self.ovx = conn
        self.name = self.port
        self.type = "port"
        OVXPortCmd.prompt = "ovxctl(vn-%s-%s-%s)$ " % (self.tid, self.sw, self.port)
        self.file_name= ".history_"+ self.__class__.__name__+".txt"
        self.file= open(self.file_name,'a+',0)
    
    def do_enable(self, args):
        """
        Boot this virtual port.
        """
        self.ovx.startPort(self.tid, to_long_dpid(self.sw), int(self.port))
        
    
    def do_disable(self, args):
        """
        Shutdown this virtual port.
        """
        self.ovx.stopPort(self.tid, to_long_dpid(self.sw), int(self.port))
    
    def do_drop(self, args):
        """
        Permanently delete this virtual port.
        """
        resp = raw_input("Are you sure you want to delete vPort %s? (Yes/no) : " % self.port)
        if resp == "Yes":
            self.ovx.removePort(self.tid, to_long_dpid(self.sw), int(self.port))
            raise SubinterpreterExit(dropped = True)
 
    

    
class OVXSwitchCmd(OVXCmd):
    
    def __init__(self, tid, sw, conn):
        Cmd.__init__(self)
        self.type = "switch"
        self.name =  sw
        self.tid = tid
        self.sw = sw
        self.ovx = conn
        OVXSwitchCmd.prompt = "ovxctl(vn-%s-%s)$ " % (self.tid, self.sw)
        self.__set_showers()
        self.__set_adders()
        self.file_name= ".history_"+ self.__class__.__name__+".txt"
        self.file= open(self.file_name,'a+',0)
        
    def __set_showers(self):
        self.showers = {"mapping" : VirtualSwitchMapping(self.ovx, self.tid), \
                        "flowtable" : VirtualFlowtable(self.ovx, self.tid), \
                        "ports" : VirtualPorts(self.ovx, self.tid), \
                        "hosts" : VirtualHosts(self.ovx, self.tid)}
                        
    def __set_adders(self):
        self.mapped = self.showers['mapping'].show([self.sw])
        self.adders = {"port" : VirtualPortAdd(self.ovx, self.tid, self.mapped), \
                       "host" : VirtualHostAdd(self.ovx, self.tid, self.sw) }
        
                        
    def __isBigSwitch(self):
        if self.mapped is None:
            return False
        if (len(self.mapped) > 1):
            return True
        return False
    
    def __wait_for_subint(self, cmd):
        while True: 
            try:
                cmd.cmdloop()
            except SubinterpreterExit, e:
                if (e.drop):
                    print "Removed virtual %s %s." % (cmd.type, cmd.name)
                else:
                    print "Exiting virtual %s %s context." % (cmd.type, cmd.name)
                     #Clearing previous histroy and loading own history
                    readline.clear_history()
                    try:
                        self.file= open(self.file_name,'r+',0)
                        for line in self.file.readlines():
                            readline.add_history(line)
                    except IOError:
                        print "Can't read from the file: ",self.file_name
                break
            except exceptions.KeyboardInterrupt, e:
                print
                continue
            except Exception, e:
                    #(self.__exc_t, self.__exc_v, self.__exc_tb) = sys.exc_info()
                    #print self.__exc_t, sys.exc_info()
                    print e
                    continue  
                
    def __find_host(self, hostId):
        hosts = self.ovx.getVirtualHosts(self.tid)
        for host in hosts:
            if host['hostId'] == hostId:
                return host
        return None
 
    
    def complete_show(self, text, line, begidx, endidx):
        terms = self.showers.keys()
        parts = line.split()
        if len(parts) == 1:
            return terms 
        if (len(parts) == 2 and parts[1] not in terms):
            return [ poss for poss in terms
                if poss.startswith(text) ]
            
    def help_show(self):
        print '{:<20} {}'.format('Command', 'Description')
        print '='.rjust(40, '=')
        for cmd, obj in self.showers.iteritems():
            print '{:<20} {}'.format(cmd, obj.help())                
     
    def do_show(self, args):
        arglist = args.split()
        if (arglist[0] in self.showers):
            self.showers[arglist[0]].pprint(self.sw)
            
    def complete_add(self, text, line, begidx, endidx):
        terms = self.adders.keys()
        parts = line.split()
        if len(parts) == 1:
            return terms 
        if (len(parts) == 2 and parts[1] not in terms):
            return [ poss for poss in terms
                     if poss.startswith(text) ]
        if (len(parts) >= 2):
            if parts[1] in terms:
                return self.adders[parts[1]].complete(text, line)
            
    def do_host(self, args):
        """
        Enter a virtual host context.
        Usage: host <hostId>
        """
        hostId = int(args)
        host = self.__find_host(hostId)
        if host is not None:
            host_cmd = OVXHostCmd(self.tid, host, self.ovx)
            self.__wait_for_subint(host_cmd)
        else:
            print "Virtual Host %s does not exist." % hostId
            
    def help_add(self):
        print '{:<20} {}'.format('Command', 'Description')
        print '='.rjust(40, '=')
        for cmd, obj in self.adders.iteritems():
            print '{:<20} {}'.format(cmd, obj.help())
        
                    
    def do_add(self, args):
        """
        Add a virtual port to the mapped physical switch and port.
        """
        arglist = args.split()
        if (arglist[0] in self.adders):
            try:
                self.adders[arglist[0]].process(arglist[1], arglist[2])
            except exceptions.Exception, e:
                print "Unable to process command."
                print self.adders[arglist[0]].help()
            
    def complete_port(self, text, line, begidx, endidx):
        ports = self.showers['ports'].show(self.sw).keys()
        
        if (text is None):
            completions = ports
        else:
            completions = [ poss 
                     for poss in ports
                     if poss.startswith(text) ]
        return completions
            
    def do_port(self, args):
        """
        Enter a port context.
        Usage: port <port_number>
        """
        arglist = args.split()
        ports = self.showers['ports'].show(self.sw).keys()
        if arglist[0] in ports:
            port_cmd = OVXPortCmd(self.tid, arglist[0], self.sw, self.ovx)
        else:
            
            print "Virtual port %s on switch %s does not exist." % (arglist[0], sw)
            print "Use add command to create a new virtual port."
            return
            
        self.__wait_for_subint(port_cmd)
        
            
            
    #def do_ctrl(self, args):
    #    """
    #    Add a controller to this virtual switch.
    #    Usage: ctrl tcp:<controller_host>:<controller_port>
    #    """
    #    self.ovx.addController(self.tid, to_long_dpid(self.sw), args.split()) 
        
    def do_enable(self, args):
        """
        Boots up this virtual switch.
        """
        self.ovx.startSwitch(self.tid, to_long_dpid(self.sw))
    
    def do_disable(self, args):
        """
        Shuts down this virtual switch.
        """
        self.ovx.stopSwitch(self.tid, to_long_dpid(self.sw))
          

    def do_drop(self, args):
        """
        Permanently delete this virtual switch from the virtual network
        """
        resp = raw_input("Are you sure you want to delete vswitch %s? (Yes/no) : " % self.sw)
        if resp == "Yes":
          self.ovx.removeSwitch(self.tid, to_long_dpid(self.sw))
          raise SubinterpreterExit(dropped = True)

class VirtualNetwork(OVXCmd):
    
    def __init__(self, vn, conn):
        Cmd.__init__(self)
        self.tid = vn['tenantId']
        self.ctrls = vn['controllerUrls']
        self.ovx = conn
        VirtualNetwork.prompt = "ovxctl(vn-%s)$ " % self.tid
        self.__set_switch_types()
        self.__set_showers()
        self.__set_adders()
        self.file_name= ".history_"+ self.__class__.__name__+".txt"
        self.file= open(self.file_name,'a+',0)
        self.file.close()


    def __set_switch_types(self):
        self.switch_types = {"singleswitch" : SingleSwitch(self.ovx), \
                             "bigswitch" : BigSwitch(self.ovx) }
        
    def __set_showers(self):
        vnets = VNets(self.ovx)
        self.showers = {"topology" : VirtualTopology(self.ovx, self.tid), \
                         "devices" : VirtualDevices(self.ovx, self.tid), \
                         "status" : vnets, \
                         "ctrls" : vnets}

    def __set_adders(self):
        self.adders = { "link" : VirtualLinkAdd(self.ovx, self.tid) }
        
    def __wait_for_subint(self, sw):
        while True: 
            try:
                sw.cmdloop()
            except SubinterpreterExit, e:
                if (e.drop):
                    print "Removed virtual %s %s." % (sw.type, sw.name)
                else:
                    print "Exiting virtual %s %s context." % (sw.type, sw.name)
                    #Clearing previous histroy and loading own history
                    readline.clear_history()
                    try:
                        self.file= open(self.file_name,'r+',0)
                        for line in self.file.readlines():
                            readline.add_history(line)
                    except IOError:
                        print "Can't read from the file: ",self.file_name

                break
            except exceptions.KeyboardInterrupt, e:
                print
                continue
            except Exception, e:
                    #(self.__exc_t, self.__exc_v, self.__exc_tb) = sys.exc_info()
                    #print self.__exc_t, sys.exc_info()
                    print e
                    continue    
    
    def complete_add(self, text, line, begidx, endidx):
        
        terms = self.switch_types.keys() + self.adders.keys()
        
        parts = line.split()
        
        if len(parts) == 1:
            return terms 
        if (len(parts) == 2 and parts[1] not in terms):
            return [ poss for poss in terms
                if poss.startswith(text) ]
        if (parts[1] in self.switch_types):
            return self.switch_types[parts[1]].complete(text, line)
        else:
            return self.adders[parts[1]].complete(text, line)
        
    def help_add(self):
        items = dict(self.adders.items() + self.switch_types.items())
        print '{:<20} {}'.format('Command', 'Description')
        print '='.rjust(40, '=')
        for cmd, obj in items.iteritems():
            print '{:<20} {}'.format(cmd, obj.help())
          
    def help_show(self):
        print '{:<20} {}'.format('Command', 'Description')
        print '='.rjust(40, '=')
        for cmd, obj in self.showers.iteritems():
            print '{:<20} {}'.format(cmd, obj.help())
        
    def do_add(self, args):
        """
        Adds a switch to the virtual network
        """
        vdpid = None
        arglist = args.split()
        subcmd = arglist[0]
        if subcmd in self.switch_types:
            (sws, vdpid) = self.switch_types[subcmd].process(args)
            data = self.ovx.createSwitch(self.tid, sws, vdpid = vdpid)
            assert self.tid == data['tenantId']
            created = to_dpid_str(data['vdpid'])
            print "Created %s (%s)." % (subcmd, created)
        elif subcmd in self.adders:
            assert len(arglist) == 5
            link = self.adders[subcmd].process(arglist[1], arglist[3], arglist[2], arglist[4])
        else:
            print "Unknown subcommand type. Supported types are: %s" \
                        % self.switch_types.keys() + self.adders.keys()
            return
            
        
        
    def complete_show(self, text, line, begidx, endidx):
        terms = self.showers.keys()
        parts = line.split()
        if len(parts) == 1:
            return terms 
        if (len(parts) == 2 and parts[1] not in terms):
            return [ poss for poss in terms
                if poss.startswith(text) ]
        if (parts[1] in terms):
            if (len(parts) == 3):
                return self.showers[parts[1]].complete(parts[2])
            else:
                return self.showers[parts[1]].complete(None)    
    
    def do_show(self, args):
        """
        displays information about the virtual network.
        """
        arglist = args.split()
        if len(arglist) != 1:
            print "Incomplete command: possible arguments are %s" % self.showers.keys() 
            return
        
        if (arglist[0] == "ctrls"):
            vnets = self.showers[arglist[0]].show(None)
            for vnet in vnets:
                if self.tid == vnet['tenantId']:
                    print "Configured controllers : %s" % vnet['controllerUrls']
                    return
        
        if (arglist[0] == "status"):
            vnets = self.showers[arglist[0]].show(None)
            for vnet in vnets:
                if self.tid == vnet['tenantId']:
                    state = "disabled"
                    if vnet['isBooted']:
                        state = "enabled"
                    print "Virtual Network %s is %s." % (self.tid, state)
                    return
            
                
        if (arglist[0] in self.showers):
            self.showers[arglist[0]].pprint(*args.split())
            
    

    def __find_link(self, linkId):
        links = self.showers['topology'].show(self.tid)
        for link in links:
            if int(linkId) == link['linkId']:
                return link
        return None
    
    def do_link(self, args):
        """
        Enter the virtual link configuration context.
        Usage: link <linkid>
        LinkId can be obtained from the virtual topology
        """
        linkId = args
        link = self.__find_link(linkId)
        if link is not None:
            link_cmd = OVXLinkCmd(self.tid, link, self.ovx)
            self.__wait_for_subint(link_cmd)
        else:
            print "Virtual link %s does not exist." % linkId
            
    
            
    def complete_switch(self, text, line, begidx, endidx):
        devices = self.showers['devices'].show(None)
        
        if (text is None):
            completions = devices
        else:
            completions = [ poss 
                     for poss in devices
                     if poss.startswith(text) ]
        return completions
            
    def do_switch(self, args):
        """
        Enter the virtual switch configuration context.
        """
        sw = args
        devices = self.showers['devices'].show(None)
        if sw in devices:
            sw_cmd = OVXSwitchCmd(self.tid, sw, self.ovx)
        else:
            
            print "Virtual switch %s does not exist." % sw
            print "Use add command to create a new virtual switch."
            return
            
        self.__wait_for_subint(sw_cmd)
        
    def do_ctrl(self, args):
        """
        Add a controller to this virtual network.
        Usage: ctrl tcp:<controller_host>:<controller_port>
        """
        self.ovx.addController(self.tid, args.split())
        
        
    def do_rctrl(self, args):
        """
        Remove a controller to this virtual network.
        Usage: rctrl tcp:<controller_host>:<controller_port>
        """
        self.ovx.removeController(self.tid, args.split())
        
    def do_enable(self, args):
        """
        Boot this virtual network
        """
        vnet = self.ovx.startNetwork(self.tid)
        state = "disabled"
        if vnet['isBooted']:
            state = "enabled"
        print "Virtual Network %s is %s." % (self.tid, state)
        
    def do_disable(self, args):
        """
        Shutdown this virtual network
        """
        vnet = self.ovx.stopNetwork(self.tid)
        state = "disabled"
        if vnet['isBooted']:
            state = "enabled"
        print "Virtual Network %s is %s." % (self.tid, state)
        
    def do_drop(self, args):
        """
        Delete this virtual network. Permanently.
        """
        resp = raw_input("Are you sure you want to delete VN %s? (Yes/no) : " % self.tid)
        if resp == "Yes":
          self.ovx.removeNetwork(self.tid)
          raise SubinterpreterExit(dropped = True)  
      
class OVXCtl(OVXCmd):
    prompt = "ovxctl(offline)$ "
    def __init__(self, host, user, port, passwd, isSSL):
        Cmd.__init__(self, completekey='tab')
        self.user = user
        self.connections = []
        self.__ac = None
        self.vns = {}
        self.__connect(host, user, port, passwd, isSSL)
        for vn in self.__ac.listVirtualNetworks():
            self.vns[vn['tenantId']] = vn
        self.__set_showers()
        self.file_name= ".history_"+ self.__class__.__name__+".txt"
        self.file= open(self.file_name,'a+',0)

    
    def __wait_for_subint(self, vn):
        while True: 
            try:
                vn.cmdloop()
            except SubinterpreterExit, e:
                if (e.drop):
                    print "Removed virtual network %s." % (vn.tid)
                    del self.vns[vn.tid]
                else:
                    print "Exiting virtual network %s context." % (vn.tid)
                    #Clearing history of previous context and loading Own History
                    readline.clear_history()
                    try:
                        self.file= open(self.file_name,'r+',0)
                        for line in self.file.readlines():
                            readline.add_history(line)
                    except IOError:
                        print "Can't read from the file: ",self.file_name

                break
            except exceptions.KeyboardInterrupt, e:
                print
                continue
            except Exception, e:
                    #(self.__exc_t, self.__exc_v, self.__exc_tb) = sys.exc_info()
                    #print self.__exc_t, sys.exc_info()
                    print e
                    continue
        
        
    def __set_showers(self):
        
        self.showers = { "topology" : PhysicalTopology(self.__ac), \
                         "flowtable" : PhysicalFlowtable(self.__ac), \
                         "vnets" : VNets(self.__ac) }
        
        
    def cmdloop(self):
        while True:
            try:
                Cmd.cmdloop(self)
            
            except exceptions.SystemExit, e:
                
                sys.exit()
            except exceptions.KeyboardInterrupt, e:
                
                print
                continue
            except:
                
                (self.__exc_t, self.__exc_v, self.__exc_tb) = sys.exc_info()
                print self.__exc_t, sys.exc_info()
                continue
            
    def __connect(self, host, user, port, passwd, isSSL):
        conn = Connection(user, passwd, host, port, isSSL)
        self.connections.append(conn)
        self.__ac = conn
        OVXCtl.prompt = "ovxctl$ "
        
    def help_show(self):
        print '{:<20} {}'.format('Command', 'Description')
        print '='.rjust(40, '=')
        for cmd, obj in self.showers.iteritems():
            print '{:<20} {}'.format(cmd, obj.help())
            
    
    def complete_show(self, text, line, begidx, endidx):
        terms = self.showers.keys()
        parts = line.split()
        if len(parts) == 1:
            return terms 
        if (len(parts) == 2 and parts[1] not in terms):
            return [ poss for poss in terms
                if poss.startswith(text) ]
        if (parts[1] in terms):
            if (len(parts) == 3):
                return self.showers[parts[1]].complete(parts[2])
            else:
                return self.showers[parts[1]].complete(None)
          
        
    def do_show(self, args):
        """
        displays information about the physical network (dataplane).
        """
        arglist = args.split()
        if len(arglist) != 1:
            print "Incomplete command: possible arguments are %s" % self.showers.keys() 
            return

        if (arglist[0] in self.showers):
            print arglist[0]
            self.showers[arglist[0]].pprint(*args.split())
        else:
            print "Unkown argument %s; possible values are %s." % (arglist[0], self.showers.keys()) 
            return
 
    #def help_vnet(self):
    #    print "Creates a virtual network. Usage: vnet [ctrl1[,ctrl2[,...]]"
            
    def do_vnet(self, args):
        """
        Creates a virtual network. 
        Optionally you can specify a controller.
        Usage: vnet [ctrl1[,ctrl2[,...]]
        """
        arglist = args.split()
        if len(arglist) == 0:
            vn_info = self.__ac.createNetwork([])
            vn = VirtualNetwork(vn_info, self.__ac)
        else:
            vn_info = self.__ac.createNetwork(arglist.split(','))
            vn = VirtualNetwork(vn_info, self.__ac)
           
        self.vns[vn.tid] = vn_info
        
        self.__wait_for_subint(vn)
        
            
    def do_conf(self, args):
        """
        enters a virtual network context.
        eg. conf 1
        """
        arglist = args.split()
        if len(arglist) != 1:
            print "Incomplete command: Enter the virtual network id"
            print "Usage: conf <vnetid>"
            return
        
        try:
            tid = int(args)
        except:
            print "Incomplete command: The vnet id must be a number" 
            print "Usage: conf <vnetid>"       
            return

        if tid in self.vns:
            vn = VirtualNetwork(self.vns[tid], self.__ac)
        else:
            """
            Don't want to create a network here because 
            I want OVX to handle all vnet numbering.
            """
            print "Virtual Network %s does not exist." % tid
            print "Use vnet command to create a new virtual network."
            return
            
        self.__wait_for_subint(vn)
        
            
    def do_exit(self, arg):
        print "Bye %s" % self.user
        sys.exit()
               
def print_version(option, opt, value, parser):
    """Prints the version of this cli"""
    print "ovxcli-%s\n" % VERSION
    print "supports OVX versions up to : %s" % SUPPORTED_OVX
    sys.exit(0)    
    
def print_help(option, opt, value, parser):
    """Prints help menu"""
    print parser.format_help().strip()
    sys.exit(0)

def parse_args(args):
    usage = "%s [options]" % sys.argv[0]
    parser = OptionParser(add_help_option=False, usage=usage)
    parser.add_option("-h", "--hostname", dest="host", default="localhost",
                      help="Specify the OpenVirteX host; default='localhost'")  
    parser.add_option("-p", "--port", dest="port", default="8080", 
                      help="Specify the OpenVirteX web port; default=8080")
    parser.add_option("-u", "--user", dest="user", default="admin",
                      help="OpenVirtex admin user; default='admin'")
    parser.add_option("-n", "--no-passwd", action="store_true",  dest="no_passwd", default=False,
                      help="Run ovxcli with no password; default false")
    parser.add_option("-s", "--ssl", action="store_true", dest="ssl", default=False,
                      help = "Connect to OVX using an SSL connection.")
    parser.add_option("-v", "--version", action="callback", callback=print_version)
    parser.add_option("--help", action="callback", callback=print_help)
    
    (options, args) = parser.parse_args()
    return options


if __name__ == '__main__':
    options = parse_args(sys.argv)
    passwd = getpass.getpass("Password: ")
    
    options.host = socket.gethostbyname(options.host)
    
    try:
        app = OVXCtl(options.host, options.user, options.port, passwd, options.ssl)
        app.cmdloop()
    except socket.error as e:
        if e.errno == errno.ECONNREFUSED:
            print "Connection refused: Either OVX is not running or it is not running at %s:%s" % (options.host, options.port)
        else:
            print "Error: %s" % e
    
    


