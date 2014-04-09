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
package net.onrc.openvirtex.elements.host;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFPhysicalPort.OFPortState;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.db.DBManager;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.Persistable;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.util.MACAddress;

/**
 * Class representing a network host. This is a purely "Virtual" 
 * construct. 
 */
public class Host implements Persistable {
	
	/**
	 * The Host's FSM. Like Link states, it is affected by a 
	 * Port's status. 
	 */
	enum HostState {
		INIT {
			protected void register(Host h) {
				log.info("registering {}", h);
				try {
					Mappable map = OVXMap.getInstance();
					map.addMAC(h.mac, h.port.getTenantId());
					map.getVirtualNetwork(h.port.getTenantId()).addHost(h);
					DBManager.getInstance().save(h);
					h.state = HostState.INACTIVE;
				} catch (NetworkMappingException e) {
					log.warn("tenant {} was not found in global map",
							h.port.getTenantId());
				}
			}
		},
		INACTIVE {
			protected boolean boot(Host h) {
				log.info("booting {}", h);
				/* port to host goes up GIVEN port is ACTIVE. Host can attach
				 * to an inactive port. */
				h.state = HostState.ACTIVE;
				h.port.setState(h.port.getState() & ~OFPortState.OFPPS_LINK_DOWN.getValue());
				h.port.setEdge(true);
				return true;
			}
			
			protected void unregister(Host h) {
				log.info("un-registering {}", h);
				try {
					Mappable map = OVXMap.getInstance();
					map.removeMAC(h.mac);
					map.getVirtualNetwork(h.port.getTenantId()).removeHost(h);
					DBManager.getInstance().remove(h);
					h.state = HostState.STOPPED;	
				} catch (NetworkMappingException e) {
					log.warn("tenant {} was not found in global map", 
							h.port.getTenantId());
				}
			}
		},
		ACTIVE {
			protected boolean teardown(Host h) {
				log.info("inactivating {}", h);
				/* port to host goes down GIVEN port is ACTIVE.*/
				h.state = HostState.INACTIVE;
				h.port.setState(h.port.getState() | OFPortState.OFPPS_LINK_DOWN.getValue());
				return true;
			}
		},
		STOPPED;
		
		protected void register(Host h) {	
			log.warn("Cannot register {} while status={}", 
					h, h.state);
		}
		
		protected boolean boot(Host h) {
			log.warn("Cannot boot {} while status={}", 
					h, h.state);
			return false;
		}
		
		protected boolean teardown(Host h) {
			log.warn("Cannot teardown {} while status={}", 
					h, h.state);
			return false;
		}
		
		protected void unregister(Host h) {
			log.warn("Cannot unregister {} while status={}", 
					h, h.state);
		}
		
	}
	
	static Logger log = LogManager.getLogger(Host.class.getName());
	
	public static final String DB_KEY = "hosts"; 

	private final Integer hostId;
	private final MACAddress mac;
	private final OVXPort port;
	
	private OVXIPAddress ipAddress = new OVXIPAddress(0, 0);
	private HostState state;
	
	public Host(final MACAddress mac, final OVXPort port, final Integer hostId) {
		this.mac = mac;
		this.port = port;
		this.hostId = hostId;
		this.state = HostState.INIT;
	}

	public void setIPAddress(int ip) {
		this.ipAddress  = new OVXIPAddress(this.port.getTenantId(), ip);
	}
	
	public OVXIPAddress getIp() {
		return this.ipAddress;
	}
	
	public MACAddress getMac() {
		return mac;
	}

	public OVXPort getPort() {
		return port;
	}

	public void register() {
		this.state.register(this);
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
		dbObject.put(TenantHandler.VDPID, this.port.getParentSwitch().getSwitchId());
		dbObject.put(TenantHandler.VPORT, this.port.getPortNumber());
		dbObject.put(TenantHandler.MAC, this.mac.toLong());
		dbObject.put(TenantHandler.HOST, this.hostId);
		return dbObject;
	}

	public Integer getHostId() {
		return hostId;
	}

	public void unregister() {
		this.state.unregister(this);
	}
	
	public void tearDown() {
		this.state.teardown(this);
	}

	public boolean boot() {
		return this.state.boot(this);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mac == null) ? 0 : mac.hashCode());
		result = prime * result + ((port == null) ? 0 : port.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Host other = (Host) obj;
		if (mac == null) {
			if (other.mac != null)
				return false;
		} else if (!mac.equals(other.mac))
			return false;
		if (port == null) {
			if (other.port != null)
				return false;
		} else if (!port.equals(other.port))
			return false;
		return true;
	}

	
	/*
	 * Super ugly method to convert virtual elements of 
	 * a host to physical data
	 */
	public HashMap<String, Object> convertToPhysical() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("hostId", this.hostId);
		map.put("dpid", this.port.getPhysicalPort().getParentSwitch().getSwitchName());
		map.put("port", port.getPhysicalPortNumber());
		map.put("mac", this.mac.toString());
		
		if (this.ipAddress.getIp() != 0)
			try {
				map.put("ipAddress", OVXMap.getInstance().getPhysicalIP(this.ipAddress, this.port.getTenantId()).toSimpleString());
			} catch (AddressMappingException e) {
				log.warn("Unable to fetch physical IP for host");
			}
		return map;
	}
	
	@Override
	public String toString() {
		return "Host [HWAddr:" + this.mac.toString() +
				" Port:" + this.port.toAP() +"]";
	}
	
}
