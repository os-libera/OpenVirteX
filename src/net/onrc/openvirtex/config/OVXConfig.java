/**
 * 
 */
package net.onrc.openvirtex.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;

import net.onrc.openvirtex.api.APITenantManager;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * @author sumanth
 * 
 */
public class OVXConfig {

    public static String                                               HOST     = "localhost";
    public static Integer                                              OVXPORT  = 6633;
    public static Integer                                              APIPORT  = 8080;

    public static String                                               VNET     = "virtualnetwork";
    public static String                                               NODE     = "node";
    public static String                                               EDGE     = "edge";
    public static String                                               SWID     = "switch-id";
    public static String                                               PORTNUM  = "port-number";
    public static String                                               SRC      = "src";
    public static String                                               DST      = "dst";
    public static String                                               OVX      = "openvirtex";
    public static String                                               TID      = "tenant-id";
    public static String                                               LINKID   = "link-id";
    public static String                                               MAP      = "mapping";
    public static String                                               LINKMAP  = "link-map";
    public static String                                               PHYLINK  = "physical-link";
    public static String                                               PORT     = "port";
    public static String                                               DPID     = "dpid";
    public static String                                               SWMAP    = "switch-map";
    public static String                                               VSWID    = "virtual-switch-id";
    public static String                                               PSWID    = "pswitch-id";
    public static String                                               NET      = "network";
    public static String                                               CON_ADDR = "controler-address";
    public static String                                               CON_PORT = "controller-port";
    public static String                                               PROTOCOL = "tcp";

    public static String                                               COMMA    = ",";
    public static String                                               HYPHEN   = "-";
    public static String                                               FWDSLASH = "/";

    private static HashMap<String, ArrayList<HashMap<String, Object>>> config;
    private static Integer                                             tenant;
    private static String                                              network;
    private static String                                              networkAddress;
    private static short                                               mask;
    private static HashMap<String, String>                             gateway;
    private static ArrayList<String>                                   dpids;
    private static String                                              controllerAddress;
    private static Integer                                             controllerPort;
    private static String                                              dpid;
    private static Integer                                             linkId;

    Logger                                                             log      = LogManager
	                                                                                .getLogger(OVXConfig.class
	                                                                                        .getName());

    private static APITenantManager                                    manager;                              // =
													     // APITenantManager.getInstance();

