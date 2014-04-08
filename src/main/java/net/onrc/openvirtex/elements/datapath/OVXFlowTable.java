/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFError.OFFlowModFailedCode;

import net.onrc.openvirtex.exceptions.MappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXMessageUtil;

/**
 * Virtualized version of the switch flow table.
 */
public class OVXFlowTable implements FlowTable {
	
    private final Logger log = LogManager.getLogger(OVXFlowTable.class.getName());
    
    /** OVXSwitch tied to this table */
    protected OVXSwitch vswitch;
    
    /** Map of FlowMods to physical cookies for vlinks*/
    protected ConcurrentHashMap<Long, OVXFlowMod> flowmodMap;
    /** Reverse map of FlowMod hashcode to cookie */
    protected ConcurrentHashMap<Integer, Long> cookieMap;
    
    /** a temporary solution that should be replaced by something that doesn't fragment */
    private AtomicInteger cookieCounter;
 
    /** stores previously used cookies so we only generate one when this list is empty */
    private LinkedList<Long> freeList;
    private static final int FREELIST_SIZE = 1024;
    
    /* statistics per specs */
    protected int activeEntries;
    protected long lookupCount;
    protected long matchCount;

    public OVXFlowTable(OVXSwitch vsw) {
		this.flowmodMap = new ConcurrentHashMap<Long, OVXFlowMod>();
		this.cookieMap = new ConcurrentHashMap<Integer, Long>();
		this.cookieCounter = new AtomicInteger(1);
		this.freeList = new LinkedList<Long>();
		this.vswitch = vsw;
		
		/* initialise stats*/
		this.activeEntries = 0;
		this.lookupCount = 0;
		this.matchCount = 0;
    }
    
    public boolean isEmpty() {
    	return this.flowmodMap.isEmpty();
    }

    /**
     * Process FlowMods according to command field, writing out FlowMods
     * south if needed.  
     * 
     * @param fm The FlowMod to apply to this table 
     * @param cookie the cookie value - FlowTable will return the cookie value
     * that it decided to use in this value. This may be a cookie already in 
     * the table, or the one supplied to this method. 
     * @return if the FlowMod needs to be sent south during de-virtualization.
     */
    public boolean handleFlowMods(OVXFlowMod fm) { 
    	switch (fm.getCommand()) {
		    case OFFlowMod.OFPFC_ADD:
		    	return doFlowModAdd(fm);
		    case OFFlowMod.OFPFC_MODIFY:
		    case OFFlowMod.OFPFC_MODIFY_STRICT:
		    	return doFlowModModify(fm);
		    case OFFlowMod.OFPFC_DELETE:
		    	return doFlowModDelete(fm, false);
		    case OFFlowMod.OFPFC_DELETE_STRICT:
		    	return doFlowModDelete(fm, true);
		    default:
			/* we don't know what it is. drop. */ 
		    	return false;
		}
    }
    
    /**
     * Delete an existing FlowEntry, expanding out a OFPFW_ALL delete
     * sent initially be a controller. If not, just check for entries, 
     * and only allow entries that exist here to be deleted.   
     * @param fm
     * @param nostrict true if not a _STRICT match
     * @return true if FlowMod should be written south 
     */
    private boolean doFlowModDelete(OVXFlowMod fm, boolean strict) {
		/* don't do anything if FlowTable is empty */
		if (this.flowmodMap.isEmpty()) {
		    return false;
		}
		/* fetch our vswitches */
		try {
			/* expand wildcard delete, remove all entries pertaining just to this tenant */
			if (fm.getMatch().getWildcards() == OFMatch.OFPFW_ALL) {
				List<PhysicalSwitch> p_list 
						= this.vswitch.getMap().getPhysicalSwitches(this.vswitch);
				for (PhysicalSwitch psw : p_list) {
					/* do FlowMod cleanup like when port dies. */ 
					psw.cleanUpTenant(this.vswitch.getTenantId(), OFPort.OFPP_NONE.getValue());
		    	}
				this.flowmodMap.clear();
				this.cookieMap.clear();
			    return false;
			} else {
			    /* remove matching flow entries, and let FlowMod be sent down */
				Iterator<Map.Entry<Long, OVXFlowMod>> itr = this.flowmodMap.entrySet().iterator();
				OVXFlowEntry fe = new OVXFlowEntry();
			    while(itr.hasNext()) {
			    	Map.Entry<Long, OVXFlowMod> entry = itr.next();
			    	fe.setFlowMod(entry.getValue());
					int overlap = fe.compare(fm.getMatch(), strict);
					if (overlap == OVXFlowEntry.EQUAL) {
						this.cookieMap.remove(entry.getValue().hashCode());
					    itr.remove();
					}
			    }
			    return true;
			}
		} catch (SwitchMappingException e) {
			log.warn("Could not clear PhysicalSwitch tables: {}", e);
		}
		return false;
    }

    /**
     * Adds a flow entry to the FlowTable. The FlowMod is checked for 
     * overlap if its flag says so. 
     * @param fm
     * @return true if FlowMod should be written south 
     */
    private boolean doFlowModAdd(OVXFlowMod fm) {
		if ((fm.getFlags() & OFFlowMod.OFPFF_CHECK_OVERLAP) 
				== OFFlowMod.OFPFF_CHECK_OVERLAP) {
			OVXFlowEntry fe = new OVXFlowEntry();
		    for (OVXFlowMod fmod : this.flowmodMap.values()) {
				/* if not disjoint AND same priority send up OVERLAP error and drop it */
		    	fe.setFlowMod(fmod);
				int res = fe.compare(fm.getMatch(), false);
				if ((res != OVXFlowEntry.DISJOINT) & (fm.getPriority() == fe.getPriority())) {
				    this.vswitch.sendMsg(OVXMessageUtil.makeErrorMsg(
						OFFlowModFailedCode.OFPFMFC_OVERLAP, fm), this.vswitch);
				    return false;
				}
		    }
		}
		return doFlowModModify(fm);
    }
    
