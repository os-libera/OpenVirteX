package net.onrc.openvirtex.elements.datapath;

import java.util.List;

import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.util.MACAddress;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

/**
 * Class representing a virtual flow entry.  
 * Loosely based on FlowEntry
 */
public class OVXFlowEntry implements Comparable<OVXFlowEntry>{
    
    /** relation of this FlowEntry to another FlowEntry during comparison */
    public static enum Union{ 
	EQUAL,		//exactly same 
	SUPERSET, 	//more general
	SUBSET, 	//more specific
	INTERSECT, 	//mix of wildcards and matching fields
	DISJOINT	//non-matching non-wildcarded fields 
    };
    
    /** of physical switch this entry maps back to */
    protected long dpid;
    
    /* Flow table match and actions */
    protected OFMatch ruleMatch;
    protected List<OFAction> actionsList;
    
    /* useful FlowMod fields */
    protected short priority;
    protected short outPort;
    protected short idleTimeout;
    protected short hardTimeout;
    
    /* Flow table statistics */
    protected int durationSeconds;
    protected int durationNanoseconds;
    protected long packetCount;
    protected long byteCount;
    
    public OVXFlowEntry(OVXFlowMod fm, long dpid) {
	/* fields set from FlowMod */
	this.dpid = dpid;
	this.priority = fm.getPriority();
	this.outPort = fm.getOutPort();
	this.ruleMatch = fm.getMatch();
	this.actionsList = fm.getActions();
	this.idleTimeout = fm.getIdleTimeout();
	this.hardTimeout = fm.getHardTimeout();
	
	/* initialise stats */
	this.durationSeconds = 0;
	this.durationNanoseconds = 0;
	this.packetCount = 0;
	this.byteCount = 0;
    }
   
