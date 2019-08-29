/*
 * ******************************************************************************
 *  Copyright 2019 Korea University & Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ******************************************************************************
 *  Developed by Libera team, Operating Systems Lab of Korea University
 *  ******************************************************************************
 */
package net.onrc.openvirtex.elements.datapath;

import java.util.*;

import net.onrc.openvirtex.messages.OVXFlowMod;


import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;


/**
 * Class representing a virtual flow entry - a wrapper for FlowMods that enables
 * the flow table to do matching on contents.
 */
public class OVXFlowEntry implements Comparable<OVXFlowEntry> {

    /* relation of this FlowEntry to another FlowEntry during comparison */
    public static final int EQUAL = 0; // exactly same
    public static final int SUPERSET = 1; // more general
    public static final int SUBSET = 2; // more specific
    public static final int INTERSECT = 3; // mix of wildcards and matching fields
    public static final int DISJOINT = 4; // non-matching non-wildcarded fields

    // The FlowMod this Entry represents
    protected OVXFlowMod ovxFlowMod;
    // The newly generated cookie for the FlowMod
    protected long newcookie;

    public OVXFlowEntry() {
    }

    public OVXFlowEntry(OVXFlowMod fm, long cookie) {
        this.ovxFlowMod = fm.clone();
        this.newcookie = cookie;
    }

