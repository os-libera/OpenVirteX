from utils import *
from jsonrpclib import ProtocolError

    
class IProcess():
    def __init__(self):
        pass

    def help(self):
        raise NotImplementedError("Do not instantiate this class, you bad person")
    
    def complete(self, text, line):
        raise NotImplementedError("Do not instantiate this class, you bad person")
    
    def process(self, line):
        pass

class SingleSwitch(IProcess):
    def __init__(self, conn):
        self.ovx = conn
    
    def help(self):
        return "Defines a singleswitch. \n {:<20} {}" \
            .format("", "Usage: add singleswitch <dp1> [vdpid]")
        
 
    def complete(self, text, line):
        devices = self.ovx.listDevices()
        terms = line.split()
        if len(terms) == 3 and terms[-1] in devices:
            completions = ["<cr>", "dpid"]
        elif len(line.split()) > 3:
            completions = ["<cr>", "dpid"]
        else:
            if text is None:
                completions = devices
            else:
                completions = [ poss 
                           for poss in devices
                           if poss.startswith(text) ]
                
        return completions
    
    def process(self, line):
        terms = line.split()
        sw = [to_long_dpid(terms[1])]
        if "dpid" in terms:
            vdpid = to_long_dpid(terms[-1])
        else:
            vdpid = None
        return (sw, vdpid)        

class BigSwitch(IProcess):
    def __init__(self, conn):
        self.ovx = conn
    
    def help(self):
        return "Defines a bigswitch. \n {:<20} {}" \
            .format("", "Usage: add bigswitch <dp1> [extra dpids] [vdpid]")
        
    def complete(self, text, line = None):
        if ("dpid" in line.split()):
            return
        devices = self.ovx.listDevices()[:]
        
        if len(line.split()) > 3:
            devices.append("dpid")
        if text is None:
            completions = devices
        else:
            completions = [ poss 
                     for poss in devices
                     if poss.startswith(text) ]
            
        
        for term in line.split():
            if term in completions:
                completions.remove(term)
    
        return completions
    
    def process(self, line):
        terms = line.split()
        sws = []
        for sw in terms[1:]:
            if sw == "dpid":
                break
            sws.append(to_long_dpid(sw))
        
        if "dpid" in terms:
            vdpid = to_long_dpid(terms[-1])
        else:
             vdpid = None
             
        return (sws, vdpid)


class VirtualHostAdd(IProcess):
    def __init__(self, conn, tid, sw):
        self.ovx = conn
        self.tid = tid
        self.sw = sw

    def __prune(self, ports, links, sw):
        for link in links:
            if srcDpid is sw:
                ports.remove(srcPort)
            if dstDpid is sw:
                ports.remove(dstPort)

    def help(self):
        return "Adds a virtual port to this virtual switch \n {:<20} {}" \
                .format("", "Usage: add host <virtual_port> <host_mac>")
    
    def complete(self, text, line):
        if len(line.split()) > 2:
            return
        ports = self.ovx.getVirtualSwitchPorts(self.tid, to_long_dpid(self.sw)).keys()
        links = self.ovx.getVirtualTopology(self.tid)['links']
        self.__prune(ports, links, self.sw)
        str_ports = []
        for port in ports:
           if (port >= 0):
               str_ports.append(str(port))
        return str_ports
                 
        
    def process(self, port, mac):
        try:
            ret = self.ovx.connectHost(self.tid, to_long_dpid(self.sw), int(port), mac)
        except ProtocolError, e:
            (errcode, msg) =  e.message
            print msg
            return
        print "Created virtual host %s." % ret['hostId']

          

class VirtualPortAdd(IProcess):
    def __init__(self, conn, tid, mapped):
        self.ovx = conn
        self.tid = tid
        self.mapped = mapped

    def help(self):
        return "Adds a virtual port to this virtual switch. \n {:<20} {}" \
                .format("", "Usage: add port <dpid> <phys_port>")

    def complete(self, text, line):
        parts = line.split()
        if (len(parts) >= 4):
            return
        if (len(parts) == 3 and parts[2] in self.mapped):
            ports = self.ovx.getPhysicalSwitchPorts(to_long_dpid(parts[2]))
            str_ports = []
            for port in ports:
                if (port >= 0):
                    str_ports.append(str(port))
            
            return  str_ports
        if (len(parts) <= 3 and text not in self.mapped):
            if text is None:
                completions = self.mapped
            else:
                completions = [ poss 
                               for poss in self.mapped
                               if poss.startswith(text) ]
            return completions
        
    def process(self, sw, port):
        try:
            ret = self.ovx.createPort(self.tid, to_long_dpid(sw), int(port))
        except ProtocolError, e:
            (errcode, msg) =  e.message
            print msg
            return
        print "Created virtual port %s." % ret['vport']


class VirtualLinkAdd(IProcess):
    def __init__(self, conn, tid):
        self.ovx = conn
        self.tid = tid

    def __prune(self, ports, links, sw):
        for link in links:
            if srcDpid is sw:
                ports.remove(srcPort)
            if dstDpid is sw:
                ports.remove(dstPort)

    def help(self):
        return "Defines a link. \n {:<20} {}" \
            .format("", "Usage: add link <srcdp> <srcport> <dstdp> <dstport>")
  
    def complete(self, text, line):
        parts = line.split()
        if (len(parts) >= 6):
            return
        topo = self.ovx.getVirtualTopology(self.tid)
        if (len(parts) == 3 and parts[2] in topo['switches']) or (len(parts) == 5 and parts[4] in topo['switches']):
            index = 2
            if len(parts) > 3:
                index = 4
                
            ports = self.ovx.getVirtualSwitchPorts(self.tid, to_long_dpid(parts[index]))
            self.__prune(ports, topo['links'], parts[index])
            str_ports = []
            for port in ports:
                if (port >= 0):
                    str_ports.append(str(port))
            
            return  str_ports
        if (len(parts) <= 3 and text not in topo['switches']) or (len(parts) <= 5 and text not in topo['switches']):
            if text is None:
                completions = topo['switches']
            else:
                completions = [ poss 
                               for poss in topo['switches']
                               if poss.startswith(text) ]
            return completions
        
    def process(self, srcdp, dstdp, srcport, dstport, backup = 0):
        ret = self.ovx.connectLink(self.tid, to_long_dpid(srcdp), int(srcport), \
                    to_long_dpid(dstdp), int(dstport), backup = backup)
        print "Created virtual link %s." % ret['linkId']


 