    /**
     * Compares this entry against another, and tries to determine if
     * it is a superset, subset, or equal to it. Required for non-strict 
     * matching and overlap checking
     * <p>
     * For each field, we first check wildcard equality.
     * If both are equal, they are either 1 or 0. 
     * If 0, we further check for field equality. If the 
     * fields are not equal, the flow entries are considered
     * disjoint and we exit comparison. 
     * <p>
     * If both wildcards are not equal, we check if one 
     * subsumes the other. 
     * <p>
     * The result is tracked for each field in three ints -
     * equality, superset, and subset. 
     * At the end, either 1) one of the ints are 0x3fffff,
     * or 2) none are.
     * 
     * @param other The other FlowEntry to compare this one against. 
     * @return Union enum representing the relationship 
     */
    public Union compare(OFMatch omatch, boolean nostrict) {
	int equal = 0;
	int superset = 0;
	int subset = 0;
	
	OFMatch tmatch = this.ruleMatch;
	int twcard = tmatch.getWildcards();
	int owcard = omatch.getWildcards();
	
	/* inport */
	if ((twcard & OFMatch.OFPFW_IN_PORT) == (owcard & OFMatch.OFPFW_IN_PORT)) {
	    if (findDisjoint(twcard, OFMatch.OFPFW_IN_PORT, equal, 
		    tmatch.getInputPort(), omatch.getInputPort())) {
		return Union.DISJOINT;
	    }
	} else { /*check if super or subset*/
	    findRelation(twcard, owcard, OFMatch.OFPFW_IN_PORT, superset, subset);
	}
	
	/* L2 */
	if ((twcard & OFMatch.OFPFW_DL_SRC) == (owcard & OFMatch.OFPFW_DL_SRC)) {
	    if (findDisjoint(twcard, OFMatch.OFPFW_DL_SRC, equal, 
		    tmatch.getDataLayerSource(), omatch.getDataLayerSource())) {
		return Union.DISJOINT;
	    }
	} else { /*check if super or subset*/
	    findRelation(twcard, owcard, OFMatch.OFPFW_DL_SRC, superset, subset);
	}
	if ((twcard & OFMatch.OFPFW_DL_DST) == (owcard & OFMatch.OFPFW_DL_DST)) {
	    if (findDisjoint(twcard, OFMatch.OFPFW_DL_SRC, equal, 
		    tmatch.getDataLayerDestination(), omatch.getDataLayerDestination())) {
		return Union.DISJOINT;
	    }
	} else { /*check if super or subset*/
	    findRelation(twcard, owcard, OFMatch.OFPFW_DL_DST, superset, subset);
	}
	if ((twcard & OFMatch.OFPFW_DL_TYPE) == (owcard & OFMatch.OFPFW_DL_TYPE)) {
	    if (findDisjoint(twcard, OFMatch.OFPFW_DL_TYPE, equal, 
		    tmatch.getDataLayerType(), omatch.getDataLayerType())) {
		return Union.DISJOINT;
	    }
	} else { /*check if super or subset*/
	    findRelation(twcard, owcard, OFMatch.OFPFW_DL_TYPE, superset, subset);
	}
	if ((twcard & OFMatch.OFPFW_DL_VLAN) == (owcard & OFMatch.OFPFW_DL_VLAN)) {
	    if (findDisjoint(twcard, OFMatch.OFPFW_DL_VLAN, equal, 
		    tmatch.getDataLayerVirtualLan(), omatch.getDataLayerVirtualLan())) {
		return Union.DISJOINT;
	    }
	} else { /*check if super or subset*/
	    findRelation(twcard, owcard, OFMatch.OFPFW_DL_VLAN, superset, subset);
	}
	if ((twcard & OFMatch.OFPFW_DL_VLAN_PCP) == (owcard & OFMatch.OFPFW_DL_VLAN_PCP)) {
	    if (findDisjoint(twcard, OFMatch.OFPFW_DL_VLAN_PCP, equal, 
		    tmatch.getDataLayerVirtualLanPriorityCodePoint(), 
		    omatch.getDataLayerVirtualLanPriorityCodePoint())) {
		return Union.DISJOINT;
	    }
	} else { /*check if super or subset*/
	    findRelation(twcard, owcard, OFMatch.OFPFW_DL_VLAN_PCP, superset, subset);
	}
	
	/* L3 */
	if ((twcard & OFMatch.OFPFW_NW_PROTO) == (owcard & OFMatch.OFPFW_NW_PROTO)) {
	    if (findDisjoint(twcard, OFMatch.OFPFW_NW_PROTO, equal, 
		    tmatch.getNetworkProtocol(), omatch.getNetworkProtocol())) {
		return Union.DISJOINT;
	    }
	} else { /*check if super or subset*/
	    findRelation(twcard, owcard, OFMatch.OFPFW_NW_PROTO, superset, subset);
	}
	if ((twcard & OFMatch.OFPFW_NW_TOS) == (owcard & OFMatch.OFPFW_NW_TOS)) {
	    if (findDisjoint(twcard, OFMatch.OFPFW_NW_TOS, equal, 
		    tmatch.getNetworkTypeOfService(), omatch.getNetworkTypeOfService())) {
		return Union.DISJOINT;
	    }
	} else { /*check if super or subset*/
	    findRelation(twcard, owcard, OFMatch.OFPFW_NW_TOS, superset, subset);
	}
	if ((twcard & OFMatch.OFPFW_NW_DST_ALL) == (owcard & OFMatch.OFPFW_NW_DST_ALL)) {
	    if (findDisjoint(twcard, OFMatch.OFPFW_NW_DST_ALL, equal, 
		    tmatch.getNetworkDestination(), omatch.getNetworkDestination())) {
		return Union.DISJOINT;
	    }
	} else { /*check if super or subset*/
	    findRelation(twcard, owcard, OFMatch.OFPFW_NW_DST_ALL, superset, subset);
	}
	if ((twcard & OFMatch.OFPFW_NW_SRC_ALL) == (owcard & OFMatch.OFPFW_NW_SRC_ALL)) {
	    if (findDisjoint(twcard, OFMatch.OFPFW_NW_SRC_ALL, equal, 
		    tmatch.getNetworkSource(), omatch.getNetworkSource())) {
		return Union.DISJOINT;
	    }
	} else { /*check if super or subset*/
	    findRelation(twcard, owcard, OFMatch.OFPFW_NW_SRC_ALL, superset, subset);
	}
	if ((twcard & OFMatch.OFPFW_TP_SRC) == (owcard & OFMatch.OFPFW_TP_SRC)) {
	    if (findDisjoint(twcard, OFMatch.OFPFW_TP_SRC, equal, 
		    tmatch.getTransportSource(), omatch.getTransportSource())) {
		return Union.DISJOINT;
	    }
	} else { /*check if super or subset*/
	    findRelation(twcard, owcard, OFMatch.OFPFW_TP_SRC, superset, subset);
	}
	if ((twcard & OFMatch.OFPFW_TP_DST) == (owcard & OFMatch.OFPFW_TP_DST)) {
	    if (findDisjoint(twcard, OFMatch.OFPFW_TP_DST, equal, 
		    tmatch.getTransportDestination(), omatch.getTransportDestination())) {
		return Union.DISJOINT;
	    }
	} else { /*check if super or subset*/
	    findRelation(twcard, owcard, OFMatch.OFPFW_TP_DST, superset, subset);
	}
	
	if (nostrict) {
	    equal |= subset;
	}
	if (equal == OFMatch.OFPFW_ALL) {
	    return Union.EQUAL;
	}
	if (superset == OFMatch.OFPFW_ALL) {
	    return Union.SUPERSET;
	}
	if (subset == OFMatch.OFPFW_ALL) {
	    return Union.SUBSET;
	}
	return Union.INTERSECT;
    }
    