    /**
     * Compares this entry against another, and tries to determine if it is a
     * superset, subset, or equal to it. Required for non-strict matching and
     * overlap checking
     * <p>
     * For each field, we first check wildcard equality. If both are equal, they
     * are either 1 or 0. If 0, we further check for field equality. If the
     * fields are not equal, the flow entries are considered disjoint and we
     * exit comparison.
     * <p>
     * If both wildcards are not equal, we check if one subsumes the other.
     * <p>
     * The result is tracked for each field in three ints - equality, superset,
     * and subset. At the end, either 1) one of the ints are 0x3fffff, or 2)
     * none are.
     *
     * @param omatch
     *            The other FlowEntry to compare this one against.
     * @param strict
     *            whether FlowMod from which the match came was strict or not.
     * @return Union enum representing the relationship
     */
    public int compare(Match omatch, boolean strict) {
        Set<MatchField>[] intersect = new HashSet[3];
        intersect[EQUAL] = new HashSet<MatchField>();
        intersect[SUPERSET] = new HashSet<MatchField>();
        intersect[SUBSET] = new HashSet<MatchField>();

        Match tmatch = this.ovxFlowMod.getFlowMod().getMatch();

        if(omatch.get(MatchField.IN_PORT) != null && tmatch.get(MatchField.IN_PORT) != null)
        {
            if(omatch.get(MatchField.IN_PORT).equals(tmatch.get(MatchField.IN_PORT)))
            {
                updateIntersect(intersect, MatchField.IN_PORT);
            }else{
                return DISJOINT;
            }
        }else{
            findRelation(tmatch, omatch, MatchField.IN_PORT, intersect);
        }

        if(omatch.get(MatchField.ETH_DST) != null && tmatch.get(MatchField.ETH_DST) != null)
        {
            if(omatch.get(MatchField.ETH_DST).equals(tmatch.get(MatchField.ETH_DST)))
            {
                updateIntersect(intersect, MatchField.ETH_DST);
            }else{
                return DISJOINT;
            }
        }else {
            findRelation(tmatch, omatch, MatchField.ETH_DST, intersect);
        }

        if(omatch.get(MatchField.ETH_SRC) != null && tmatch.get(MatchField.ETH_SRC) != null)
        {
            if(omatch.get(MatchField.ETH_SRC).equals(tmatch.get(MatchField.ETH_SRC)))
            {
                updateIntersect(intersect, MatchField.ETH_SRC);
            }else{
                return DISJOINT;
            }
        }else {
            findRelation(tmatch, omatch, MatchField.ETH_SRC, intersect);
        }

        if(omatch.get(MatchField.ETH_TYPE) != null && tmatch.get(MatchField.ETH_TYPE) != null)
        {
            if(omatch.get(MatchField.ETH_TYPE).equals(tmatch.get(MatchField.ETH_TYPE)))
            {
                updateIntersect(intersect, MatchField.ETH_TYPE);
            }else{
                return DISJOINT;
            }
        }else{
            findRelation(tmatch, omatch, MatchField.ETH_TYPE, intersect);
        }

        if(omatch.get(MatchField.VLAN_VID) != null && tmatch.get(MatchField.VLAN_VID) != null)
        {
            if(omatch.get(MatchField.VLAN_VID).equals(tmatch.get(MatchField.VLAN_VID)))
            {
                updateIntersect(intersect, MatchField.VLAN_VID);
            }else{
                return DISJOINT;
            }
        }else{
            findRelation(tmatch, omatch, MatchField.VLAN_VID, intersect);
        }

        if(omatch.get(MatchField.VLAN_PCP) != null && tmatch.get(MatchField.VLAN_PCP) != null)
        {
            if(omatch.get(MatchField.VLAN_PCP).equals(tmatch.get(MatchField.VLAN_PCP)))
            {
                updateIntersect(intersect, MatchField.VLAN_PCP);
            }else{
                return DISJOINT;
            }
        }else{
            findRelation(tmatch, omatch, MatchField.VLAN_PCP, intersect);
        }

        if(omatch.get(MatchField.IP_PROTO) != null && tmatch.get(MatchField.IP_PROTO) != null)
        {
            if(omatch.get(MatchField.IP_PROTO).equals(tmatch.get(MatchField.IP_PROTO)))
            {
                updateIntersect(intersect, MatchField.IP_PROTO);
            }else{
                return DISJOINT;
            }
        }else{
            findRelation(tmatch, omatch, MatchField.IP_PROTO, intersect);
        }

        if(omatch.get(MatchField.IP_DSCP) != null && tmatch.get(MatchField.IP_DSCP) != null)
        {
            if(omatch.get(MatchField.IP_DSCP).equals(tmatch.get(MatchField.IP_DSCP)))
            {
                updateIntersect(intersect, MatchField.IP_DSCP);
            }else{
                return DISJOINT;
            }
        }else{
            findRelation(tmatch, omatch, MatchField.IP_DSCP, intersect);
        }

        if(omatch.get(MatchField.IPV4_DST) != null && tmatch.get(MatchField.IPV4_DST) != null)
        {
            if(omatch.get(MatchField.IPV4_DST).equals(tmatch.get(MatchField.IPV4_DST)))
            {
                updateIntersect(intersect, MatchField.IPV4_DST);
            }else{
                return DISJOINT;
            }
        }else {
            findRelation(tmatch, omatch, MatchField.IPV4_DST, intersect);
        }

        if(omatch.get(MatchField.IPV4_SRC) != null && tmatch.get(MatchField.IPV4_SRC) != null)
        {
            if(omatch.get(MatchField.IPV4_SRC).equals(tmatch.get(MatchField.IPV4_SRC)))
            {
                updateIntersect(intersect, MatchField.IPV4_SRC);
            }else{
                return DISJOINT;
            }
        }else {
            findRelation(tmatch, omatch, MatchField.IPV4_SRC, intersect);
        }

        if(omatch.get(MatchField.TCP_DST) != null && tmatch.get(MatchField.TCP_DST) != null)
        {
            if(omatch.get(MatchField.TCP_DST).equals(tmatch.get(MatchField.TCP_DST)))
            {
                updateIntersect(intersect, MatchField.TCP_DST);
            }else{
                return DISJOINT;
            }
        }else {
            findRelation(tmatch, omatch, MatchField.TCP_DST, intersect);
        }

        if(omatch.get(MatchField.TCP_SRC) != null && tmatch.get(MatchField.TCP_SRC) != null)
        {
            if(omatch.get(MatchField.TCP_SRC).equals(tmatch.get(MatchField.TCP_SRC)))
            {
                updateIntersect(intersect, MatchField.TCP_SRC);
            }else{
                return DISJOINT;
            }
        }else {
            findRelation(tmatch, omatch, MatchField.TCP_SRC, intersect);
        }


        if(tmatch.equals(omatch)) {
            return EQUAL;
        }else if(intersect[SUBSET].size() > intersect[SUPERSET].size()) {
            if(strict) {
                return SUBSET;
            }else {
                return EQUAL;
            }
        }else{
            return SUPERSET;
        }
    }