    /**
     * Makes a serialized json string by obtaining the instantiated values
     * of all the elements.
     * Uses gson for serialization purposes.
     * 
     * @param None
     * @return A serialized structure to the new json file
     * 
     */
    public static synchronized String saveConfig() {

	final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	// HashMap - output which would be finally serialized using gson.
	final HashMap<String, Object> output = new HashMap<String, Object>();

	// output gets the instantiated values from OpenVirteXController
	final OpenVirteXController ovx = OpenVirteXController.getInstance();
	output.putAll(ovx.toJson());

	// output gets the instantiated values from elements.network
	final OVXMap map = OVXMap.getInstance();
	if (map != null) {
	    final Collection<OVXNetwork> networks = map.getVirtualNetworks();
	    if (networks != null) {
		final LinkedList<HashMap<String, Object>> netList = new LinkedList<HashMap<String, Object>>();
		for (final OVXNetwork network : networks) {
		    netList.add(network.toJson());
		}
		output.put(OVXConfig.VNET, netList);
	    }

	    // output gets the instantiated values from elements.node
	    final Collection<OVXSwitch> nodes = map.getVirtualSwitches();
	    if (nodes != null) {
		final LinkedList<HashMap<String, Object>> nodeList = new LinkedList<HashMap<String, Object>>();
		for (final OVXSwitch node : nodes) {
		    nodeList.add(node.toJson());
		}
		output.put(OVXConfig.NODE, nodeList);
	    }

	    // output gets the instantiated values from elements.map
	    final Set<OVXLink> links = map.getVirtualLinks();
	    if (links != null) {
		final LinkedList<HashMap<String, Object>> linkList = new LinkedList<HashMap<String, Object>>();
		for (final OVXLink link : links) {
		    linkList.add(link.toJson());
		}
		output.put(OVXConfig.EDGE, linkList);
	    }

	    output.putAll(map.toJson());
	}
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
    public static synchronized void loadConfig(final String fileName)
	    throws FileNotFoundException {
	try {
	    final Gson gson = new GsonBuilder().create();
	    final File file = new File(fileName);
	    final String json = new Scanner(file).useDelimiter("\\Z").next();
	    OVXConfig.config = gson.fromJson(json,
		    new TypeToken<HashMap<String, Object>>() {
		    }.getType());
	    final OpenVirteXController ovx = OpenVirteXController.getInstance();
	    OVXConfig.manager = APITenantManager.getInstance();
	    if (OVXConfig.config.containsKey(OVXConfig.OVX)) {
		ovx.fromJson(OVXConfig.config.get(OVXConfig.OVX));
	    }
	    if (OVXConfig.config.containsKey(OVXConfig.VNET)) {
		OVXConfig.virtualNetworkFromJson(OVXConfig.config
		        .get(OVXConfig.VNET));
	    }
	    if (OVXConfig.config.containsKey(OVXConfig.NODE)) {
		OVXConfig.virtualSwitchFromJson(OVXConfig.config
		        .get(OVXConfig.NODE));
	    }
	    if (OVXConfig.config.containsKey(OVXConfig.EDGE)) {
		OVXConfig.virtualEdgeFromJson(OVXConfig.config
		        .get(OVXConfig.EDGE));
	    }
	} catch (final IOException ioe) {
	    System.err.println("Error: " + ioe.getStackTrace());
	}
    }

    /**
     * Takes a json deserialized ArrayList and instantiates the link element.
     * 
     * @param vLinks
     *            : ArrayList of virtual links
     */
    private static void virtualEdgeFromJson(
	    final ArrayList<HashMap<String, Object>> vLinks) {
	for (final HashMap<String, Object> row : vLinks) {
	    OVXConfig.tenant = ((Double) row.get(OVXConfig.TID)).intValue();
	    OVXConfig.linkId = ((Double) row.get(OVXConfig.LINKID)).intValue();
	    ArrayList<HashMap<String, Object>> linkMap = new ArrayList<HashMap<String, Object>>();
	    final ArrayList<HashMap<String, Object>> map = OVXConfig.config
		    .get(OVXConfig.MAP);
	    for (final HashMap<String, Object> tenantMap : map) {
		if (((Double) tenantMap.get(OVXConfig.TID)).intValue() == OVXConfig.tenant) {
		    linkMap = (ArrayList<HashMap<String, Object>>) tenantMap
			    .get(OVXConfig.LINKMAP);
		}
	    }
	    final ArrayList<HashMap<String, Object>> phyLinks = OVXConfig
		    .getPhysicalLinks(OVXConfig.linkId, linkMap);
	    final String phyPath = OVXConfig.getPhysicalPath(phyLinks);
	    OVXConfig.manager.createOVXLink(OVXConfig.tenant, phyPath);
	}
    }

    /**
     * Takes an ArrayList of physical links and returns a string representing
     * the entire
     * physical path.
     * 
     * @param phyLinks
     *            : ArrayList of physical links
     * 
     * @return phyPath: A String representing the physical path.
     */
    /*
     * private static String getPhysicalPath(
     * ArrayList<HashMap<String, Object>> phyLinks) {
     * StringBuilder phyPath = new StringBuilder();
     * for(HashMap<String,Object> vPath: phyLinks){
     * HashMap<String,String> srcNode = new HashMap<String,String>();
     * HashMap<String,String> dstNode = new HashMap<String,String>();
     * srcNode = (HashMap<String,String>) vPath.get(SRC);
     * phyPath.append(srcNode.get(SWID));
     * phyPath.append(FWDSLASH);
     * phyPath.append(srcNode.get(PORTNUM));
     * phyPath.append(HYPHEN);
     * dstNode = (HashMap<String,String>) vPath.get(DST);
     * phyPath.append(dstNode.get(SWID));
     * phyPath.append(FWDSLASH);
     * phyPath.append(dstNode.get(PORTNUM));
     * phyPath.append(COMMA);
     * }
     * System.out.println("phyPath: "+phyPath);
     * //Remove the last comma
     * phyPath.deleteCharAt(phyPath.length()-1);
     * return phyPath.toString();
     * }
     */

    private static String getPhysicalPath(
	    final ArrayList<HashMap<String, Object>> phyLinks) {
	String phyPath = new String();
	for (final HashMap<String, Object> vPath : phyLinks) {
	    HashMap<String, Object> srcNode = new HashMap<String, Object>();
	    HashMap<String, Object> dstNode = new HashMap<String, Object>();
	    srcNode = (HashMap<String, Object>) vPath.get(OVXConfig.SRC);
	    phyPath += srcNode.get(OVXConfig.SWID);
	    phyPath += OVXConfig.FWDSLASH;
	    short srcPort, dstPort;
	    srcPort = ((Double) srcNode.get(OVXConfig.PORTNUM)).shortValue();
	    phyPath += String.valueOf(srcPort);
	    phyPath += OVXConfig.HYPHEN;
	    dstNode = (HashMap<String, Object>) vPath.get(OVXConfig.DST);
	    phyPath += dstNode.get(OVXConfig.SWID);
	    phyPath += OVXConfig.FWDSLASH;
	    dstPort = ((Double) dstNode.get(OVXConfig.PORTNUM)).shortValue();
	    phyPath += String.valueOf(dstPort);
	    phyPath += OVXConfig.COMMA;
	}

	// Remove the last comma
	phyPath = phyPath.replace(phyPath.substring(phyPath.length() - 1), "");
	return phyPath;
    }

    /**
     * Takes a linkId and an ArrayList of a HashMap obtained from the json file
     * to return an ArrayList of physical links (represented by src and dst
     * nodes)
     * 
     * @param linkId
     *            : An Integer representing the link id.
     * @param linkMap
     *            : ArrayList otained from the json file
     * 
     * @return phyLinks: A String representing the physical path.
     */
    private static ArrayList<HashMap<String, Object>> getPhysicalLinks(
	    final Integer linkId,
	    final ArrayList<HashMap<String, Object>> linkMap) {
	ArrayList<HashMap<String, Object>> phyLinks = new ArrayList<HashMap<String, Object>>();
	for (final HashMap<String, Object> vLink : linkMap) {
	    final Integer lid = ((Double) vLink.get(OVXConfig.LINKID))
		    .intValue();
	    if (linkId instanceof Integer) {
		if (lid == linkId) {
		    phyLinks = (ArrayList<HashMap<String, Object>>) vLink
			    .get(OVXConfig.PHYLINK);
		}
	    }
	}
	return phyLinks;
    }

    /**
     * Takes a json deserialized ArrayList and instantiates the node element.
     * 
     * @param vNodes
     *            : ArrayList of virtual nodes
     */
    private static void virtualSwitchFromJson(
	    final ArrayList<HashMap<String, Object>> vNodes) {
	for (final HashMap<String, Object> row : vNodes) {
	    OVXConfig.tenant = ((Double) row.get(OVXConfig.TID)).intValue();
	    OVXConfig.dpid = (String) row.get(OVXConfig.DPID);
	    final ArrayList<Integer> portNumbers = (ArrayList<Integer>) row
		    .get(OVXConfig.PORT);
	    ArrayList<HashMap<String, Object>> switchMap = new ArrayList<HashMap<String, Object>>();
	    final ArrayList<HashMap<String, Object>> map = OVXConfig.config
		    .get(OVXConfig.MAP);
	    for (final HashMap<String, Object> tenantMap : map) {
		if (((Double) tenantMap.get(OVXConfig.TID)).intValue() == OVXConfig.tenant) {
		    switchMap = (ArrayList<HashMap<String, Object>>) tenantMap
			    .get(OVXConfig.SWMAP);
		}
	    }
	    final ArrayList<String> phyDpid = OVXConfig.getPhysicalSwitches(
		    OVXConfig.dpid, switchMap);

	    OVXConfig.manager.createOVXSwitch(OVXConfig.tenant, phyDpid);
	}

    }

    /**
     * Takes a switch dpid and an ArrayList of a HashMap obtained from the json
     * file
     * to return an ArrayList of physical switches.
     * 
     * @param sw
     *            : A String representing the dpid.
     * @param switchMap
     *            : ArrayList otained from the json file
     * 
     * @return phySwitches:An ArrayList of String representing the physical
     *         switches.
     */
    private static ArrayList<String> getPhysicalSwitches(final String sw,
	    final ArrayList<HashMap<String, Object>> switchMap) {
	ArrayList<String> phySwitches = new ArrayList<String>();
	for (final HashMap<String, Object> vSwitch : switchMap) {
	    if ((String) vSwitch.get(OVXConfig.VSWID) == sw) {
		phySwitches = (ArrayList<String>) vSwitch.get(OVXConfig.PSWID);
	    }
	}
	return phySwitches;
    }

    /**
     * Takes a json deserialized ArrayList and instantiates the network element.
     * 
     * @param vNetMap
     *            : ArrayList of virtual networks
     */
    private static void virtualNetworkFromJson(
	    final ArrayList<HashMap<String, Object>> vNetMap) {

	// Presently sleeping for 7 seconds so as to allow the network discovery
	// to complete.
	try {
	    Thread.sleep(7000);
	} catch (final InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	for (final HashMap<String, Object> row : vNetMap) {
	    OVXConfig.tenant = ((Double) row.get(OVXConfig.TID)).intValue();
	    OVXConfig.network = (String) row.get(OVXConfig.NET);
	    OVXConfig.getIpAndMask(OVXConfig.network);
	    OVXConfig.dpids = (ArrayList<String>) row.get(OVXConfig.SWID);
	    OVXConfig.controllerAddress = (String) row.get(OVXConfig.CON_ADDR);
	    OVXConfig.controllerPort = ((Double) row.get(OVXConfig.CON_PORT))
		    .intValue();
	    OVXConfig.manager.createOVXNetwork(OVXConfig.PROTOCOL,
		    OVXConfig.controllerAddress, OVXConfig.controllerPort,
		    OVXConfig.networkAddress, OVXConfig.mask);

	}
    }

    /**
     * Takes a string of ipAddress plus mask and separates them.
     * 
     * @param net
     *            : String of network ip and mask
     */
    private static void getIpAndMask(final String net) {
	final String[] splits = net.split(OVXConfig.FWDSLASH);
	OVXConfig.networkAddress = splits[0];
	OVXConfig.mask = Short.parseShort(splits[1]);// Short.parseInt(splits[1]);
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {

    }

}
