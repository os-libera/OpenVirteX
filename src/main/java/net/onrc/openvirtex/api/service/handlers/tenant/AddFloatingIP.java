package net.onrc.openvirtex.api.service.handlers.tenant;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.FloatingIPException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.routing.nat.NatIpManager;

/**
 *
 * Add floating IP to a PhysicalPort
 * ovxctl.py <PhysicalSwitch> <PhysicalPort> publicIP1 publicIP2 ...
 *
 */
public class AddFloatingIP  extends ApiHandler<Map<String, Object>> {

    private Logger log = LogManager.getLogger(AddFloatingIP.class.getName());

    @Override
    public JSONRPC2Response process(Map<String, Object> params) {
        JSONRPC2Response resp = null;
        try {

            final Number dpid = HandlerUtils.<Number>fetchField(TenantHandler.DPID, params, true, null);
            final Number port = HandlerUtils.<Number>fetchField(TenantHandler.PORT, params, true, null);
            final ArrayList<String> publicIPs = HandlerUtils.<ArrayList<String>>fetchField(TenantHandler.NETADD, params, true, null);

            final PhysicalSwitch physicalSwitch = PhysicalNetwork.getInstance().getSwitch(dpid.longValue());
            final PhysicalPort physicalPort = physicalSwitch.getPort(port.shortValue());

            for (String publicIPString : publicIPs) {
                NatIpManager.getInstance().addPublicIP(publicIPString, physicalPort);
            }

            ArrayList<InetAddress> publicInetAddresses = NatIpManager.getInstance().getPublicIP(physicalPort);

            if (publicInetAddresses == null || publicInetAddresses.size() == 0) {
                resp = new JSONRPC2Response(
                        new JSONRPC2Error(
                                JSONRPC2Error.INTERNAL_ERROR.getCode(),
                                this.cmdName()), 0);
            } else {
                this.log.info(
                        "Added PublicIPs {} to PhysicalPort {} on PhysicalSwitch {}",
                        publicInetAddresses, physicalPort, physicalSwitch);
                Map<String, Object> reply = new HashMap<String, Object>();
                reply.put(TenantHandler.NETADD, publicInetAddresses);
                resp = new JSONRPC2Response(reply, 0);
            }

        } catch (final MissingRequiredField e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Unable to add PublicIPs : "
                            + e.getMessage()), 0);
        } catch (final InvalidDPIDException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid DPID : " + e.getMessage()), 0);
        } catch (FloatingIPException e) {
            resp = new JSONRPC2Response(
                    new JSONRPC2Error(
                            JSONRPC2Error.INVALID_PARAMS.getCode(),
                            this.cmdName()
                                    + ": Impossible to add PublicIPs, "
                                    + e.getMessage()), 0);
        }
        return resp;
    }

    @Override
    public JSONRPC2ParamsType getType() {
        return JSONRPC2ParamsType.OBJECT;
    }

}
