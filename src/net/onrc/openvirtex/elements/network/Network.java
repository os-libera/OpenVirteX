/**
 * 
 */
package net.onrc.openvirtex.elements.network;

import java.util.HashMap;
import java.util.HashSet;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;

/**
 * @author gerola
 * 
 */
public abstract class Network<T1, T2, T3> implements Mappable {

    private HashSet<T1>             switchSet;
    private HashSet<T3>             linkSet;
    public HashMap<T2, T2>          neighbourPortMap;
    public OVXMap                   map;
    public HashMap<T1, HashSet<T1>> neighbourMap;

    // public OFControllerChannel channel;

    public void registerSwitch(final T1 sw) {
    }

    public void unregisterSwitch(final T1 sw) {
    }

    public void registerLink(final T3 link) {
    }

    public void unregisterLink(final T3 link) {
    }

    public boolean initialize() {
	return true;
    }

    public HashSet<T1> getNeighbours(final T1 sw) {
	return null;
    }

    public abstract void sendLLDP(T1 sw);

    public abstract void receiveLLDP(T1 sw);

}