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
package net.onrc.openvirtex.api.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;

public class TenantService extends AbstractService {

    private static Logger log = LogManager.getLogger(TenantService.class
            .getName());

    Dispatcher dispatcher = new Dispatcher();

    public TenantService() {
        this.dispatcher.register(new TenantHandler());
    }

    @Override
    public void handle(final HttpServletRequest request,
            final HttpServletResponse response) {
        JSONRPC2Request json = null;
        JSONRPC2Response jsonResp = null;
        try {
            json = this.parseJSONRequest(request);
            jsonResp = this.dispatcher.process(json, null);
            jsonResp.setID(json.getID());
        } catch (final IOException e) {
            jsonResp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.PARSE_ERROR.getCode(),
                    AbstractService.stack2string(e)), 0);
        } catch (final JSONRPC2ParseException e) {
            jsonResp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.PARSE_ERROR.getCode(),
                    AbstractService.stack2string(e)), 0);
        }
        try {
            this.writeJSONObject(response, jsonResp);
        } catch (final IOException e) {
            TenantService.log.fatal("Unable to send response: {} ",
                    AbstractService.stack2string(e));
        }

    }

}
