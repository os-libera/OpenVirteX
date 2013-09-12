package net.onrc.openvirtex.api.service.handlers;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public abstract class ApiHandler<T> {

		
	public abstract JSONRPC2Response process(T params);

	public abstract JSONRPC2ParamsType getType();

	public String cmdName() {
		return this.getClass().getSimpleName();
	}

}
