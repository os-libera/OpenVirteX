package net.onrc.openvirtex.elements.datapath;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
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

	/** Map of FlowMods to physical cookies for vlinks */
	protected ConcurrentHashMap<Long, OVXFlowMod> flowmodMap;
	protected SortedSet<OVXFlowEntry> flowTable;

	/**
	 * a temporary solution that should be replaced by something that doesn't
	 * fragment
	 */
	private final AtomicInteger cookieCounter;

	/**
	 * stores previously used cookies so we only generate one when this list is
	 * empty
	 */
	private final LinkedList<Long> freeList;
	private static final int FREELIST_SIZE = 1024;

	public OVXFlowTable(final OVXSwitch vsw) {
		this.flowmodMap = new ConcurrentHashMap<Long, OVXFlowMod>();
		this.flowTable = new TreeSet<OVXFlowEntry>();

		this.cookieCounter = new AtomicInteger(1);
		this.freeList = new LinkedList<Long>();
		this.vswitch = vsw;
	}

	public void handleFlowMod(final OVXFlowMod flowmod) {
		switch (flowmod.getCommand()) {

		}
	}

	/* flowmodMap ops */
	/**
	 * get a OVXFlowMod out of the map without removing it.
	 * 
	 * @param cookie
	 *            the physical cookie
	 * @return
	 */
	public OVXFlowMod getFlowMod(final Long cookie) {
		return this.flowmodMap.get(cookie);
	}

	/**
	 * Add a FlowMod to the mapping
	 * 
	 * @param flowmod
	 * @return the new physical cookie
	 */
	public long addFlowMod(final OVXFlowMod flowmod) {
		final long cookie = this.generateCookie();
		this.flowmodMap.put(cookie, flowmod);
		return cookie;
	}

	/**
	 * Remove an entry in the mapping
	 * 
	 * @param cookie
	 * @return
	 */
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
			final long cookie = this.cookieCounter.getAndIncrement();
			return (long) this.vswitch.getTenantId() << 32 | cookie;
		}
	}

}
