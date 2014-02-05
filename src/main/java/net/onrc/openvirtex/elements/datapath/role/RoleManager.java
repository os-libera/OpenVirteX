package net.onrc.openvirtex.elements.datapath.role;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;





import java.util.concurrent.atomic.AtomicReference;

import net.onrc.openvirtex.exceptions.UnknownRoleException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFMessage;
import org.openflow.vendor.nicira.OFRoleVendorData;

public class RoleManager {

	
	private static Logger log = LogManager.getLogger(RoleManager.class
			.getName());

	
	private  HashMap<Channel, Role> state;
	
	private final AtomicReference<HashMap<Channel, Role>> currentState;
	
	private Channel currentMaster;
	
	public static enum Role {
        EQUAL(OFRoleVendorData.NX_ROLE_OTHER),
        MASTER(OFRoleVendorData.NX_ROLE_MASTER),
        SLAVE(OFRoleVendorData.NX_ROLE_SLAVE);

        private final int nxRole;

        private Role(int nxRole) {
            this.nxRole = nxRole;
        }

        private static Map<Integer,Role> nxRoleToEnum
                = new HashMap<Integer,Role>();
        static {
            for(Role r: Role.values())
                nxRoleToEnum.put(r.toNxRole(), r);
        }
        public int toNxRole() {
            return nxRole;
        }

        
        // Return the enum representing the given nxRole or null if no
        // such role exists
        public static Role fromNxRole(int nxRole) {
            return nxRoleToEnum.get(nxRole);
        }
        
       
    };
    
    public RoleManager() {
    	this.state = new HashMap<Channel, Role>();
    	this.currentState = new AtomicReference<HashMap<Channel,Role>>(state);
    }
    
    private HashMap<Channel, Role> getState() {
    	return new HashMap<>(this.currentState.get());
    }
    
    private void setState() {
    	this.currentState.set(this.state);
    }
    
    public void addController(Channel chan) {
    	this.state = getState();
    	this.state.put(chan, Role.EQUAL);
    	setState();
    }
    
    public void setRole(Channel channel, Role role) throws IllegalArgumentException, UnknownRoleException {
    	if (!this.currentState.get().containsKey(channel))
    		throw new IllegalArgumentException("Unknown controller " + channel.getRemoteAddress());
    	this.state = getState();
    	log.info("Setting controller {} to role {}", channel.getRemoteAddress(), role);
    	switch (role) {
    		case MASTER:
    			if (channel == currentMaster) {
    				this.state.put(channel, Role.MASTER);
    				break;
    			}
    			this.state.put(currentMaster, Role.SLAVE);
    			this.state.put(channel, Role.MASTER);
    			this.currentMaster = channel;
    			break;
    		case SLAVE:
    			if (channel == currentMaster) {
    				this.state.put(currentMaster, Role.SLAVE);
    				currentMaster = null;
    				break;
    			}
    			this.state.put(channel, Role.SLAVE);
    			break;
    		case EQUAL:
    			if (channel == currentMaster) {
    				this.state.put(currentMaster, Role.EQUAL);
    				this.currentMaster = null;
    				break;
    			}
    			this.state.put(channel, Role.EQUAL);
    			break;
    		default:
    			throw new UnknownRoleException("Unkown role : " + role);
    				
    	}
    	setState();
    	
    }
    
    public boolean canSend(Channel channel, OFMessage m) {
    	Role r = this.currentState.get().get(channel);
    	if (r == Role.MASTER || r == Role.EQUAL)
    		return true;
    	switch (m.getType()) {
    		case GET_CONFIG_REQUEST:
    		case QUEUE_GET_CONFIG_REQUEST:
    		case PORT_STATUS:
    		case STATS_REQUEST:
    			return true;
    		default:
    			return false;
    	}
    }
    
    public boolean canReceive(Channel channel, OFMessage m) {
    	Role r = this.currentState.get().get(channel);
    	if (r == Role.MASTER || r == Role.EQUAL)
    		return true;
    	switch (m.getType()) {
    		case GET_CONFIG_REPLY:
    		case QUEUE_GET_CONFIG_REPLY:
    		case PORT_STATUS:
    		case STATS_REPLY:
    			return true;
    		default:
    			return false;
    	}
    }

	public Role getRole(Channel channel) {
		this.state = getState();
		return this.state.get(channel);
	}
	
	private void checkAndSend(Channel c, OFMessage m) {
		if (canReceive(c, m)) {
			if (c != null && c.isOpen())
				c.write(Collections.singletonList(m));
		}
	}

	public void sendMsg(OFMessage msg, Channel c) {
		if (c != null) {
			checkAndSend(c, msg);
		} else {
			final Map<Channel, Role> readOnly = Collections.unmodifiableMap(this.currentState.get());
			for (Channel chan : readOnly.keySet()) {
				checkAndSend(chan, msg);
			}
		}
		
		
	}

	public void removeChannel(Channel channel) {
		this.state = getState();
		this.state.remove(channel);
		setState();
	}

	public void shutDown() {
		this.state = getState();
		for (Channel c : state.keySet())
			if (c != null && c.isConnected())
				c.close();
		state.clear();
		setState();
	}
    
}
