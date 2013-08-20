/**
 * 
 */
package net.onrc.openvirtex.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.*;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.api.APITenantManager;
/**
 * @author sumanth
 *
 */
	public class OVXConfig {
	    
	    public static String HOST = "localhost";
	    public static Integer OVXPORT = 6633;
	    public static Integer APIPORT = 8080;
	    
	    public static String VNET = "virtualnetwork";
	    public static String NODE = "node";
	    public static String EDGE = "edge";
	    public static String SWID = "switch-id";
	    public static String PORTNUM = "port-number";
	    public static String SRC = "src";
	    public static String DST = "dst";
	    public static String OVX = "openvirtex";
	    public static String TID = "tenant-id";
	    public static String LINKID = "link-id";
	    public static String MAP = "mapping";
	    public static String LINKMAP = "link-map";
	    public static String PHYLINK = "physical-link";
	    public static String PORT = "port";
	    public static String DPID = "dpid";
	    public static String SWMAP = "switch-map";
	    public static String VSWID = "virtual-switch-id";
	    public static String PSWID = "pswitch-id";
	    public static String NET = "network";
	    public static String CON_ADDR = "controler-address";
	    public static String CON_PORT = "controller-port";
	    public static String PROTOCOL = "tcp";
	    
	    public static String COMMA = ",";
	    public static String HYPHEN = "-";
	    public static String FWDSLASH = "/";
	    
	    private static HashMap<String, ArrayList<HashMap<String, Object>>> config;
	    private static Integer tenant;
	    private static String network;
	    private static String networkAddress;
	    private static short mask;
	    private static HashMap<String,String> gateway;
	    private static ArrayList<String> dpids;
	    private static String controllerAddress;
	    private static Integer controllerPort;
	    private static String dpid;
	    private static Integer linkId;
	    
	    
	    private static APITenantManager manager = new APITenantManager();
	    
	    /**
	     * Makes a serialized json string by obtaining the instantiated values 
	     * of all the elements.
	     * Uses gson for serialization purposes.
	     * 
	     * @param None
	     * @return A serialized structure to the new json file
	     * 
	     */
	    public static synchronized String saveConfig(){
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		//HashMap - output which would be finally serialized using gson.
		HashMap<String,Object> output = new HashMap<String,Object>();
		
		//output gets the instantiated values from OpenVirteXController
		OpenVirteXController ovx = OpenVirteXController.getInstance();
		output.putAll(ovx.toJson());
		
		//output gets the instantiated values from elements.network
		OVXMap map = OVXMap.getInstance();
		Collection<OVXNetwork> networks = map.getVirtualNetworks();
		LinkedList<HashMap<String,Object>> netList = new LinkedList<HashMap<String,Object>>();
		for(OVXNetwork network:networks){
		    netList.add(network.toJson());
		}
		output.put(VNET, netList);
		
		//output gets the instantiated values from elements.node
		Collection<OVXSwitch> nodes = map.getVirtualSwitches();
		LinkedList<HashMap<String,Object>> nodeList = new LinkedList<HashMap<String,Object>>();
		for(OVXSwitch node:nodes){
		    nodeList.add(node.toJson());
		}
		output.put(NODE, nodeList);
		
		//output gets the instantiated values from elements.map
		Collection<OVXLink> links = map.getVirtualLinks();
		LinkedList<HashMap<String,Object>> linkList = new LinkedList<HashMap<String,Object>>();
		for(OVXLink link:links){
		    linkList.add(link.toJson());
		}
		output.put(EDGE, linkList);
		
		output.putAll(map.toJson());
		
		return gson.toJson(output);
	    }
	    
	    /**
	     * loads the json file and instantiates all the elements.
	     * uses gson to deserialize.
	     * 
	     * @param fileName
	     * @return None
	     * 
	     */
	    public static synchronized void loadConfig(String fileName)
	    	throws FileNotFoundException{
		try{
		    Gson gson = new GsonBuilder().create();
		    File file = new File(fileName);
		    String json = new Scanner(file).useDelimiter("\\Z").next();
		    config 
		    	= gson.fromJson(json, new TypeToken<HashMap<String,Object>>(){}.getType());
		    
		    OpenVirteXController ovx = new OpenVirteXController();
		    if(config.containsKey(OVX)){
			ovx.fromJson(config.get(OVX));
		    }
		    if(config.containsKey(VNET)){
			virtualNetworkFromJson(config.get(VNET));
		    }
		    if(config.containsKey(NODE)){
			virtualSwitchFromJson(config.get(NODE));
		    }
		    if(config.containsKey(EDGE)){
			virtualEdgeFromJson(config.get(EDGE));
		    }
		}
		catch(IOException ioe){
		    System.err.println("Error: "+ioe.getStackTrace());
		}
	    }

	    
	/**
	 * Takes a json deserialized ArrayList and instantiates the link element.
	 * 
	 * @param vLinks: ArrayList of virtual links
	 */    
	private static void virtualEdgeFromJson(
		ArrayList<HashMap<String, Object>> vLinks) {
	    	for(HashMap<String,Object> row: vLinks){
	            tenant = (Integer) row.get(TID);
	            linkId = (Integer) row.get(LINKID);
	            ArrayList<HashMap<String,Object>> linkMap = new ArrayList<HashMap<String,Object>>();
	            ArrayList<HashMap<String,Object>> map = config.get(MAP);
	            for (HashMap<String,Object> tenantMap:map){
	        	if(tenantMap.get(TID)==tenant){
	        	    linkMap = (ArrayList<HashMap<String,Object>>) tenantMap.get(LINKMAP);
	        	}
	            }
	            ArrayList<HashMap<String,Object>> phyLinks = getPhysicalLinks(linkId,linkMap); 
	            String phyPath = getPhysicalPath(phyLinks);
	            manager.createOVXLink(tenant,phyPath);
	    	}
            }

	/**
	 * Takes an ArrayList of physical links and returns a string representing the entire 
	 * physical path.
	 * 
	 * @param phyLinks: ArrayList of physical links
	 * 
	 * @return phyPath: A String representing the physical path.
	 */ 
	private static String getPhysicalPath(
                ArrayList<HashMap<String, Object>> phyLinks) {
	    String phyPath = new String();
	    for(HashMap<String,Object> vPath: phyLinks){
		HashMap<String,String> srcNode = new HashMap<String,String>();
		HashMap<String,String> dstNode = new HashMap<String,String>();
		srcNode = (HashMap<String,String>) vPath.get(SRC);
		phyPath.concat(srcNode.get(SWID));
		phyPath.concat(FWDSLASH);
		phyPath.concat(srcNode.get(PORTNUM));
		phyPath.concat(HYPHEN);
		dstNode = (HashMap<String,String>) vPath.get(DST);
		phyPath.concat(dstNode.get(SWID));
		phyPath.concat(FWDSLASH);
		phyPath.concat(dstNode.get(PORTNUM));
		phyPath.concat(COMMA);
	    }
	    
	    //Remove the last comma
	    phyPath = phyPath.replace(phyPath.substring(phyPath.length()-1), "");
	    return phyPath;
        }

	
	/**
	 * Takes a linkId and an ArrayList of a HashMap obtained from the json file
	 * to return an ArrayList of physical links (represented by src and dst nodes) 
	 * 
	 * @param linkId: An Integer representing the link id.
	 * @param linkMap: ArrayList otained from the json file
	 * 
	 * @return phyLinks: A String representing the physical path.
	 */ 
	private static ArrayList<HashMap<String, Object>> getPhysicalLinks(
                Integer linkId, ArrayList<HashMap<String, Object>> linkMap) {
	    ArrayList<HashMap<String,Object>> phyLinks = new ArrayList<HashMap<String,Object>>();
	    for(HashMap<String,Object> vLink: linkMap){
		if(vLink.get(LINKID)==linkId){
		    phyLinks = (ArrayList<HashMap<String,Object>>) vLink.get(PHYLINK);
		}
	    }
	    return phyLinks;
        }

	/**
	 * Takes a json deserialized ArrayList and instantiates the node element.
	 * 
	 * @param vNodes: ArrayList of virtual nodes
	 */
	private static void virtualSwitchFromJson(
		ArrayList<HashMap<String, Object>> vNodes) {
	        for(HashMap<String,Object> row: vNodes){
	            tenant = (Integer) row.get(TID);
	            dpid = (String) row.get(DPID);
	            ArrayList<Integer> portNumbers = (ArrayList<Integer>) row.get(PORT);
	            ArrayList<HashMap<String,Object>> switchMap = new ArrayList<HashMap<String,Object>>();
	            ArrayList<HashMap<String,Object>> map = config.get(MAP);
	            for (HashMap<String,Object> tenantMap:map){
	        	if(tenantMap.get(TID)==tenant){
	        	    switchMap = (ArrayList<HashMap<String,Object>>) tenantMap.get(SWMAP);
	        	}
	            }
	            ArrayList<String> phyDpid = getPhysicalSwitches(dpid,switchMap);
	          manager.createOVXSwitch(tenant,phyDpid);
	        }
	        
            }

	
	/**
	 * Takes a switch dpid and an ArrayList of a HashMap obtained from the json file
	 * to return an ArrayList of physical switches.
	 * 
	 * @param sw: A String representing the dpid.
	 * @param switchMap: ArrayList otained from the json file
	 * 
	 * @return phySwitches:An ArrayList of String representing the physical switches.
	 */ 
	private static ArrayList<String> getPhysicalSwitches(String sw, ArrayList<HashMap<String, Object>> switchMap) {
	    ArrayList<String> phySwitches = new ArrayList<String>();
	    for(HashMap<String,Object> vSwitch: switchMap){
		if(vSwitch.get(VSWID)==sw){
		    phySwitches = (ArrayList<String>) vSwitch.get(PSWID);
		}
	    }
	    return phySwitches;
        }

	/**
	 * Takes a json deserialized ArrayList and instantiates the network element.
	 * 
	 * @param vNetMap: ArrayList of virtual networks
	 */
	private static void virtualNetworkFromJson(
		ArrayList<HashMap<String, Object>> vNetMap) {
		for (HashMap<String,Object> row: vNetMap){
		    tenant = (Integer) row.get(TID);		    
		    network = (String) row.get(NET);
		    getIpAndMask(network);
		    dpids = (ArrayList<String>) row.get(SWID);
		    controllerAddress = (String) row.get(CON_ADDR);
		    controllerPort =  (Integer) row.get(CON_PORT);
		    manager.createOVXNetwork(PROTOCOL,controllerAddress,controllerPort,networkAddress,mask);
  
		}
            }

	/**
	 * Takes a string of ipAddress plus mask and separates them.
	 * 
	 * @param net: String of network ip and mask
	 */
	private static void getIpAndMask(String net) {
	    String[] splits = net.split(FWDSLASH);
	    networkAddress = splits[0];
	    mask = Short.parseShort(splits[1]);//Short.parseInt(splits[1]); 
        }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

	}

}
