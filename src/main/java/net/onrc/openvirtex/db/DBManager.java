package net.onrc.openvirtex.db;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.elements.Persistable;
import net.onrc.openvirtex.elements.datapath.DPIDandPort;
import net.onrc.openvirtex.elements.datapath.DPIDandPortPair;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.Link;
import net.onrc.openvirtex.routing.SwitchRoute;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoException;

public class DBManager {
	public static final String DB_CONFIG = "CONFIG";
	public static final String DB_USER = "USER";
	public static final String DB_VNET = "VNET";

	private static DBManager instance;
	private DBConnection dbConnection;
	private Map<String, DBCollection> collections;
	private boolean clear;
	// Mapping between physical dpids and a list of vnet managers
	private Map<Long, List<OVXNetworkManager>> dpidToMngr; 
	// Mapping between physical links and a list of vnet managers
	private Map<DPIDandPortPair, List<OVXNetworkManager>> linkToMngr;
	// Map between physical (dpid, port)-pair and unique link id

	private static Logger log = LogManager.getLogger(DBManager.class
			.getName());

	private DBManager() {
		this.dbConnection = new MongoConnection();
		this.collections = new HashMap<String, DBCollection>();
		this.dpidToMngr = new HashMap<Long, List<OVXNetworkManager>>();
		this.linkToMngr = new HashMap<DPIDandPortPair, List<OVXNetworkManager>>();
	}

	public static DBManager getInstance() {
		if (DBManager.instance == null)
			DBManager.instance = new DBManager();
		return DBManager.instance;
	}

	/**
	 * Creates config, user and vnet collections
	 */
	public void init(String host, Integer port, boolean clear) {
		this.dbConnection.connect(host, port);
		// Suppress error stream when MongoDB raises java.net.ConnectException in another component (and cannot be caught)
		PrintStream ps = System.err;
		System.setErr(null);
		try {
			// Retrieve (create if non-existing) collections from db
			// and store their handlers
			DB db = ((MongoConnection) this.dbConnection).getDB();

			DBCollection cfg = db.getCollection(DBManager.DB_CONFIG);
			this.collections.put(DBManager.DB_CONFIG, cfg);
			DBCollection user = db.getCollection(DBManager.DB_USER);
			this.collections.put(DBManager.DB_USER, user);
			DBCollection vnet = db.getCollection(DBManager.DB_VNET);
			this.collections.put(DBManager.DB_VNET, vnet);

			this.setIndex(DBManager.DB_VNET);

			this.clear = clear;
			if (this.clear)
				this.clear(DBManager.DB_VNET);
			else
				this.readOVXNetworks();
			
		} catch (Exception e) {
			log.error("Failed to initialize database: {}", e.getMessage());
		} finally {
			// Restore error stream
			System.setErr(ps);
		}
	}

	private void setIndex(String coll) {
		// Suppress error stream when MongoDB raises java.net.ConnectException in another component (and cannot be caught)
		PrintStream ps = System.err;
		System.setErr(null);
		try {
			BasicDBObject index = new BasicDBObject(TenantHandler.TENANT, 1);
			BasicDBObject options = new BasicDBObject("unique", true);
			this.collections.get(coll).ensureIndex(index, options);
		} catch (Exception e) {
			log.error("Failed to set database index: {}", e.getMessage());
		} finally {
			// Restore error stream
			System.setErr(ps);
		}
	}

	private void clear(String coll) {
		// Suppress error stream when MongoDB raises java.net.ConnectException in another component (and cannot be caught)
		PrintStream ps = System.err;
		System.setErr(null);
		try {
			this.collections.get(coll).drop();
			this.setIndex(DBManager.DB_VNET);
		} catch (Exception e) {
			log.error("Failed to clear database: {}", e.getMessage());
		} finally {
			// Restore error stream
			System.setErr(ps);
		}
	}

	public void close() {
		// Suppress error stream when MongoDB raises java.net.ConnectException in another component (and cannot be caught)
		PrintStream ps = System.err;
		System.setErr(null);
		try {
			this.dbConnection.disconnect();
		} catch (Exception e) {
			log.error("Failed to close database connection: {}", e.getMessage());
		} finally {
			// Restore error stream
			System.setErr(ps);			
		}
	}

	/**
	 * Create persistable object obj
	 */
	public void create(Persistable obj) {
		// Suppress error stream when MongoDB raises java.net.ConnectException in another component (and cannot be caught)
		PrintStream ps = System.err;
		System.setErr(null);
		try {
			DBCollection collection = this.collections.get(obj.getDBName());
			collection.insert(new BasicDBObject(obj.getDBObject()));
		} catch (Exception e) {
			// Do not log when duplicate key
			// Virtual network was already stored and we're trying to create it again on startup
			if (e instanceof MongoException.DuplicateKey)
				log.warn("Skipped saving of virtual network with duplicate tenant id");
			else
				log.error("Failed to insert into database: {}", e.getMessage());
		} finally {
			// Restore error stream
			System.setErr(ps);
		}
	}

