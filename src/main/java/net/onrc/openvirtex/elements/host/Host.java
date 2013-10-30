/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.elements.host;

import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.Persistable;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.util.MACAddress;

public class Host implements Persistable {
	public static final String DB_KEY = "hosts"; 

	private final Integer hostId;
	private final MACAddress mac;
	private final OVXPort port;

	public Host(final MACAddress mac, final OVXPort port, final Integer hostId) {
		this.mac = mac;
		this.port = port;
		this.hostId = hostId;
	}

	public MACAddress getMac() {
		return mac;
	}

	public OVXPort getPort() {
		return port;
	}

	public void register() {
		DBManager.getInstance().save(this);
	}

	@Override
	public Map<String, Object> getDBIndex() {
		Map<String, Object> index = new HashMap<String, Object>();
		index.put(TenantHandler.TENANT, this.port.getTenantId());
		return index;
	}

	@Override
	public String getDBKey() {
		return Host.DB_KEY;
	}

	@Override
	public String getDBName() {
		return DBManager.DB_VNET;
	}

	@Override
	public Map<String, Object> getDBObject() {
		Map<String, Object> dbObject = new HashMap<String, Object>();
		dbObject.put(TenantHandler.DPID, this.port.getParentSwitch().getSwitchId());
		dbObject.put(TenantHandler.PORT, this.port.getPortNumber());
		dbObject.put(TenantHandler.MAC, this.mac.toLong());
		dbObject.put(TenantHandler.HOST, this.hostId);
		return dbObject;
	}

	public Integer getHostId() {
		return hostId;
	}

	public void unregister() {
		try {
			this.tearDown();
			Mappable map = this.port.getParentSwitch().getMap();
			map.removeMAC(this.mac);
			map.getVirtualNetwork(port.getTenantId()).removeHost(this);
		} catch (NetworkMappingException e) {
			//log object?
		}

	}
	
	public void tearDown() {
		this.port.tearDown();	
	}
	
}
