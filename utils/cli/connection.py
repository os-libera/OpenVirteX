import jsonrpclib
import socket

class Connection():
    
    def __init__(self, user, passwd, host, port, isSSL = False):
        if (isSSL):
            self.tenant = jsonrpclib.Server("https://%s:%s@%s:%s/tenant" % (user, passwd, host, port))
            self.monitoring = jsonrpclib.Server("https://%s:%s@%s:%s/status" % (user, passwd, host, port))
        else:
            self.tenant = jsonrpclib.Server("http://%s:%s@%s:%s/tenant" % (user, passwd, host, port))
            self.monitoring = jsonrpclib.Server("http://%s:%s@%s:%s/status" % (user, passwd, host, port))
        
        self.__physlinks = None
        self.__devices = None
        

    def __fetchDevices(self, reset = False):
        if (self.__devices is None or reset):
            self.__populate_caches()
        return self.__devices
    
    listDevices = __fetchDevices
    
    def __fetchLinks(self, reset = False):
        if (self.__physlinks is None or reset):
            self.__populate_caches()
        return self.__physlinks
    
    listPhysicalLinks = __fetchLinks
    
    def __populate_caches(self):
        topo_info = self.monitoring.getPhysicalTopology()
        self.__devices = topo_info['switches']
        self.__physlinks = topo_info['links']
    
    #TODO for each existing virtual network pre-compute link mappings
    
    """
    Monitoring APIs
    """
        
    def getPhysicalFlowtable(self, dpid = None):
        if dpid is not None:
            dpid = int(dpid)
        return self.monitoring.getPhysicalFlowtable(**{"dpid" : dpid })
    
    def getPhysicalHosts(self):
        return self.monitoring.getPhysicalHosts()
    
    def getPhysicalSwitchPorts(self, dpid):
        return self.monitoring.getPhysicalSwitchPorts(**{"dpid" : dpid})
    
    def getVirtualAddressMapping(self, tid):
        return self.monitoring.getVirtualAddressMapping(**{"tenantId" : tid})
    
    def getVirtualFlowtable(self, tid, vdpid = None):
        return self.monitoring.getVirtualFlowtable(**{"tenantId" : tid, "vdpid" : vdpid})
    
    def getVirtualHosts(self, tid):
        return self.monitoring.getVirtualHosts(**{"tenantId" : tid})
    
    def getVirtualLinkMapping(self, tid):
        return self.monitoring.getVirtualLinkMapping(**{"tenantId" : tid})
    
    def getVirtualSwitchMapping(self, tid):
        return self.monitoring.getVirtualSwitchMapping(**{"tenantId" : tid})
    
    def getVirtualSwitchPorts(self, tid, vdpid):
        return self.monitoring.getVirtualSwitchPorts(**{"tenantId" : tid, "vdpid" : vdpid})
    
    def getVirtualTopology(self, tid):
        return self.monitoring.getVirtualTopology(**{"tenantId" : tid})
    
    def listVirtualNetworks(self):
        return self.monitoring.listVirtualNetworks()
    
    """
    Tenant APIs
    """
    
    def addController(self, tid, ctrls):
        data = { "tenantId" : tid, \
                    "controllerUrls" : ctrls }
        
        return self.tenant.addControllers(**data)

    def removeController(self, tid, ctrls):
        data = { "tenantId" : tid, \
                    "controllerUrls" : ctrls }
        
        return self.tenant.removeControllers(**data)
 
    
    def connectHost(self, tid, vdpid, vport, mac):
        data = { "tenantId" : tid, \
                    "vdpid" : vdpid, \
                    "vport" : vport, \
                    "mac" : mac }
        
        return self.tenant.connectHost(**data)
    
    def connectLink(self, tid, srcDpid, srcPort, dstDpid, dstPort, alg = "SPF", backup = 0):
        data = { "tenantId" : tid, \
                    "srcDpid" : srcDpid, \
                    "srcPort" : srcPort, \
                    "dstDpid" : dstDpid, \
                    "dstPort" : dstPort, \
                    "algorithm" : alg, \
                    "backup_num" : backup }
        
        return self.tenant.connectLink(**data)
    
    def connectRoute(self, tid, vdpid, srcPort, dstPort, path, priority):
        data = { "tenantId" : tid, \
                    "vdpid" : vdpid, \
                    "srcPort" : srcPort, \
                    "dstPort" : dstPort, \
                    "path" : path, \
                    "priority" : priority }
        
        return self.tenant.connectRoute(**data)
    
    def createNetwork(self, ctrls, mask = 0, networkAddress = "0.0.0.0"):
        data = { "controllerUrls" : ctrls, \
                    "mask" : mask, \
                    "networkAddress" : networkAddress }
        
        return self.tenant.createNetwork(**data)
    
    def createPort(self, tid, dpid, port):
        data = { "tenantId" : tid, \
                    "dpid" : dpid, \
                    "port" : port }
                
        return self.tenant.createPort(**data)
    
    def createSwitch(self, tid, dpids, vdpid = None):
        data = { "tenantId" : tid, \
                    "dpids" : dpids, \
                    "vdpid" : vdpid }
                
        return self.tenant.createSwitch(**data)
    
    def disconnectHost(self, tid, host):
        data = { "tenantId" : tid, \
                    "hostId" : host }    
    
        return self.tenant.disconnectHost(**data)
    
    def disconnectLink(self, tid, link):
        data = { "tenantId" : tid, \
                    "linkId" : link }    
    
        return self.tenant.disconnectLink(**data)
    
    def disconnectRoute(self, tid, vdpid, route):
        data = { "tenantId" : tid, \
                    "vdpid"  : vdpid, \
                    "routeId" : route }    
    
        return self.tenant.disconnectRoute(**data)
    
    def removeNetwork(self, tid):
        data = { "tenantId" : tid }    
    
        return self.tenant.removeNetwork(**data)
    
    def removePort(self, tid, vdpid, vport):
        data = { "tenantId" : tid, \
                    "vdpid"  : vdpid, \
                    "vport" : vport }    
    
        return self.tenant.removePort(**data)
    
    def removeSwitch(self, tid, vdpid):
        data = { "tenantId" : tid, \
                    "vdpid"  : vdpid }    
    
        return self.tenant.removeSwitch(**data)
    
    
    def setBigSwitchRouting(self, tid, vdpid, srcPort, dstPort, path, priority):
        data = { "tenantId" : tid, \
                    "vdpid" : vdpid, \
                    "algorithm" : alg, \
                    "backup_num" : backup }
        
        return self.tenant.setInternalRouting(**data)
    
    def setLinkPath(self, tid, link, path, priority):
        data = { "tenantId" : tid, \
                    "linkId" : link, \
                    "path" : path, \
                    "priority" : priority }
        
        return self.tenant.setLinkPath(**data)
    
    def startNetwork(self, tid):
        data = { "tenantId" : tid }    
    
        return self.tenant.startNetwork(**data)
    
    def startPort(self, tid, vdpid, vport):
        data = { "tenantId" : tid, \
                    "vdpid"  : vdpid, \
                    "vport" : vport }    
    
        return self.tenant.startPort(**data)
    
    def startSwitch(self, tid, vdpid):
        data = { "tenantId" : tid, \
                    "vdpid"  : vdpid }    
    
        return self.tenant.startSwitch(**data)
    
    def stopNetwork(self, tid):
        data = { "tenantId" : tid }    
    
        return self.tenant.stopNetwork(**data)
    
    def stopPort(self, tid, vdpid, vport):
        data = { "tenantId" : tid, \
                    "vdpid"  : vdpid, \
                    "vport" : vport }    
    
        return self.tenant.stopPort(**data)
    
    def stopSwitch(self, tid, vdpid):
        data = { "tenantId" : tid, \
                    "vdpid"  : vdpid }    
    
        return self.tenant.stopSwitch(**data)

