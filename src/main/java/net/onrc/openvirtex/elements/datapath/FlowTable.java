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

import net.onrc.openvirtex.exceptions.MappingException;
import net.onrc.openvirtex.messages.OVXFlowMod;

/**
 * Base interface for the flow table.
 */
public interface FlowTable {

    /**
     * Handles FlowMods.
     *
     * @param fm The FlowMod to process.
     * @return true if processing occurred correctly
     */
    public boolean handleFlowMods(OVXFlowMod fm);

    /**
     * Fetches FlowMod out of this Flow Table based on cookie.
     *
     * @param cookie the cookie
     * @return the FlowMod
     * @throws MappingException
     */
    public OVXFlowMod getFlowMod(Long cookie) throws MappingException;

    /**
     * Checks if a FlowMod with given cookie exists in the FlowTable.
     *
     * @param cookie the cookie
     * @return true if FlowMod exists
     */
    public boolean hasFlowMod(long cookie);

    /**
     * Remove a FlowMod from this table. Assumes a caller who uses this method
     * directly knows what they are doing, since they are modifying the
     * FlowTable by hand.
     *
     * @param cookie
     *            the cookie value of the FlowMod to delete.
     * @return The FlowMod that was removed.
     */
    public OVXFlowMod deleteFlowMod(final Long cookie);

    /**
     * Add a FlowMod to this table with given cookie.
     * The caller is assumed to know what they are
     * doing, since they will modify the FlowTable directly.
     *
     * @param flowmod the flow mod
     * @param cookie the cookie
     * @return the cookie value
     */
    public long addFlowMod(final OVXFlowMod flowmod, long cookie);

    /**
     * @return The contents of this flow table.
     */
    public abstract Collection<OVXFlowMod> getFlowTable();

}
