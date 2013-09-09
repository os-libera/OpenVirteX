package net.onrc.openvirtex.elements.datapath;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.onrc.openvirtex.messages.OVXFlowMod;

/**
 * Virtualized version of the switch flow table. 
 * 
 */
public class OVXFlowTable {
    
    /** OVXSwitch tied to this table */
    protected OVXSwitch vswitch;
    
    /** Map of FlowMods to physical cookies for vlinks*/
    protected ConcurrentHashMap<Long, OVXFlowMod> flowmodMap;
    
    /** a temporary solution that should be replaced by something that doesn't fragment */
    private AtomicInteger cookieCounter;

    public OVXFlowTable(OVXSwitch vsw) {
	this.flowmodMap = new ConcurrentHashMap<Long, OVXFlowMod>();
	this.cookieCounter = new AtomicInteger(1);
	this.vswitch = vsw;
    }
    
    /**
     * get a OVXFlowMod out of the map
     * @param cookie the physical cookie
     * @return
     */
    public OVXFlowMod getFlowMod(Long cookie) {
	return this.flowmodMap.get(cookie);
    }
    
    /**
     * Add a FlowMod to the mapping
     * @param flowmod
     * @return the new physical cookie
     */
    public long addFlowMod(OVXFlowMod flowmod) {
	long cookie = generateCookie();
	this.flowmodMap.put(cookie, flowmod);
	return cookie;
    }
    
    /**
     * Remove an entry in the mapping
     * @param cookie
     * @return
     */
    public OVXFlowMod deleteFlowMod(Long cookie) {
	return this.flowmodMap.remove(cookie);
    }
    
    /**
     * Generate a new physical cookie from the OVXSwitch tenant ID and 
     * OVXSwitch-unique cookie counter. 	
     * @return a physical cookie
     */
    private long generateCookie() {
	return (((long)this.vswitch.getTenantId() << 32) | (long)this.cookieCounter.getAndIncrement());
    }
    
}