	/**
	 * Save persistable object obj
	 * @param obj
	 * @param coll
	 */
	public void save(Persistable obj) {
		BasicDBObject query = new BasicDBObject();
		for (String key: obj.getDBIndex().keySet())
			query.put(key,  obj.getDBIndex().get(key));
		BasicDBObject update = new BasicDBObject("$addToSet", new BasicDBObject(obj.getDBKey(), obj.getDBObject()));
		PrintStream ps = System.err;
		System.setErr(null);
		try {
			DBCollection collection = this.collections.get(obj.getDBName());
			collection.update(query, update, true, false);
		} catch (Exception e) {
			log.error("Failed to update database: {}", e.getMessage());
		} finally {
			System.setErr(ps);
		}
	}

	/**
	 * Remove persistable object obj
	 * @param obj
	 * @param coll
	 */
	public void remove(Persistable obj) {
		PrintStream ps = System.err;
		System.setErr(null);
		try {
			DBCollection collection = this.collections.get(obj.getDBName());		
			collection.remove(new BasicDBObject(obj.getDBObject()));
		} catch (Exception e) {
			log.error("Failed to remove from db: {}", e.getMessage());
		} finally {
			System.setErr(ps);
		}
	}

	/**
	 * Read all virtual networks from database and spawn an OVXNetworkManager for each.
	 */
	@SuppressWarnings("unchecked")
	private void readOVXNetworks() {
		PrintStream ps = System.err;
		System.setErr(null);
		try {
			// Get a cursor over all virtual networks
			DBCollection coll = this.collections.get(DBManager.DB_VNET);
			DBCursor cursor = coll.find();
			log.info("Loading {} virtual networks from database", cursor.size());
			while (cursor.hasNext()) {
				Map<String, Object> vnet = cursor.next().toMap();
				// Create vnet manager for each virtual network
				OVXNetworkManager mngr = new OVXNetworkManager(vnet);
				// Accessing DB_KEY field through a class derived from the abstract OVXSwitch
				List<Map<String, Object>> switches = (List<Map<String, Object>>) vnet.get(Switch.DB_KEY);
				List<Map<String, Object>> links = (List<Map<String, Object>>) vnet.get(Link.DB_KEY);
				List<Map<String, Object>> routes = (List<Map<String, Object>>) vnet.get(SwitchRoute.DB_KEY);
				this.readOVXSwitches(switches, mngr);
				this.readOVXLinks(links, mngr);
				this.readOVXRoutes(routes, mngr);
				DBManager.log.info("Virtual network {} waiting for {} switches and {} links", mngr.getTenantId(), mngr.getSwitchCount(), mngr.getLinkCount());
			}
		} catch (Exception e) {
			log.error("Failed to load virtual networks from db: {}", e.getMessage());
		} finally {
			System.setErr(ps);
		}
	}

	/**
	 * Read OVX switches from a list of maps in db format and register them in their manager.
	 * @param switches
	 * @param mngr
	 */
	@SuppressWarnings("unchecked")
	private void readOVXSwitches(List<Map<String, Object>> switches, OVXNetworkManager mngr) {
		if (switches == null)
			return;
		// Read explicit switch mappings (virtual to physical)
		for (Map<String, Object> sw: switches) {
			List<Long> physwitches = (List<Long>) sw.get(TenantHandler.DPIDS);
			for (Long physwitch: physwitches) {
				mngr.registerSwitch(physwitch);
				List<OVXNetworkManager> mngrs = this.dpidToMngr.get(physwitch);
				if (mngrs == null)
					this.dpidToMngr.put(physwitch, new ArrayList<OVXNetworkManager>());
				this.dpidToMngr.get(physwitch).add(mngr);
			}
		}
	}

	/**
	 * Read OVX links from a list of maps in db format and register them in their manager.
	 * Also read switches that form a virtual link and register them.
	 * @param links
	 * @param mngr
	 */
	@SuppressWarnings("unchecked")
	private void readOVXLinks(List<Map<String, Object>> links, OVXNetworkManager mngr) {
		if (links == null)
			return;
		// Register links in the appropriate manager
		for (Map<String, Object> link: links) {
			List<Map> path = (List<Map>) link.get(TenantHandler.PATH);
			for (Map<String, Object> hop: path) {
				Long srcDpid = (Long) hop.get(TenantHandler.SRC_DPID);
				Short srcPort = ((Integer) hop.get(TenantHandler.SRC_PORT)).shortValue();
				Long dstDpid = (Long) hop.get(TenantHandler.DST_DPID);
				Short dstPort = ((Integer) hop.get(TenantHandler.DST_PORT)).shortValue();
				DPIDandPortPair dpp = new DPIDandPortPair(new DPIDandPort(srcDpid, srcPort),
						new DPIDandPort(dstDpid, dstPort));
				mngr.registerLink(dpp);
				List<OVXNetworkManager> mngrs = this.linkToMngr.get(dpp);
				if (mngrs == null)
					this.linkToMngr.put(dpp, new ArrayList<OVXNetworkManager>());
				this.linkToMngr.get(dpp).add(mngr);
				
				// Register switches
				mngr.registerSwitch(srcDpid);
				mngr.registerSwitch(dstDpid);
				mngrs = this.dpidToMngr.get(srcDpid);
				if (mngrs == null)
					this.dpidToMngr.put(srcDpid, new ArrayList<OVXNetworkManager>());
				this.dpidToMngr.get(srcDpid).add(mngr);
				mngrs = this.dpidToMngr.get(dstDpid);
				if (mngrs == null)
					this.dpidToMngr.put(dstDpid, new ArrayList<OVXNetworkManager>());
				this.dpidToMngr.get(dstDpid).add(mngr);
			}
		}
	}

