package net.onrc.openvirtex.elements.datapath;

import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
 
    /** stores previously used cookies so we only generate one when this list is empty */
    private Queue<Long> freeList;

    public OVXFlowTable(OVXSwitch vsw) {
	this.flowmodMap = new ConcurrentHashMap<Long, OVXFlowMod>();
	this.cookieCounter = new AtomicInteger(1);
	//maybe a circular FIFO queue instead to limit size - how big?
	this.freeList = new ConcurrentLinkedQueue<Long>();
	this.vswitch = vsw;
    }
    
    /**
     * get a OVXFlowMod out of the map without removing it.
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
	//add/return cookie to freelist
	this.freeList.add(cookie);
	return this.flowmodMap.remove(cookie);
    }
    
    /**
     * Fetch a usable cookie for FlowMod storage. If no cookies are available, 
     * generate a new physical cookie from the OVXSwitch tenant ID and 
     * OVXSwitch-unique cookie counter. 	
     * @return a physical cookie
     */
    private long generateCookie() {
	try {
	    return this.freeList.remove();
	} catch (NoSuchElementException e) {
	    //none in queue - generate new cookie
	    long cookie = this.cookieCounter.getAndIncrement();
	    return (((long)this.vswitch.getTenantId() << 32) | cookie);
	}
    }
   
}
