package net.onrc.openvirtex.elements.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMessage;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.datapath.Dom0Switch;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.exceptions.InvalidHostException;
import net.onrc.openvirtex.exceptions.InvalidLinkException;
import net.onrc.openvirtex.exceptions.InvalidRouteException;
import net.onrc.openvirtex.exceptions.RoutingAlgorithmException;
import net.onrc.openvirtex.routing.RoutingAlgorithms;
import net.onrc.openvirtex.routing.SwitchRoute;
import net.onrc.openvirtex.util.MACAddress;

public class Dom0 extends OVXNetwork {
    
    private static Logger log = LogManager
            .getLogger(Dom0.class.getName());


    /*
     * The single bigswitch encapsulating the entire 
     * physical network.
     */
    private OVXBigSwitch sw = null;

    /**
     * Initializes dom0 and creates the single BigSwitch in it.
     */
    public Dom0() {
        super(0, new ArrayList<String>(), new OVXIPAddress("0.0.0.0", 0), (short) 255);
        this.createSwitch(new ArrayList<Long>(), ((long) 0xa42305 << 32) | this.tenantId );
    }

    @Override
    public void addControllers(ArrayList<String> ctrlUrls) {
        return;
    }

    @Override
    public Set<String> getControllerUrls() {
        return this.controllerUrls;
    }

    public void addSwitch(PhysicalSwitch psw) {
        OVXMap.getInstance().addSwitches(Collections.singletonList(psw), sw);
    }

    @Override
    public OVXSwitch createSwitch(final List<Long> dpids, final long switchId) {
        if (this.sw != null) {
            log.warn("dom0 bigswitch already exists.");
            return null;
        }
        /*
         * The switchId is generated using the ON.Lab OUI (00:A4:23:05) plus a
         * unique number inside the virtual network
         */
        final List<PhysicalSwitch> switches = new ArrayList<PhysicalSwitch>();
        // TODO: check if dpids are present in physical network
        for (final long dpid : dpids) {
            switches.add(PhysicalNetwork.getInstance().getSwitch(dpid));
        }
        sw = new Dom0Switch(switchId, this.tenantId, this);
        // Add switch to topology and register it in the map
        this.addSwitch(sw);

        sw.register(switches);
        if (this.isBooted()) {
            sw.boot();
        }
        log.debug("Created dom0 switch {}", sw.getName());
        return sw;
    }

    @Override
    public RoutingAlgorithms setOVXBigSwitchRouting(final long dpid, final String alg,
            final byte numBackups) 
            throws RoutingAlgorithmException {
        RoutingAlgorithms algorithm = new RoutingAlgorithms("SPF", (byte) 1);
        sw.setAlg(algorithm);
        log.debug("dom0 routing set to SPF.");
        return algorithm;
    }

    /*@Override
    public Host connectHost(final long ovxDpid, final short ovxPort, final MACAddress mac, 
            final int hostId) {
        throw new InvalidHostException("There can be no hosts in dom0.");
    }*/
    

    @Override
    public synchronized OVXLink connectLink(final long ovxSrcDpid, final short ovxSrcPort, 
            final long ovxDstDpid, final short ovxDstPort, final String alg, final byte numBackups, 
            final int linkId) {
        throw new InvalidLinkException("There can be no virtual links in dom0.");
    }

    @Override
    public synchronized OVXLink setLinkPath(final int linkId, final List<PhysicalLink> physicalLinks, 
            final byte priority) {
        throw new InvalidLinkException("There can be no virtual links in dom0.");
    }

    @Override
    public synchronized SwitchRoute connectRoute(final long ovxDpid, final short ovxSrcPort, 
            final short ovxDstPort, final List<PhysicalLink> physicalLinks, 
            final byte priority, final int... routeId) {
        throw new InvalidRouteException("Route cannot be explicitely created in dom0.");
    }

    @Override
    public synchronized void disconnectHost(final int hostId) {
        throw new InvalidHostException("Dom0 has no hosts");
    }

    @Override
    public synchronized void disconnectLink(final int linkId) {
        throw new InvalidLinkException("Dom0 has no links");
    }

    @Override
    public synchronized void startSwitch(final long ovxDpid) {
        log.info("Starting OpenVirteX dom0 network.");
        sw.boot();
    }

    @Override
    public synchronized void stopSwitch(final long ovxDpid) {
        log.info("Stopping OpenVirteX dom0 network.");
        sw.tearDown();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void handleLLDP(final OFMessage msg, final Switch sw) {
        return;
    }

    public void handleMsg(OFMessage msg) {
        log.warn("Got msg {}", msg);
        
    }
}