    private void updateIntersect(Set<MatchField>[] intersect, MatchField field) {
        intersect[EQUAL].add(field);
        intersect[SUPERSET].add(field);
        intersect[SUBSET].add(field);
    }

    private void findRelation(Match tmatch, Match omatch, MatchField field, Set<MatchField>[] intersect) {
        if(tmatch.get(field) != null)
        {
            intersect[SUBSET].add(field);
        }

        if(omatch.get(field) != null)
        {
            intersect[SUPERSET].add(field);
        }
    }

    /** @return original OFMatch */
    public Match getMatch() {
        return this.ovxFlowMod.getFlowMod().getMatch();
    }

    /** @return the virtual output port */
    public short getOutport() {
        return this.ovxFlowMod.getFlowMod().getOutPort().getShortPortNumber();
    }

    public short getPriority() {
        return (short)this.ovxFlowMod.getFlowMod().getPriority();
    }

    public OVXFlowMod getOVXFlowMod() {
        return this.ovxFlowMod;
    }

    public OVXFlowEntry setOVXFlowMod(OVXFlowMod fm) {
        this.ovxFlowMod = fm;
        return this;
    }

    /**
     * @return The new (Physical) cookie
     */
    public long getNewCookie() {
        return this.newcookie;
    }

    /**
     * Sets the new cookie for this entry.
     *
     * @param cookie the cookie
     */
    public OVXFlowEntry setNewCookie(Long cookie) {
        this.newcookie = cookie;
        return this;
    }

    /**
     * Gets the cookie associated with this flow entry.
     *
     * @return The original (virtual) cookie
     */
    public long getCookie() {
        return this.ovxFlowMod.getFlowMod().getCookie().getValue();
    }

    public List<OFAction> getActionsList() {
        return this.ovxFlowMod.getFlowMod().getActions();
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
        result = prime * this.ovxFlowMod.hashCode();
        result = prime * result + (int) (newcookie ^ (newcookie >>> 32));
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final OVXFlowEntry other = (OVXFlowEntry) obj;
        if (this.newcookie != other.newcookie) {
            return false;
        }
        if (this.ovxFlowMod == null) {
            if (other.ovxFlowMod != null) {
                return false;
            }
        } else if (!this.ovxFlowMod.getOFMessage().equals(other.ovxFlowMod.getOFMessage())) {
            return false;
        }
        return true;
    }

    /**
     * compare this FlowEntry to another FlowMod.
     *
     * @param other
     * @return
     */
    public boolean equals(final OVXFlowMod other) {
        return (this.ovxFlowMod.equals(other));
    }

    @Override
    public int compareTo(final OVXFlowEntry other) {
        // sort on priority, tie break on IDs
        if (this.getOVXFlowMod().getFlowMod().getPriority() != other.getOVXFlowMod().getFlowMod().getPriority()) {
            return other.getOVXFlowMod().getFlowMod().getPriority() - this.getOVXFlowMod().getFlowMod().getPriority();
        }
        return this.hashCode() - other.hashCode();
    }

    /*
    public Map<String, Object> toMap() {
        final HashMap<String, Object> map = new LinkedHashMap<String, Object>();
        if (this.ovxFlowMod.getMatch() != null) {
            map.put("match", ((OVXMatch) this.ovxFlowMod.getMatch()).toMap());
        }
        map.put("actionsList", this.ovxFlowMod.getActions());
        map.put("priority", String.valueOf(this.ovxFlowMod.getPriority()));
        return map;
    }
    */

    @Override
    public String toString() {
        return "OVXFlowEntry [FlowMod=" + this.ovxFlowMod + "\n" + "newcookie="
                + this.newcookie + "]";
    }
}
