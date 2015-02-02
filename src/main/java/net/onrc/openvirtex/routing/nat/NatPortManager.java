package net.onrc.openvirtex.routing.nat;

import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.messages.OVXPacketIn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.util.LRULinkedHashMap;

public class NatPortManager {

    private static Logger log = LogManager.getLogger(NatPortManager.class.getName());
    private static AtomicReference<NatPortManager> managerInstance = new AtomicReference<>();

    // U16 do 16bit unsigned operation
    private TreeMap<Short, OVXIPAddress> portMap;
    protected LRULinkedHashMap<Short, NatTuple> portCache;


    public NatPortManager() {
        portMap = new TreeMap<Short, OVXIPAddress>();
        portCache = new LRULinkedHashMap<>(65535-1024);
    }

    public static NatPortManager getInstance() {
        NatPortManager.managerInstance.compareAndSet(null, new NatPortManager());
        return NatPortManager.managerInstance.get();
    }

    /**
     * Depending on the employed address and port mapping technique, this resource allocation can differ
     * @param srcIP
     * @param srcPort
     * @return
     */
//    public synchronized Short allocatePort(OVXIPAddress srcAddress, Short srcPort) {
//        portCache.put(key, new NatTuple(srcAddress, srcPort));
//    }
}
