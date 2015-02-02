package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.host.NATGateway;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.DuplicateMACException;
import net.onrc.openvirtex.exceptions.FloatingIPException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.routing.nat.NatIpManager;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

/**
 *
 * ovxctl.py <TenantID> <OVXSwitch ID on the PhysicalSwitch with external connectivity> <OVXPort ID on the PhysicalPort with external connectivity> <Mac>
 *
 */
public class EnableNAT extends ApiHandler<Map<String, Object>> {

    Logger log = LogManager.getLogger(EnableNAT.class.getName());

    @Override
    public JSONRPC2Response process(Map<String, Object> params) {

        JSONRPC2Response resp = null;

        try {
            final Number tenantId = HandlerUtils.<Number>fetchField(
                    TenantHandler.TENANT, params, true, null);
            final Number dpid = HandlerUtils.<Number>fetchField(
                    TenantHandler.VDPID, params, true, null);
            final Number port = HandlerUtils.<Number>fetchField(
                    TenantHandler.VPORT, params, true, null);
            final String mac = HandlerUtils.<String>fetchField(
                    TenantHandler.MAC, params, true, null);

            HandlerUtils.isValidTenantId(tenantId.intValue());
            HandlerUtils.isValidOVXPort(tenantId.intValue(), dpid.longValue(),
                    port.shortValue());

            final OVXMap map = OVXMap.getInstance();
            final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId
                    .intValue());
            final MACAddress macAddr = MACAddress.valueOf(mac);
            HandlerUtils.isUniqueHostMAC(macAddr);

            final String gatewayIP = NatIpManager.getInstance().getDefaultGatewayVirtualIP(tenantId.intValue());
            boolean bidirectional = false;
            final NATGateway gateway = virtualNetwork.enableNAT(dpid.longValue(),
                                                                port.shortValue(),
                                                                gatewayIP,
                                                                bidirectional,
                                                                macAddr);

            if (gateway == null) {
                resp = new JSONRPC2Response(
                        new JSONRPC2Error(
                                JSONRPC2Error.INTERNAL_ERROR.getCode(),
                                this.cmdName()), 0);
            } else {
                this.log.info(
                        "Enabled NetworkGateway with id {} and mac {} to virtual port {} on virtual switch {} in virtual network {} NetworkGateway {}",
                        gateway.getHostId(), gateway.getMac().toString(), gateway
                                .getPort().getPortNumber(), gateway.getPort()
                                .getParentSwitch().getSwitchName(),
                                virtualNetwork.getTenantId(), gateway);
                Map<String, Object> reply = new HashMap<String, Object>(
                        gateway.getDBObject());
                reply.put(TenantHandler.TENANT, tenantId.intValue());
                resp = new JSONRPC2Response(reply, 0);
            }

        } catch (final MissingRequiredField e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Unable to enable NetworkGateway : " + e.getMessage()),
                    0);
        } catch (final InvalidPortException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid port : " + e.getMessage()), 0);
        } catch (final InvalidTenantIdException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid tenant id : " + e.getMessage()), 0);
        } catch (final IndexOutOfBoundException e) {
            resp = new JSONRPC2Response(
                    new JSONRPC2Error(
                            JSONRPC2Error.INVALID_PARAMS.getCode(),
                            this.cmdName()
                                    + ": Impossible to enable NetworkGateway, too many ports on this virtual switch : "
                                    + e.getMessage()), 0);
        } catch (final NetworkMappingException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": " + e.getMessage()), 0);
        } catch (final DuplicateMACException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": " + e.getMessage()), 0);
        } catch (FloatingIPException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": " + e.getMessage()), 0);
        }

        return resp;
    }

    @Override
    public JSONRPC2ParamsType getType() {
        return JSONRPC2ParamsType.OBJECT;
    }

}
