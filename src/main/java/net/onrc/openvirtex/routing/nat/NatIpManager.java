package net.onrc.openvirtex.routing.nat;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.FloatingIPAddress;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.FloatingIPException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.packet.IPv4;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;

public class NatIpManager {

    private static Logger log = LogManager.getLogger(NatIpManager.class.getName());
    private static AtomicReference<NatIpManager> managerInstance = new AtomicReference<>();

    /* The followings can be part of OVXMap */
    // Remaining available public IPs, initially specified by the admin API call. Choose a better structure later Guava InetAddresses
    // PhysicalPort -> List<InetAddess>
    private Map<PhysicalPort, ArrayList<InetAddress>> availablePublicIPMap;
    // Allocated public IPs to tenants (Key: Floating-> Value: Virtual)
    private RadixTree<OVXIPAddress> allocatedFloatingIPMap;
    // (Key: Virtual -> Value: Floating)
    private RadixTree<FloatingIPAddress> virtualFloatingIPMap;


    public NatIpManager() {
        availablePublicIPMap = new ConcurrentHashMap<PhysicalPort, ArrayList<InetAddress>>();
        allocatedFloatingIPMap = new ConcurrentRadixTree<OVXIPAddress>(new DefaultCharArrayNodeFactory());
        virtualFloatingIPMap = new ConcurrentRadixTree<FloatingIPAddress>(new DefaultCharArrayNodeFactory());
    }

    public static NatIpManager getInstance() {
        NatIpManager.managerInstance.compareAndSet(null, new NatIpManager());
        return NatIpManager.managerInstance.get();
    }

    public synchronized FloatingIPAddress allocateFloatingIP(OVXPort ovxPort, OVXIPAddress virtualIP, boolean bidirectional) throws FloatingIPException{
        if (virtualIP == null) return null;
        FloatingIPAddress floatingIP = virtualFloatingIPMap.getValueForExactKey(virtualIP.toString());

        if (floatingIP != null) {
            log.debug("VirtualIP {} in Tenant {} already has a FloatingIP {}", virtualIP.toSimpleString(), virtualIP.getTenantId(), floatingIP.toSimpleString());
            return floatingIP;
        } else {
            InetAddress publicIP = getNextPublicIP(ovxPort.getPhysicalPort());
            floatingIP = new FloatingIPAddress(publicIP, ovxPort, bidirectional);
            allocatedFloatingIPMap.put(floatingIP.toString(), virtualIP);
            virtualFloatingIPMap.put(virtualIP.toString(), floatingIP);
            log.debug("VirtualIP {} in Tenant {} is assigned a new FloatingIP {}", virtualIP.toSimpleString(), virtualIP.getTenantId(), floatingIP.toSimpleString());
            return floatingIP;
        }
    }

    public boolean releaseFloatingIPAddress(FloatingIPAddress floatingIPAddress) throws FloatingIPException{
        OVXIPAddress virtualIP = allocatedFloatingIPMap.getValueForExactKey(floatingIPAddress.toString());
        boolean result = false;
        if (virtualIP != null) {
            result = allocatedFloatingIPMap.remove(floatingIPAddress.toString());
            result &= virtualFloatingIPMap.remove(virtualIP.toString());
            this.addPublicIP(floatingIPAddress.toSimpleString(), floatingIPAddress.getOvxPort().getPhysicalPort());
        }
        return result;
    }


    public synchronized void addPublicIP(String publicIPString, PhysicalPort physicalPort) throws FloatingIPException {
        InetAddress publicIP;
        try {
            publicIP = InetAddress.getByName(publicIPString);
            if (availablePublicIPMap == null) {
                availablePublicIPMap = new ConcurrentHashMap<PhysicalPort, ArrayList<InetAddress>>();
            }
            ArrayList<InetAddress> publicIPList = availablePublicIPMap.get(physicalPort);
            if (publicIPList == null) {
                publicIPList = new ArrayList<InetAddress>();
                availablePublicIPMap.put(physicalPort, publicIPList);
            }
            publicIPList.add(publicIP);
        } catch (UnknownHostException e) {
            log.error(e);
            throw new FloatingIPException("Can't add public IP", e);
        }
    }

    private synchronized InetAddress getNextPublicIP(PhysicalPort physicalPort) throws FloatingIPException{
        if (availablePublicIPMap == null || availablePublicIPMap.get(physicalPort).size() == 0){
            log.error("No Public IP left.");
            throw new FloatingIPException("No Public IP left.");
        } else {
            Iterator<InetAddress> floatingIPs = availablePublicIPMap.get(physicalPort).iterator();
            InetAddress floatingIp = floatingIPs.next();
            floatingIPs.remove();
            return floatingIp;
        }
    }

    public ArrayList<InetAddress> getPublicIP(PhysicalPort physicalPort){
        return availablePublicIPMap.get(physicalPort);
    }

    public Map<PhysicalPort, ArrayList<InetAddress>> getPublicIP(){
        return availablePublicIPMap;
    }

    public String getDefaultGatewayVirtualIP(Integer tenantId) throws NetworkMappingException{
        if (tenantId == null) return null;
        final OVXMap map = OVXMap.getInstance();
        final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId
                .intValue());
        String network = virtualNetwork.getNetwork().toSimpleString();
        // FIXME: I know this is terrible, leave it for now
        byte[] ip = IPv4.toIPv4AddressBytes(network);
        ip[3] = (byte) 254;
        String gatewayIp;
        try {
            gatewayIp = InetAddress.getByAddress(ip).getHostAddress();
            log.info("getDefaultGatewayVirtualIP tenant {} network {} gateway {}", tenantId, network, gatewayIp);
            return gatewayIp;
        } catch (UnknownHostException e) {
            log.error(e);
            return null;
        }
    }
}
