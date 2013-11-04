/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFError.OFFlowModFailedCode;

import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXMessageUtil;

/**
 * Virtualized version of the switch flow table.
 * 
 */
public class OVXFlowTable implements FlowTable {
    
    private final Logger log = LogManager.getLogger(OVXFlowTable.class.getName());
    
    /** OVXSwitch tied to this table */
    protected OVXSwitch vswitch;
    
    /** Map of FlowMods to physical cookies for vlinks*/
    protected ConcurrentHashMap<Long, OVXFlowMod> flowmodMap;

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
     * @param cookie the cookie value
     * @return if the FlowMod needs to be sent south during de-virtualization.
     */
    public boolean handleFlowMods(OVXFlowMod fm, Long cookie) {
    	switch (fm.getCommand()) {
		    case OFFlowMod.OFPFC_ADD:
		    	return doFlowModAdd(fm, cookie);
		    case OFFlowMod.OFPFC_MODIFY:
		    case OFFlowMod.OFPFC_MODIFY_STRICT:
		    	return doFlowModModify(fm, cookie);
		    case OFFlowMod.OFPFC_DELETE:
		    	return doFlowModDelete(fm, cookie, false);
		    case OFFlowMod.OFPFC_DELETE_STRICT:
		    	return doFlowModDelete(fm, cookie, true);
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
    private boolean doFlowModDelete(OVXFlowMod fm, Long cookie, boolean strict) {
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
    private boolean doFlowModAdd(OVXFlowMod fm, Long cookie) {
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
		return doFlowModModify(fm, cookie);
    }
    
    /**
     * Try to add the FlowMod to the table 
     * 
     * @param fm
     * @return true if FlowMod should be written south 
     */
    private boolean doFlowModModify(OVXFlowMod fm, Long cookie) {
		/* TODO replace entry that matches on equals(). */
		this.addFlowMod(fm, cookie);
		return true;
    }
    
    /* flowmodMap ops */
    /**
     * get a OVXFlowMod out of the map without removing it.
     * @param cookie the physical cookie
     * @return
     */
    public OVXFlowMod getFlowMod(Long cookie) {
    	return this.flowmodMap.get(cookie);
    }
    
    public long getCookie() {
    	return this.generateCookie();
    }
    
	public long addFlowMod(final OVXFlowMod flowmod, long cookie) {
		this.flowmodMap.put(cookie, flowmod);
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
			return this.flowmodMap.remove(cookie);
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
		for (final OVXFlowMod fe : this.flowmodMap.values()) {
			ret += fe.toString() + "\n";
		}
		this.log.info("OVXFlowTable \n========================\n" + ret
				+ "========================\n");
	}

	public Collection<OVXFlowMod> getFlowTable() {
		return Collections.unmodifiableCollection(this.flowmodMap.values());
	}
	
}