	/**
	 * Read OVX routes from a list of maps in db format and register the switches and links in their manager.
	 * @param links
	 * @param mngr
	 */
	@SuppressWarnings("unchecked")
	private void readOVXRoutes(List<Map<String, Object>> routes, OVXNetworkManager mngr) {
		if (routes == null)
			return;
		for (Map<String, Object> route: routes) {
			List<Map> path = (List<Map>) route.get(TenantHandler.PATH);
			for (Map<String, Object> hop: path) {
				Long srcDpid = (Long) hop.get(TenantHandler.SRC_DPID);
				Short srcPort = ((Integer) hop.get(TenantHandler.SRC_PORT)).shortValue();
				Long dstDpid = (Long) hop.get(TenantHandler.DST_DPID);
				Short dstPort = ((Integer) hop.get(TenantHandler.DST_PORT)).shortValue();
				DPIDandPortPair dpp = new DPIDandPortPair(new DPIDandPort(srcDpid, srcPort),
						new DPIDandPort(dstDpid, dstPort));
				// Register links in the appropriate manager
				mngr.registerLink(dpp);
				List<OVXNetworkManager> mngrs = this.linkToMngr.get(dpp);
				if (mngrs == null)
					this.linkToMngr.put(dpp, new ArrayList<OVXNetworkManager>());
				this.linkToMngr.get(dpp).add(mngr);
				
				// Register switches
				mngr.registerSwitch(srcDpid);
				mngr.registerSwitch(dstDpid);
				mngrs = this.dpidToMngr.get(srcDpid);
				if (mngrs == null)
					this.dpidToMngr.put(srcDpid, new ArrayList<OVXNetworkManager>());
				this.dpidToMngr.get(srcDpid).add(mngr);
				mngrs = this.dpidToMngr.get(dstDpid);
				if (mngrs == null)
					this.dpidToMngr.put(dstDpid, new ArrayList<OVXNetworkManager>());
				this.dpidToMngr.get(dstDpid).add(mngr);
			}
		}
	}
	
	/**
	 * Add physical switch to the OVXNetworkManagers that are waiting for this switch.  
	 * This method is called by the PhysicalSwitch.boot() method 
	 */
	public void addSwitch(final Long dpid) {
		// Disregard physical switch creation if OVX was started with --dbClear
		if (!this.clear) {
			// Lookup virtual networks that use this physical switch
			List<OVXNetworkManager> mngrs = this.dpidToMngr.get(dpid);
			if (mngrs != null) {
				for (OVXNetworkManager mngr: mngrs)
					mngr.setSwitch(dpid);
			}
		}
	}

	/**
	 * Delete physical switch from the OVXNetworkManagers that are waiting for this switch.  
	 * This method is called by PhysicalNetwork when switch has disconnected. 
	 */
	public void delSwitch(final Long dpid) {
		// Disregard physical switch deletion if OVX was started with --dbClear
		if (!this.clear) {
			// Lookup virtual networks that use this physical switch
			List<OVXNetworkManager> mngrs = this.dpidToMngr.get(dpid);
			if (mngrs != null) {
				for (OVXNetworkManager mngr: mngrs)
					mngr.unsetSwitch(dpid);
			}
		}
	}

	/**
	 * Add physical link to the OVXNetworkManagers that are waiting for this link.  
	 */
	public void addLink(final DPIDandPortPair dpp) {
		// Disregard physical link creation if OVX was started with --dbClear
		if (!this.clear) {
			// Lookup virtual networks that use this physical link
			List<OVXNetworkManager> mngrs = this.linkToMngr.get(dpp);
			if (mngrs != null) {
				for (OVXNetworkManager mngr: mngrs)
					mngr.setLink(dpp);
			}
		}
	}

	/**
	 * Delete physical link from the OVXNetworkManagers that are waiting for this switch.  
	 */
	public void delLink(final DPIDandPortPair dpp) {
		// Disregard physical link deletion if OVX was started with --dbClear
		if (!this.clear) {
			// Lookup virtual networks that use this physical link
			List<OVXNetworkManager> mngrs = this.linkToMngr.get(dpp);
			if (mngrs != null) {
				for (OVXNetworkManager mngr: mngrs)
					mngr.unsetLink(dpp);
			}
		}
	}
}
