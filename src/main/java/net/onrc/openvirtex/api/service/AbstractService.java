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

public abstract class AbstractService {
	
	private static Logger log = LogManager.getLogger(AbstractService.class.getName());
	
	
	public abstract void handle(HttpServletRequest request,HttpServletResponse response);
	
	/**
	 * Parses the json.
	 *
	 * @param request the request
	 * @return the jSON object
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPC2ParseException 
	 * @throws JSONException the jSON exception
	 */
	protected JSONRPC2Request parseJSONRequest(HttpServletRequest request)
			throws IOException, JSONRPC2ParseException{
		BufferedReader reader = request.getReader();
	    StringBuilder sb = new StringBuilder();
	    String line = reader.readLine();
	    while (line != null) {
	        sb.append(line + "\n");
	        line = reader.readLine();
	    }
	    reader.close();
	    log.debug("---------JSON RPC request: {}", sb.toString());
	    return JSONRPC2Request.parse(sb.toString());

	}

	
	/**
	 * Write json object.
	 *
	 * @param response the response
	 * @param jobj the jobj
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected void writeJSONObject(HttpServletResponse response, JSONRPC2Response jresp)
			throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setHeader("Content-Type", "application/json; charset=utf-8");
		response.setContentType("application/json; charset=utf-8");
		String json = jresp.toJSONString();
		log.debug("---------JSON RPC response: {}", json);
		
		response.getWriter().println(json);
	}


	protected static String stack2string(Exception e) {
		PrintWriter pw = null;
		try {
			StringWriter sw = new StringWriter();
			pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			return "------\r\n" + sw.toString() + "------\r\n";
		} catch (Exception e2) {
			return "bad stack2string";
		} finally {
			if (pw != null) {
				try {
					pw.close();
				} catch (Exception ex) {
				}
			}
		}
	}
}
