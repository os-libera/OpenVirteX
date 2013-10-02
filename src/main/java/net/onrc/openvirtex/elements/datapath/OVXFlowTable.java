package net.onrc.openvirtex.elements.datapath;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFError.OFFlowModFailedCode;

import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXMessageUtil;

/**
 * Virtualized version of the switch flow table.
 * 
 */
public class OVXFlowTable {
    
    private final Logger log = LogManager.getLogger(OVXFlowTable.class.getName());
    
    /** OVXSwitch tied to this table */
    protected OVXSwitch vswitch;
    
    /** Map of FlowMods to physical cookies for vlinks*/
    protected ConcurrentHashMap<Long, OVXFlowMod> flowmodMap;
    /** 
     * The actual flow table flow entries, sorted by priority field. Stores
     * the de-virtualized OFMatch i.e. after its address fields have been changed */
    protected SortedSet<OVXFlowEntry> flowTable;
    
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
	this.flowTable = new TreeSet<OVXFlowEntry>();
	this.cookieCounter = new AtomicInteger(1);
	this.freeList = new LinkedList<Long>();
	this.vswitch = vsw;
	
	/* initialise stats*/
	this.activeEntries = 0;
	this.lookupCount = 0;
	this.matchCount = 0;
    }
    
    public boolean isEmpty() {
	return this.flowTable.isEmpty();
    }

    /**
     * Process FlowMods according to command field, writing out FlowMods
     * south if needed.  
     * 
     * @param fm
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
	if (this.flowTable.isEmpty()) {
	    return false;
	}
	/* expand wildcard delete, remove all our entries */
	if (fm.getMatch().getWildcards() == OFMatch.OFPFW_ALL) {
	    /* make it exact? */
	    //fm.setCommand(OFFlowMod.OFPFC_DELETE_STRICT);

	    /* Send out FlowMod per flow entry. Since the entries are devirtualized
	     * already - Handle the sends here so we don't have to process the 
	     * FlowMod again just so they are sent south. */

	    for (OVXFlowEntry fe: this.flowTable) {
		OFMatch match = fe.getMatch();
		fm.setMatch(match);
		if (this.vswitch instanceof OVXBigSwitch) {
		    OVXPort iport = this.vswitch.getPort(match.getInputPort());
			((OVXBigSwitch) this.vswitch).sendSouthBS(fm, iport);
		} else {
			this.vswitch.sendSouth(fm);
		}
	    }
	    this.flowTable.clear();
	    return false;
	} else {
	    /* remove matching flow entries, and let FlowMod be sent down */
	    Iterator<OVXFlowEntry> itr = this.flowTable.iterator();
	    while(itr.hasNext()) {
		int overlap = itr.next().compare(fm.getMatch(), strict);
		if (overlap == OVXFlowEntry.EQUAL) {
		    itr.remove();
		}
	    }
	    return true;
	}
    }

    /**
     * In our case, just add if there are none identical, don't modify actions (for now). 
     * @param fm
     * @return true if FlowMod should be written south 
     */
    private boolean doFlowModModify(OVXFlowMod fm) {
	//TODO use physical switch DPID. 
	OVXFlowEntry fe = new OVXFlowEntry(fm, vswitch.getSwitchId());
	/* TODO replace entry that matches on equals(). */
	this.flowTable.add(fe);
	return true;
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
	    for (OVXFlowEntry fe : this.flowTable) {
		/* if not disjoint AND same priority send up OVERLAP error and drop it */
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
    
    /* flowmodMap ops */
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
	synchronized(this.freeList) {
	    if (this.freeList.size() <= FREELIST_SIZE) {
		//add/return cookie to freelist IF list is below FREELIST_SIZE
		this.freeList.add(cookie);
	    } else {
		//remove head element, then add
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
	    final long cookie = this.cookieCounter.getAndIncrement();
	    return (long) this.vswitch.getTenantId() << 32 | cookie;
	}
    }
    
    /**
     * dump the contents of the FlowTable
     */
    public void dump() {
	String ret = "";
	for (OVXFlowEntry fe : this.flowTable) {
	    ret += fe.toString() + "\n";
	}
	System.out.println("OVXFlowTable \n========================\n"+ret+"========================\n");
    }

}
