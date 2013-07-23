/**
 * 
 */
package net.onrc.openvirtex.elements.datapath;

import java.util.HashMap;

import net.onrc.openvirtex.elements.OVXMap;

import org.openflow.protocol.OFFeaturesReply;

/**
 * The Class Switch.
 * 
 * @param <T1>
 *            the generic type
 * @author gerola
 */
public abstract class Switch<T1> {

    /** The switch name. */
    private String                     switchName;

    /** The port map. */
    protected final HashMap<Short, T1> portMap;

    /** The switch info. */
    private OFFeaturesReply            switchInfo;

    /** The switch id. */
    private long                       switchId;

    /** The map. */
    private final OVXMap               map;

    /**
     * Instantiates a new switch.
     */
    public Switch() {
	super();
	this.switchName = "";
	this.switchId = 0;
	this.map = null;
	this.portMap = new HashMap<Short, T1>();
	this.switchInfo = null;
    }

    /**
     * Instantiates a new switch.
     * 
     * @param switchName
     *            the switch name
     * @param switchId
     *            the switch id
     * @param map
     *            the map
     */

    public Switch(final String switchName, final long switchId, final OVXMap map) {
	super();
	this.switchName = switchName;
	this.switchId = switchId;
	this.map = map;
	this.portMap = new HashMap<Short, T1>();
	this.switchInfo = null;
    }

    /**
     * Gets the switch name.
     * 
     * @return the switch name
     */
    public String getSwitchName() {
	return this.switchName;
    }

    /**
     * Sets the switch name.
     * 
     * @param switchName
     *            the switch name
     * @return true, if successful
     */
    public boolean setSwitchName(final String switchName) {
	this.switchName = switchName;
	return true;
    }

    /**
     * Gets the switch info.
     * 
     * @return the switch info
     */
    public OFFeaturesReply getSwitchInfo() {
	return this.switchInfo;
    }

    /**
     * Sets the switch info.
     * 
     * @param switchInfo
     *            the switch info
     * @return true, if successful
     */
    public boolean setSwitchInfo(final OFFeaturesReply switchInfo) {
	this.switchInfo = switchInfo;
	return true;
    }

    /**
     * Gets the switch id.
     * 
     * @return the switch id
     */
    public long getSwitchId() {
	return this.switchId;
    }

    /**
     * Sets the switch id.
     * 
     * @param switchId
     *            the switch id
     * @return true, if successful
     */
    public boolean setSwitchId(final long switchId) {
	this.switchId = switchId;
	return true;
    }

    /**
     * Gets the port map.
     * 
     * @return the port map
     */
    public HashMap<Short, T1> getPortMap() {
	return new HashMap<Short, T1>(this.portMap);
    }

    /**
     * Gets the port.
     * 
     * @param portNumber
     *            the port number
     * @return the port
     */
    public abstract T1 getPort(short portNumber);

    /**
     * Adds the port.
     * 
     * @param port
     *            the port
     * @return true, if successful
     */
    public abstract boolean addPort(T1 port);

    /**
     * Update port.
     * 
     * @param port
     *            the port
     * @return true, if successful
     */
    public abstract boolean updatePort(T1 port);

    /**
     * Removes the port.
     * 
     * @param portNumber
     *            the port number
     * @return true, if successful
     */
    public abstract boolean removePort(short portNumber);

    /**
     * Initialize.
     * 
     * @return true, if successful
     */
    public abstract boolean initialize();

    /**
     * Send msg.
     * 
     * @return true, if successful
     */
    public abstract boolean sendMsg();
}
