package net.onrc.openvirtex.elements;

/**
 * Methods implemented by components that represent network elements in OVX. 
 * Each component is assumed to implement a state machine with the 
 * following states:
 * <ul>
 * <li> INIT - just initialized, not accessible or usable</li>
 * <li> ACTIVE - can be accessed and can handle events, 
 * accumulate state (flow entries, counters), etc.</li>
 * <li> INACTIVE - can be accessed, but won't handle events
 * or accumulate state (e.g. like a downed interface) </li>
 * <li> STOPPED - element removed from network representation</li>
 * </ul>
 * Components can also have subcomponents whose states may depend on 
 * its own state. In general, a subcomponent's state won't affect 
 * the component, but the reverse is not true e.g. ports are 
 * removed if a switch is removed, but a switch port can be removed 
 * without affecting the whole switch.  
 */
public interface Component {
	
	/**
	 * Adds this component to mappings, storage, and makes it accessible to OVX
	 * as subscribers to components that this one depends on (sets state to INACTIVE
	 * from INIT) 
	 */
	public void register();
	
	/**
	 * Initializes component's services and sets its state up to be activated.
	 * 
	 * @return true if successfully initialized (sets state to ACTIVE)
	 */
	public boolean boot();
	
	/**
	 * Removes this component and its subcomponents from global mapping and unsubsribes
	 * this component from others. (sets state to STOPPED). If this component is a 
	 * Physical entity, it will also attempt to unregister() Virtual components mapped to 
	 * it.
	 */
	public void unregister();
	
	/**
	 * Halts event processing at this component (sets state to INACTIVE) and its 
	 * subcomponents. If this component is a 
	 * Physical entity, it will also attempt to unregister() Virtual components mapped to 
	 * it. The order in which components are unregistered is described by a DAG. If this
	 * component is designated as a root (isRoot=true), the DAG will be reduced to a 
	 * spanning tree rooted at this component, so we guarantee unregister() is called for 
	 * all dependent components once. 
	 * 
	 * @return true if component is in-activated 
	 */
	public boolean tearDown();
}
