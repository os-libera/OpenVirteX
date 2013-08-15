/**
 * 
 */
package net.onrc.openvirtex.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
	     * saves the new json file.
	     * uses gson to serialize.
	     * 
	     * @param None
	     * @return A serialized structure to the new json file
	     * 
	     */
	    public static synchronized String saveConfig(String fileName){
	    		//throws FileNotFoundException{
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		HashMap<String,Object> output = new HashMap<String,Object>();
		/*try{
		    //Gson gson = new GsonBuilder().setPrettyPrinting().create();
		   
		    FileWriter foutput = new FileWriter(fileName);	    
		    //OVXNetwork network = new OVXNetwork();
		    //network.toJson(output);
		
		    //foutput.write(gson.toJson(output));
		    
		}catch(IOException ioe){
		    System.err.println("Error: "+ioe.getStackTrace());
		}*/
		
		OpenVirteXController ovx = new OpenVirteXController("localhost",6633,8080);
		output.putAll(ovx.toJson());
		
		OVXMap map = OVXMap.getInstance();
		ArrayList<OVXNetwork> networks = (ArrayList<OVXNetwork>) map.getVirtualNetworks();
		LinkedList<HashMap<String,Object>> netList = new LinkedList<HashMap<String,Object>>();
		for(OVXNetwork network:networks){
		    netList.add(network.toJson());
		}
		output.put("virtualnetwork", netList);
		
		ArrayList<OVXSwitch> nodes = (ArrayList<OVXSwitch>) map.getVirtualSwitches();
		LinkedList<HashMap<String,Object>> nodeList = new LinkedList<HashMap<String,Object>>();
		for(OVXSwitch node:nodes){
		    nodeList.add(node.toJson());
		}
		output.put("node", nodeList);
		
		ArrayList<OVXLink> links = (ArrayList<OVXLink>) map.getVirtualLinks();
		LinkedList<HashMap<String,Object>> linkList = new LinkedList<HashMap<String,Object>>();
		for(OVXLink link:links){
		    linkList.add(link.toJson());
		}
		output.put("edge", linkList);
		
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
		    if(config.containsKey("openvirtex")){
			ovx.fromJson(config.get("openvirtex"));
		    }
		    if(config.containsKey("virtualnetwork")){
			virtualNetworkFromJson(config.get("virtualnetwork"));
			/*
			int size = config.get("virtualnetwork").size();
			int count =0;
			while(count<size){
			    OVXNetwork virNet = new OVXNetwork();
			    virNet.fromJson(config.get("virtualnetwork").get(count));
			    count++;
			}*/
		    }
		    if(config.containsKey("node")){
			virtualSwitchFromJson(config.get("node"));
			/*int size = config.get("node").size();
			int count =0;
			while(count<size){
			    OVXSwitch sw = new OVXSwitch();
			    sw.fromJson(config.get("node").get(count));
			    count++;
			}*/
		    }
		    if(config.containsKey("edge")){
			virtualEdgeFromJson(config.get("edge"));
		    }
		    /*if(config.containsKey("mapping")){
			
		    }*/
		}
		catch(IOException ioe){
		    System.err.println("Error: "+ioe.getStackTrace());
		}
	    }

	    
	    
	private static void virtualEdgeFromJson(
		ArrayList<HashMap<String, Object>> vLinks) {
	    	for(HashMap<String,Object> row: vLinks){
	            tenant = (Integer) row.get("tenant-id");
	            linkId = (Integer) row.get("link-id");
	            ArrayList<HashMap<String,Object>> linkMap = new ArrayList<HashMap<String,Object>>();
	            ArrayList<HashMap<String,Object>> map = config.get("mapping");
	            for (HashMap<String,Object> tenantMap:map){
	        	if(tenantMap.get("tenant-id")==tenant){
	        	    linkMap = (ArrayList<HashMap<String,Object>>) tenantMap.get("link-map");
	        	}
	            }
	            ArrayList<HashMap<String,Object>> phyLinks = getPhysicalLinks(linkId,linkMap); 
	            String phyPath = getPhysicalPath(phyLinks);
	            manager.createOVXLink(tenant,phyPath);
	    	}
            }

	private static String getPhysicalPath(
                ArrayList<HashMap<String, Object>> phyLinks) {
	    String phyPath = new String();
	    for(HashMap<String,Object> vPath: phyLinks){
		HashMap<String,String> srcNode = new HashMap<String,String>();
		HashMap<String,String> dstNode = new HashMap<String,String>();
		srcNode = (HashMap<String,String>) vPath.get("src");
		phyPath.concat(srcNode.get("switch-id"));
		phyPath.concat("/");
		phyPath.concat(srcNode.get("port-number"));
		phyPath.concat("-");
		dstNode = (HashMap<String,String>) vPath.get("dst");
		phyPath.concat(dstNode.get("switch-id"));
		phyPath.concat("/");
		phyPath.concat(dstNode.get("port-number"));
		phyPath.concat(",");
	    }
	    //Remove the last comma
	    phyPath = phyPath.replace(phyPath.substring(phyPath.length()-1), "");
	    return phyPath;
        }

	private static ArrayList<HashMap<String, Object>> getPhysicalLinks(
                Integer linkId, ArrayList<HashMap<String, Object>> linkMap) {
	    ArrayList<HashMap<String,Object>> phyLinks = new ArrayList<HashMap<String,Object>>();
	    for(HashMap<String,Object> vLink: linkMap){
		if(vLink.get("link-id")==linkId){
		    phyLinks = (ArrayList<HashMap<String,Object>>) vLink.get("physical-link");
		}
	    }
	    return phyLinks;
        }

	private static void virtualSwitchFromJson(
		ArrayList<HashMap<String, Object>> vNodes) {
	        for(HashMap<String,Object> row: vNodes){
	            tenant = (Integer) row.get("tenant-id");
	            dpid = (String) row.get("dpid");
	            ArrayList<Integer> portNumbers = (ArrayList<Integer>) row.get("port");
	            ArrayList<HashMap<String,Object>> switchMap = new ArrayList<HashMap<String,Object>>();
	            ArrayList<HashMap<String,Object>> map = config.get("mapping");
	            for (HashMap<String,Object> tenantMap:map){
	        	if(tenantMap.get("tenant-id")==tenant){
	        	    switchMap = (ArrayList<HashMap<String,Object>>) tenantMap.get("switch-map");
	        	}
	            }
	            ArrayList<String> phyDpid = getPhysicalSwitches(dpid,switchMap);
	          manager.createOVXSwitch(tenant,phyDpid);
	        }
	        
            }

	private static ArrayList<String> getPhysicalSwitches(String sw, ArrayList<HashMap<String, Object>> switchMap) {
	    //ArrayList<HashMap<String,Object>> vSwitch = new ArrayList<HashMap<String,Object>>();
	    ArrayList<String> phySwitches = new ArrayList<String>();
	    for(HashMap<String,Object> vSwitch: switchMap){
		if(vSwitch.get("virtual-switch-id")==sw){
		    phySwitches = (ArrayList<String>) vSwitch.get("pswitch-id");
		}
	    }
	    return phySwitches;
	    //return null;
        }

	private static void virtualNetworkFromJson(
		ArrayList<HashMap<String, Object>> vNetMap) {
	    	
	    
		for (HashMap<String,Object> row: vNetMap){
		    tenant = (Integer) row.get("tenant-id");		    
		    network = (String) row.get("network");
		    getIpAndMask(network);
		    dpids = (ArrayList<String>) row.get("switch-id");
		    controllerAddress = (String) row.get("controler-address");
		    controllerPort =  (Integer) row.get("controller-port");
		    manager.createOVXNetwork("tcp",controllerAddress,controllerPort,networkAddress,mask);
  
		}
            }

	/**
	 * Takes a string of ipAddress plus mask and separates them.
	 * 
	 * @param net: String of network ip and mask
	 */
	private static void getIpAndMask(String net) {
	    String[] splits = net.split("/");
	    networkAddress = splits[0];
	    mask = Short.parseShort(splits[1]);//Short.parseInt(splits[1]); 
        }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