    /**
     * determine if a field is not equal-valued, for non-array fields 
     * @param wcard
     * @param field
     * @param equal
     * @param val1
     * @param val2
     * @return true if disjoint FlowEntries
     */
    protected boolean findDisjoint(int wcard, int field, int equal, 
	    Number val1, Number val2) {
	if (((wcard & field) == field) || (val1.equals(val2))) {
	    equal |= field;
	    return false;
	}
	return true;
    }
    
    /**
     * determine if fields are disjoint, for byte arrays.
     * @param wcard
     * @param field
     * @param equal
     * @param val1
     * @param val2
     * @return
     */
    protected boolean findDisjoint(int wcard, int field, int equal, 
	    byte [] val1, byte [] val2) {
	boolean match = true;
	
	if ((wcard & field) == field) {
	    equal |= field;
	    return false;
	}
	for (int i = 0; i < MACAddress.MAC_ADDRESS_LENGTH; i++) {
	    if (val1[i] != val2[i]) {
		match = false;
		break;
	    }
	}
	if (match) {
	    equal |= field;
	    return false;
	}
	return true;
    }
    
    protected void findRelation(int wcard1, int wcard2, int field, int sup, int sub) {
	if ((wcard1 & field) > (wcard2 & field)) {
	    sup |= field;
	} else {
	    sub |= field;
	}
    }
    
    /* non-stats fields */
    public OFMatch getMatch() {
	return this.ruleMatch;
    }
    
    public OVXFlowEntry setMatch(OFMatch match) {
	this.ruleMatch = match;
	return this;
    }
    
    public long getDPID() {
	return this.dpid;
    }
    
    public OVXFlowEntry setDPID(long dpid) {
	this.dpid = dpid;
	return this;
    }
    
    public short getOutport() {
	return this.outPort;
    }
    
    public OVXFlowEntry setOutport(short oport) {
	this.outPort = oport;
	return this;
    }
    
    public short getPriority() {
	return this.priority;
    }
    
    public OVXFlowEntry setPriority(short prio) {
	this.priority = prio;
	return this;
    }
    
    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result
		+ ((actionsList == null) ? 0 : actionsList.hashCode());
	result = prime * result + (int) (dpid ^ (dpid >>> 32));
	result = prime * result + priority;
	result = prime * result
		+ ((ruleMatch == null) ? 0 : ruleMatch.hashCode());
	return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	OVXFlowEntry other = (OVXFlowEntry) obj;
	if (actionsList == null) {
	    if (other.actionsList != null)
		return false;
	} else if (!actionsList.equals(other.actionsList)) {
	    return false;
	}
	if (dpid != other.dpid)
	    return false;
	if (priority != other.priority)
	    return false;
	if (ruleMatch == null) {
	    if (other.ruleMatch != null)
		return false;
	} else if (!ruleMatch.equals(other.ruleMatch)) {
	    return false;
	}
	return true;
    }

    @Override
    public int compareTo(OVXFlowEntry other) {
	// sort on priority, tie break on IDs
	if (this.priority != other.priority)
	    return other.priority - this.priority;
	return this.hashCode() - other.hashCode();
    }
	
}
