package net.onrc.openvirtex.routing.nat;

import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.exceptions.DuplicateIndexException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.util.BitSetIndex;
import net.onrc.openvirtex.util.BitSetIndex.IndexType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.util.LRULinkedHashMap;

public class NatPortManager {

    private static Logger log = LogManager.getLogger(NatPortManager.class.getName());
    private static AtomicReference<NatPortManager> managerInstance = new AtomicReference<>();
    private BitSetIndex natSrcPortIndex;
    // U16 do 16bit unsigned operation
    private LRULinkedHashMap<NatTuple, Short> portCache;


    public NatPortManager() {
        portCache = new LRULinkedHashMap<>(65535-1024);
        natSrcPortIndex = new BitSetIndex(IndexType.TCP_NAT);
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
     * @throws IndexOutOfBoundException
     */
    public synchronized Short allocatePort(OVXIPAddress srcAddress, Short srcPort) {
        NatTuple natTuple = new NatTuple(srcAddress, srcPort);
        Short natSrcPort = portCache.get(natTuple);
        if (natSrcPort == null){
            try {
                natSrcPort = getNextNatSrcPort();
            } catch (IndexOutOfBoundException e) {
                log.debug(e);
                // TODO: Cache should be cleaned now and a LRU natSrcPort being reused...
                // + deleting entries
            }
            portCache.put(natTuple, natSrcPort);
        }
        return natSrcPort;
    }

    private Short getNextNatSrcPort() throws IndexOutOfBoundException{
        return (short) (natSrcPortIndex.getNewIndex().shortValue() + 1023);
    }
}