    /**
     * Try to add the FlowMod to the table 
     * 
     * @param fm
     * @return true if FlowMod should be written south 
     */
    private boolean doFlowModModify(OVXFlowMod fm){
    	OVXFlowEntry fe = new OVXFlowEntry();
		int res;
		for (Map.Entry<Long, OVXFlowMod> fmod : this.flowmodMap.entrySet()) {
			fe.setFlowMod(fmod.getValue());
			res = fe.compare(fm.getMatch(), true);
			/* replace table entry that strictly matches with given FlowMod. */
	    	if (res == OVXFlowEntry.EQUAL) {
				long c = fmod.getKey();
				log.info("replacing equivalent FlowEntry [cookie={}]", c);
				OVXFlowMod old = this.flowmodMap.get(c);
				this.cookieMap.remove(old.hashCode());
				this.addFlowMod(fm, c);
				/*return cookie to pool and use the previous cookie*/
				return true;
			}
    	}
		/*make a new cookie, add FlowMod*/
		long newc = this.getCookie();
		this.addFlowMod(fm.clone(), newc);
		return true;
    }
    
    /* flowmodMap ops */
    /**
     * get a OVXFlowMod out of the map without removing it.
     * @param cookie the physical cookie
     * @return a clone of the stored FlowMod, if found.
     * @throws MappingException 
     */
    public OVXFlowMod getFlowMod(Long cookie) throws MappingException {
    	OVXFlowMod fm =  this.flowmodMap.get(cookie);
    	if (fm == null) {
    		throw new MappingException(cookie, OVXFlowMod.class);
    	}
    	return fm.clone();
    }
    
    public boolean hasFlowMod(long cookie) {
		return this.flowmodMap.containsKey(cookie);
	}
	
    public long getCookie() {
    	return this.generateCookie();
    }
    
    public final long getCookie(OVXFlowMod flowmod, Boolean cflag) {
    	if (cflag) {
    		long cookie = this.getCookie();
    		OVXFlowEntry fe = new OVXFlowEntry();
    		int res;
    		for (Map.Entry<Long, OVXFlowMod> fmod : this.flowmodMap.entrySet()) {
    			fe.setFlowMod(fmod.getValue());
    			res = fe.compare(flowmod.getMatch(), true);
    			/* replace table entry that strictly matches with given FlowMod. */
    	    	if (res == OVXFlowEntry.EQUAL) {
    				long c = fmod.getKey();
    				log.info("replacing equivalent FlowEntry with new [cookie={}]", cookie);
    				OVXFlowMod old = this.flowmodMap.get(c);
    				this.cookieMap.remove(old.hashCode());
    				this.flowmodMap.remove(c);
    				this.addFlowMod(flowmod, cookie);
    				/*return cookie to pool and use the previous cookie*/
    				return cookie;
    			}
        	}
    		
    	}
    	Long cookie = this.cookieMap.get(flowmod.hashCode());
    	if (cookie == null) {
    		cookie = this.getCookie();
    	}
    	return cookie;
    }
    
	public long addFlowMod(final OVXFlowMod flowmod, long cookie) {
		this.flowmodMap.put(cookie, flowmod);
		this.cookieMap.put(flowmod.hashCode(), cookie);
		return cookie;
	}

	public OVXFlowMod deleteFlowMod(final Long cookie) {
		synchronized (this.freeList) {
			if (this.freeList.size() <= OVXFlowTable.FREELIST_SIZE) {
				// add/return cookie to freelist IF list is below FREELIST_SIZE
				this.freeList.add(cookie);
			} else {
				// remove head element, then add
				this.freeList.remove();
				this.freeList.add(cookie);
			}
			OVXFlowMod ret = this.flowmodMap.remove(cookie);
			if (ret != null) {
				this.cookieMap.remove(ret.hashCode());
			}
			return ret;
		}
	}

	/**
	 * Fetch a usable cookie for FlowMod storage. If no cookies are available,
	 * generate a new physical cookie from the OVXSwitch tenant ID and
	 * OVXSwitch-unique cookie counter.
	 * 
	 * @return a physical cookie
	 */
	private long generateCookie() {
		try {
			return this.freeList.remove();
		} catch (final NoSuchElementException e) {
			// none in queue - generate new cookie
			// TODO double-check that there's no duplicate in flowmod map.
			final int cookie = this.cookieCounter.getAndIncrement();
			return (long) this.vswitch.getTenantId() << 32 | cookie;
		}
	}

	/**
	 * dump the contents of the FlowTable
	 */
	public void dump() {
		String ret = "";
		for (final Map.Entry<Long, OVXFlowMod> fe : this.flowmodMap.entrySet()) {
			ret += "cookie["+fe.getKey() + "] :" + fe.getValue().toString() + "\n";
		}
		this.log.info("OVXFlowTable \n========================\n" + ret
				+ "========================\n");
	}

	public Collection<OVXFlowMod> getFlowTable() {
		return Collections.unmodifiableCollection(this.flowmodMap.values());
	}

}
