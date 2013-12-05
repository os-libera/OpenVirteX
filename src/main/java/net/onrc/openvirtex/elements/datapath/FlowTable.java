package net.onrc.openvirtex.elements.datapath;

import java.util.Collection;
import java.util.Map;

import net.onrc.openvirtex.exceptions.MappingException;
import net.onrc.openvirtex.messages.OVXFlowMod;

/**
 * Base interface for the flow table.  
 */
public interface FlowTable {
	
	/**
	 * Main method for handling FlowMods.  
	 * 
	 * @param fm The FlowMod to process.
	 * @cookie the new cookie generated for this FlowMod. 
	 * @return true if processing occured correctly
	 */
	public boolean handleFlowMods(OVXFlowMod fm, Long cookie);
	
	/**
	 * Fetch FlowMod out of this Flow Table based on cookie. 
	 * 
	 * @param cookie
	 * @return The FlowMod
	 * @throws MappingException 
	 */
	public OVXFlowMod getFlowMod(Long cookie) throws MappingException;
	
	/**
	 * Check if a FlowMod exists in the FlowTable
	 * @param cookie
	 * @return true if FlowMod exists
	 */
	public boolean hasFlowMod(long cookie);
	
	/**
	 * Remove a FlowMod from this table. Assumes a caller who uses 
	 * this method directly knows what they are doing, since they are 
	 * modifying the FlowTable by hand.  
	 * 
	 * @param cookie the cookie value of the FlowMod to delete. 
	 * @return The FlowMod that was removed. 
	 */
	public OVXFlowMod deleteFlowMod(final Long cookie); 
	
	/**
	 * Add a FlowMod to this table. The caller is assumed to know what 
	 * they are doing, since they will modify the FlowTable directly.
	 * 
	 * @param flowmod
	 * @param cookie
	 * @return the cookie value
	 */
	public long addFlowMod(final OVXFlowMod flowmod, long cookie); 
	
	/**
	 * @return The contents of this flow table. 
	 */
	public abstract Collection<OVXFlowMod> getFlowTable();
	
}
