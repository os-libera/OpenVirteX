package net.onrc.openvirtex.messages.statistics;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;

public interface VirtualizableStatistic {
    /**
     * 
     * Virtualize a statistics object.
     * 
     * @param sw
     *            Switch which received the object
     * @param msg
     *            the actual statistics message.
     */
    public void virtualizeStatistic(PhysicalSwitch sw, OVXStatisticsReply msg);

}
