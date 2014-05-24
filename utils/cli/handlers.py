from utils import *


class IHandler():
    def __init__(self):
        pass
    
    def show(self, *args):
        raise NotImplementedError("Do not instantiate this class, you bad person")
    
    def pprint(self):
        raise NotImplementedError("Do not instantiate this class, you bad person")
    
    def help(self):
        raise NotImplementedError("Do not instantiate this class, you bad person")
    
    def complete(self):
        raise NotImplementedError("Do not instantiate this class, you bad person")

class PhysicalFlowtable(IHandler):
    def __init__(self, conn):
        self.ovx = conn
        
    def show(self, *args):
        try:
            (cmd, device) = args[0]
        except exceptions.ValueError, e:
            device = None
            cmd = "flowtable"
        assert cmd == "flowtable"
        if (device is None):
            param = None
        elif (":" in device):
            param = int(device.replace(":",""),16)
        else:
            param = int(device)
        return self.ovx.getPhysicalFlowtable(param)
    
    def pprint(self, *args):
        print self.show(args)
        
    def help(self):
        return "Fetches the physical flowtable. \n {:<20} {}" \
            .format("", "Usage: show flowtable [dpid]")
        
    def complete(self, text):
        devices = self.ovx.listDevices()
        
        if text is None:
            completions = devices
        else:
            completions = [ poss 
                           for poss in devices
                           if poss.startswith(text) ]
            
        return completions

class PhysicalTopology(IHandler):
    def __init__(self, conn):
        self.ovx = conn
        
    def show(self, *args):
        return self.ovx.listPhysicalLinks()
    
    def pprint(self, *args):
        data = self.show(args)
        print "{:<23} {:<3}  {:>20} {:>10}" \
            .format("  src dpid", "port","dst dpid", "port")
        print '='.rjust(61, '=')
        for d in data:
            print "{:<23} {:>3} <--> {:<23} {:>3}" \
                .format(d['src']['dpid'], d['src']['port'], \
                        d['dst']['dpid'], d['dst']['port'])
        
    def help(self):
        return "Displays the topology."
        
    def complete(self, text):
        pass
    
class VirtualTopology(PhysicalTopology):
    
    def __init__(self, conn, tid):
        PhysicalTopology.__init__(self, conn)
        self.ovx = conn
        self.tid = tid
    
    def show(self, *args):
        return self.ovx.getVirtualTopology(self.tid)['links']

    def pprint(self, *args):
        data = self.show(args)
        print " {:<3}  {:<23} {:<3}  {:>20} {:>10}" \
            .format("id","  src dpid", "port","dst dpid", "port")
        print '='.rjust(65, '=')
        for d in data:
            print " {:<3}  {:<23} {:>3} <--> {:<23} {:>3}" \
                .format(int(d['linkId']), d['src']['dpid'], d['src']['port'], \
                        d['dst']['dpid'], d['dst']['port'])
     

    def help(self):
        return "Shows the virtual topology for this virtual network"

class VirtualDevices(IHandler):
    
    def __init__(self, conn, tid):
        self.ovx = conn
        self.tid = tid
    
    def show(self, *args):
        return self.ovx.getVirtualTopology(self.tid)['switches']
    
    def pprint(self, *args):
        data = self.show(args)
        print "Existing Virtual Switches: "
        for d in data:
            print "    %s" % d

    def help(self):
        return "Shows the virtual switches in this virtual network"


    
class VirtualSwitchMapping(IHandler):
    def __init__(self, conn, tid):
        self.ovx = conn
        self.tid = tid

    def help(self):
        return "Displays what physical switch this switch is mapped to." 
    
    def show(self, args):
        return self.ovx.getVirtualSwitchMapping(self.tid)[args[0]]['switches']
    
    def pprint(self, *args):
        data = self.show(args)
        print "Virtual Switch %s mapped to: " % args[0]
        for d in data:
            print "    %s" % d
            
class VirtualFlowtable(IHandler):
    def __init__(self, conn, tid):
        self.ovx = conn
        self.tid = tid
        
    def show(self, *args):
        try:
            (cmd, device) = args[0]
        except exceptions.ValueError, e:
            device = None
            cmd = "flowtable"
        assert cmd == "flowtable"
        if (device is None):
            param = None
        elif (":" in device):
            param = int(device.replace(":",""),16)
        else:
            param = int(device)
        return self.ovx.getVirtualFlowtable(self.tid, param)
    
    def pprint(self, *args):
        print self.show(args)
        
    def help(self):
        return "Fetches the virtual flowtable." 
        
    def complete(self, text):
        devices = self.ovx.getVirtualTopology(self.tid)['switches']
        
        if text is None:
            completions = devices
        else:
            completions = [ poss 
                           for poss in devices
                           if poss.startswith(text) ]
            
        return completions
    
class VirtualPorts(IHandler):
    def __init__(self, conn, tid):
        self.ovx = conn
        self.tid = tid
        
    def show(self, args):
        return self.ovx.getVirtualSwitchPorts(self.tid, to_long_dpid(args))
    
    def pprint(self, args):
        data = self.show(args)
        print "{:<10}  {:>15} {:>15}" \
            .format(" vPort", "dpid","port")
        print '='.rjust(45, '=')
        for (k,v) in data.iteritems():
            print "   {:<10}  {:<20} {:>3}" \
                .format(k, v['dpid'], v['port'])
        
    def help(self):
        return "Shows the virtual ports on this switch as well as their mapping" 
        
    def complete(self, text):
        pass

class VNets(IHandler):
    def __init__(self, conn):
        self.ovx = conn
        
    def show(self, *args):
        return self.ovx.listVirtualNetworks()
    
    def pprint(self, *args):
        data = self.show(args)
        print "Existing Virtual Networks:"
        for d in data:
            print "    Virtual Network %s" % d['tenantId']
        
    def help(self):
        return "Displays the available virtual networks."
        
    def complete(self, text):
        pass    


class VirtualHosts(IHandler):
    def __init__(self, conn, tid):
        self.ovx = conn
        self.tid = tid
        
    def show(self, args):
        return self.ovx.getVirtualHosts(self.tid)
    
    def pprint(self, args):
        data = self.show(None)
        print "Attached Virtual Hosts:"
        for d in data:
            if d['dpid'] == args:
                print "    Virtual Host %s on port %s with mac %s." % (int(d['hostId']), \
                                                                 int(d['port']), d['mac'])
        
    def help(self):
        return "Displays the attached virtual hosts."
        
    def complete(self, text):
        pass    

