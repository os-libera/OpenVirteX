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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

/**
 * Abstract class to handle JSON requests.
 */
public abstract class AbstractService {

    private static Logger log = LogManager.getLogger(AbstractService.class
            .getName());

    /**
     * Handles the service request and stores the result in the response.
     *
     * @param request the request
     * @param response the response
     */
    public abstract void handle(HttpServletRequest request,
            HttpServletResponse response);

    /**
     * Parses the JSON request.
     *
     * @param request
     *            the request
     * @return the JSON object
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws JSONRPC2ParseException if JSON request is invalid
     */
    protected JSONRPC2Request parseJSONRequest(final HttpServletRequest request)
            throws IOException, JSONRPC2ParseException {
        final BufferedReader reader = request.getReader();
        final StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            sb.append(line + "\n");
            line = reader.readLine();
        }
        reader.close();
        AbstractService.log.debug("---------JSON RPC request: {}",
                sb.toString());
        return JSONRPC2Request.parse(sb.toString());

    }

    /**
     * Writes json object.
     *
     * @param response
     *            the response
     * @param jresp the JSON response
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    protected void writeJSONObject(final HttpServletResponse response,
            final JSONRPC2Response jresp) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Type", "application/json; charset=utf-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json; charset=utf-8");
        final String json = jresp.toJSONString();
        AbstractService.log.debug("---------JSON RPC response: {}", json);

        response.getWriter().println(json);
    }

    /**
     * Gets the exception stack trace in a string.
     *
     * @param e the exception
     * @return a string
     */
    protected static String stack2string(final Exception e) {
        PrintWriter pw = null;
        try {
            final StringWriter sw = new StringWriter();
            pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return "------\r\n" + sw.toString() + "------\r\n";
        } catch (final Exception e2) {
            return "bad stack2string";
        } finally {
            if (pw != null) {
                try {
                    pw.close();
                } catch (final Exception ex) {
                    return "cannot convert exception: " + ex.toString();
                }
            }
        }
    }
}
