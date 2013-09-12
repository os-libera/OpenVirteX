package net.onrc.openvirtex.elements.datapath;

import java.util.List;

import net.onrc.openvirtex.messages.OVXFlowMod;

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
	INTERSECT 	//none of above 
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
	
	/*initialise stats*/
	this.durationSeconds = 0;
	this.durationNanoseconds = 0;
	this.packetCount = 0;
	this.byteCount = 0;
    }
    
    /**
     * Compares this entry against another, and tries to determine if
     * it is a superset, subset, or equal to it. Required for non-strict 
     * matching and overlap checking
     * 
     * @param entry
     * @return Union enum representing the relationship 
     */
    public Union compare(OVXFlowEntry entry) {
	
	return Union.INTERSECT;
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
