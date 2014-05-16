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
package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;

import junit.framework.TestCase;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class AbstractAPICalls extends TestCase {

    public JSONRPC2Response createNetwork() {
        return this.createNetwork(6633);
    }

    public JSONRPC2Response createNetwork(final Integer port) {
        final CreateOVXNetwork cn = new CreateOVXNetwork();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                ArrayList<String> ctrls = new ArrayList<String>();
                ctrls.add("tcp:localhost:" + port);

                this.put(TenantHandler.CTRLURLS, ctrls);
                this.put(TenantHandler.NETADD, "10.0.0.0");
                this.put(TenantHandler.NETMASK, 24);
            }
        };

        return cn.process(request);

    }

    public JSONRPC2Response createSwitch(final Integer tenantId,
            final List<Integer> dpids) {
        OVXMap.getInstance();
        final CreateOVXSwitch cs = new CreateOVXSwitch();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.DPIDS, dpids);
            }
        };

        return cs.process(request);

    }

    public JSONRPC2Response createPort(final Integer tenantId, final Long dpid,
            final Short port) {

        final CreateOVXPort cp = new CreateOVXPort();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.DPID, dpid);
                this.put(TenantHandler.PORT, port);
            }
        };

        return cp.process(request);
    }

    public JSONRPC2Response setInternalRouting(final Integer tenantId,
            final Long dpid, final String protocol, final byte backups) {

        final SetOVXBigSwitchRouting sr = new SetOVXBigSwitchRouting();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.VDPID, dpid);
                this.put(TenantHandler.ALGORITHM, protocol);
                this.put(TenantHandler.BACKUPS, backups);
            }
        };

        return sr.process(request);
    }

    public JSONRPC2Response connectHost(final Integer tenantId,
            final Long dpid, final Short port, final String mac) {

        final ConnectHost ch = new ConnectHost();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.VDPID, dpid);
                this.put(TenantHandler.VPORT, port);
                this.put(TenantHandler.MAC, mac);
            }
        };

        return ch.process(request);
    }

    public JSONRPC2Response connectLink(final int tenantId, final long srcDpid,
            final short srcPort, final long dstDpid, final short dstPort,
            final String alg, final byte backups) {
        final ConnectOVXLink cl = new ConnectOVXLink();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.SRC_DPID, srcDpid);
                this.put(TenantHandler.SRC_PORT, srcPort);
                this.put(TenantHandler.DST_DPID, dstDpid);
                this.put(TenantHandler.DST_PORT, dstPort);
                this.put(TenantHandler.ALGORITHM, alg);
                this.put(TenantHandler.BACKUPS, backups);
            }
        };

        return cl.process(request);
    }

    public JSONRPC2Response setLinkPath(final int tenantId, final int linkId,
            final String pathString, final byte priority) {
        final SetOVXLinkPath sl = new SetOVXLinkPath();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.LINK, linkId);
                this.put(TenantHandler.PATH, pathString);
                this.put(TenantHandler.PRIORITY, priority);
            }
        };

        return sl.process(request);
    }

    public JSONRPC2Response connectRoute(final int tenantId, final long dpid,
            final short srcPort, final short dstPort, final String pathString,
            final byte priority) {
        final ConnectOVXRoute cr = new ConnectOVXRoute();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.VDPID, dpid);
                this.put(TenantHandler.SRC_PORT, srcPort);
                this.put(TenantHandler.DST_PORT, dstPort);
                this.put(TenantHandler.PATH, pathString);
                this.put(TenantHandler.PRIORITY, priority);
            }
        };

        return cr.process(request);
    }

    public JSONRPC2Response removeNetwork(final int tenantId) {
        final RemoveOVXNetwork rn = new RemoveOVXNetwork();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
            }
        };

        return rn.process(request);
    }

    public JSONRPC2Response removeSwitch(final int tenantId, final long dpid) {
        final RemoveOVXSwitch rs = new RemoveOVXSwitch();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.VDPID, dpid);
            }
        };

        return rs.process(request);
    }

    public JSONRPC2Response removePort(final int tenantId, final long dpid,
            final short port) {
        final RemoveOVXPort rp = new RemoveOVXPort();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.VDPID, dpid);
                this.put(TenantHandler.VPORT, port);
            }
        };

        return rp.process(request);
    }

    public JSONRPC2Response disconnectHost(final int tenantId, final int hostId) {
        final DisconnectHost dh = new DisconnectHost();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.HOST, hostId);
            }
        };

        return dh.process(request);
    }

    public JSONRPC2Response disconnectLink(final int tenantId, final int linkId) {
        final DisconnectOVXLink dl = new DisconnectOVXLink();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.LINK, linkId);
            }
        };

        return dl.process(request);
    }

    public JSONRPC2Response disconnectRoute(final int tenantId,
            final long dpid, final int routeId) {
        final DisconnectOVXRoute dr = new DisconnectOVXRoute();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.VDPID, dpid);
                this.put(TenantHandler.ROUTE, routeId);
            }
        };

        return dr.process(request);
    }

    public JSONRPC2Response startNetwork(final int tenantId) {
        final StartOVXNetwork sn = new StartOVXNetwork();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
            }
        };

        return sn.process(request);
    }

    public JSONRPC2Response startSwitch(final int tenantId, final long dpid) {
        final StartOVXSwitch ss = new StartOVXSwitch();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.VDPID, dpid);
            }
        };

        return ss.process(request);
    }

    public JSONRPC2Response startPort(final int tenantId, final long dpid,
            final short port) {
        final StartOVXPort sp = new StartOVXPort();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.VDPID, dpid);
                this.put(TenantHandler.VPORT, port);
            }
        };

        return sp.process(request);
    }

    public JSONRPC2Response stopNetwork(final int tenantId) {
        final StopOVXNetwork sn = new StopOVXNetwork();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
            }
        };

        return sn.process(request);
    }

    public JSONRPC2Response stopSwitch(final int tenantId, final long dpid) {
        final StopOVXSwitch ss = new StopOVXSwitch();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.VDPID, dpid);
            }
        };

        return ss.process(request);
    }

    public JSONRPC2Response stopPort(final int tenantId, final long dpid,
            final short port) {
        final StopOVXPort sp = new StopOVXPort();

        @SuppressWarnings("serial")
        final HashMap<String, Object> request = new HashMap<String, Object>() {
            {
                this.put(TenantHandler.TENANT, tenantId);
                this.put(TenantHandler.VDPID, dpid);
                this.put(TenantHandler.VPORT, port);
            }
        };

        return sp.process(request);
    }

    public void testPassing() {
        /* Make JUnit happy */
        /* http://junit.sourceforge.net/doc/faq/faq.htm#running_11 */
    }

}
