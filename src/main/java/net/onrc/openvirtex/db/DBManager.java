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
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.Port;
import net.onrc.openvirtex.exceptions.DuplicateIndexException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.routing.SwitchRoute;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoException;


/**
 * The singleton class database manager is responsible
 * for all read/write operations to persistent storage.
 * Upon start, it will read all persisted virtual networks,
 * and spawn virtual network managers for each that will
 * wait until all their elements are available and then boot
 * the virtual network.
 */
public final class DBManager {
    /**
     * Database collection name for OVX configuration.
     */
    public static final String DB_CONFIG = "CONFIG";
    /**
     * Database collection name for OVX users.
     */
    public static final String DB_USER = "USER";
    /**
     * Database collection name for virtual networks.
     */
    public static final String DB_VNET = "VNET";

    private static DBManager instance;
    private DBConnection dbConnection;
    private Map<String, DBCollection> collections;
    private boolean clear;
    // Mapping between physical dpids and a list of vnet managers
    private Map<Long, List<OVXNetworkManager>> dpidToMngr;
    // Mapping between physical links and a list of vnet managers
    private Map<DPIDandPortPair, List<OVXNetworkManager>> linkToMngr;
    // Mapping between physical ports and a list of vnet managers
    private Map<DPIDandPort, List<OVXNetworkManager>> portToMngr;

    private static Logger log = LogManager.getLogger(DBManager.class.getName());

    /** Creates the database manager instance. Connects
     * to the database backend, and creates mappings between
     * network elements and virtual network managers.
     */
    private DBManager() {
        this.dbConnection = new MongoConnection();
        this.collections = new HashMap<String, DBCollection>();
        this.dpidToMngr = new HashMap<Long, List<OVXNetworkManager>>();
        this.linkToMngr = new HashMap<DPIDandPortPair, List<OVXNetworkManager>>();
        this.portToMngr = new HashMap<DPIDandPort, List<OVXNetworkManager>>();
    }

    /**
     * Gets the database manager instance, and creates one
     * if it doesn't exist.
     *
     * @return the database manager instance
     */
    public static DBManager getInstance() {
        if (DBManager.instance == null) {
            DBManager.instance = new DBManager();
        }
        return DBManager.instance;
    }

    /**
     * Initializes database backend by creating
     * config, user and vnet collections.
     *
     * @param host the database host
     * @param port the database port
     * @param clear flag to clear the database
     */
    public void init(String host, Integer port, boolean clear) {
        this.dbConnection.connect(host, port);
        // Suppress error stream when MongoDB raises java.net.ConnectException
        // in another component (and cannot be caught)
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
            if (this.clear) {
                this.clear(DBManager.DB_VNET);
            } else {
                this.readOVXNetworks();
            }

        } catch (Exception e) {
            log.error("Failed to initialize database: {}", e.getMessage());
        } finally {
            // Restore error stream
            System.setErr(ps);
        }
    }

    /**
     * Sets the index of the given collection.
     *
     * @param coll the collection
     */
    private void setIndex(String coll) {
        // Suppress error stream when MongoDB raises java.net.ConnectException
        // in another component (and cannot be caught)
        PrintStream ps = System.err;
        System.setErr(null);
        try {
            BasicDBObject options = new BasicDBObject("unique", true);
            BasicDBObject index = new BasicDBObject(TenantHandler.TENANT, 1);
            this.collections.get(coll).ensureIndex(index, options);
        } catch (Exception e) {
            log.error("Failed to set database index: {}", e.getMessage());
        } finally {
            // Restore error stream
            System.setErr(ps);
        }
    }

    /**
     * Clears the given collection in the database.
     *
     * @param coll the collection
     */
    private void clear(String coll) {
        // Suppress error stream when MongoDB raises java.net.ConnectException
        // in another component (and cannot be caught)
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

    /**
     * Closes connection to database backend.
     */
    public void close() {
        // Suppress error stream when MongoDB raises java.net.ConnectException
        // in another component (and cannot be caught)
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
     * Creates document in db from persistable object obj.
     *
     * @param obj the object to create
     */
    public void createDoc(Persistable obj) {
        // Suppress error stream when MongoDB raises java.net.ConnectException
        // in another component (and cannot be caught)
        PrintStream ps = System.err;
        System.setErr(null);
        try {
            DBCollection collection = this.collections.get(obj.getDBName());
            collection.insert(new BasicDBObject(obj.getDBObject()));
        } catch (Exception e) {
            // Do not log when duplicate key
            // Virtual network was already stored and we're trying to create it
            // again on startup
            if (e instanceof MongoException.DuplicateKey) {
                log.warn("Skipped saving of virtual network with duplicate tenant id");
            } else {
                log.error("Failed to insert document into database: {}", e.getMessage());
            }
        } finally {
            // Restore error stream
            System.setErr(ps);
        }
    }

    /**
     * Removes document from db.
     *
     * @param obj the object to remove
     */
    public void removeDoc(Persistable obj) {
        // Suppress error stream when MongoDB raises java.net.ConnectException
        // in another component (and cannot be caught)
        PrintStream ps = System.err;
        System.setErr(null);
        try {
            DBCollection collection = this.collections.get(obj.getDBName());
            collection.remove(new BasicDBObject(obj.getDBObject()));
        } catch (Exception e) {
            log.error("Failed to remove document from database: {}",
                    e.getMessage());
        } finally {
            // Restore error stream
            System.setErr(ps);
        }
    }

    /**
     * Saves persistable object obj.
     *
     * @param obj the object to persist
     */
    public void save(Persistable obj) {
        BasicDBObject query = new BasicDBObject();
        query.putAll(obj.getDBIndex());
        BasicDBObject update = new BasicDBObject("$addToSet",
                new BasicDBObject(obj.getDBKey(), obj.getDBObject()));
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
     * Removes persistable object obj.
     *
     * @param obj the object to remove
     */
    public void remove(Persistable obj) {
        BasicDBObject query = new BasicDBObject();
        query.putAll(obj.getDBIndex());
        BasicDBObject update = new BasicDBObject("$pull", new BasicDBObject(
                obj.getDBKey(), obj.getDBObject()));
        PrintStream ps = System.err;
        System.setErr(null);
        try {
            DBCollection collection = this.collections.get(obj.getDBName());
            collection.update(query, update);
        } catch (Exception e) {
            log.error("Failed to remove from db: {}", e.getMessage());
        } finally {
            System.setErr(ps);
        }
    }

    /**
     * Removes all routes of switch for given tenant.
     *
     * @param tenantId the tenant ID
     * @param switchId the dpid
     */
    public void removeSwitchPath(int tenantId, long switchId) {
        BasicDBObject query = new BasicDBObject();
        query.put(TenantHandler.TENANT, tenantId);
        BasicDBObject pull = new BasicDBObject("$pull", new BasicDBObject(
                SwitchRoute.DB_KEY, new BasicDBObject(TenantHandler.DPID,
                        switchId)));
        PrintStream ps = System.err;
        System.setErr(null);
        try {
            DBCollection collection = this.collections.get(DB_VNET);
            collection.update(query, pull);
        } catch (Exception e) {
            log.error("Failed to remove from db: {}", e.getMessage());
        } finally {
            System.setErr(ps);
        }
    }

    /**
     * Removes stored path of vlink for specified tenant.
     *
     * @param tenantId the tenant ID
     * @param linkId the link ID
     */
    public void removeLinkPath(int tenantId, int linkId) {
        BasicDBObject query = new BasicDBObject();
        query.put(TenantHandler.TENANT, tenantId);
        BasicDBObject pull = new BasicDBObject("$pull", new BasicDBObject(
                OVXLink.DB_KEY, new BasicDBObject(TenantHandler.LINK, linkId)));
        PrintStream ps = System.err;
        System.setErr(null);
        try {
            DBCollection collection = this.collections.get(DB_VNET);
            collection.update(query, pull);
        } catch (Exception e) {
            log.error("Failed to remove from db: {}", e.getMessage());
        } finally {
            System.setErr(ps);
        }
    }

    /**
     * Reads all virtual networks from database and spawn an OVXNetworkManager
     * for each.
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
                OVXNetworkManager mngr = null;
                Map<String, Object> vnet = cursor.next().toMap();
                try {
                    // Create vnet manager for each virtual network
                    mngr = new OVXNetworkManager(vnet);
                    OVXNetwork.reserveTenantId(mngr.getTenantId());
                    // Accessing DB_KEY field through a class derived from the
                    // abstract OVXSwitch
                    List<Map<String, Object>> switches = (List<Map<String, Object>>) vnet
                            .get(Switch.DB_KEY);
                    List<Map<String, Object>> links = (List<Map<String, Object>>) vnet
                            .get(Link.DB_KEY);
                    List<Map<String, Object>> ports = (List<Map<String, Object>>) vnet
                            .get(Port.DB_KEY);
                    List<Map<String, Object>> routes = (List<Map<String, Object>>) vnet
                            .get(SwitchRoute.DB_KEY);
                    this.readOVXSwitches(switches, mngr);
                    this.readOVXLinks(links, mngr);
                    this.readOVXPorts(ports, mngr);
                    this.readOVXRoutes(routes, mngr);
                    DBManager.log
                            .info("Virtual network {} waiting for {} switches, {} links and {} ports",
                                    mngr.getTenantId(), mngr.getSwitchCount(),
                                    mngr.getLinkCount(), mngr.getPortCount());
                } catch (IndexOutOfBoundException | DuplicateIndexException e) {
                    DBManager.log.error(
                            "Failed to load virtual network {}: {}",
                            mngr.getTenantId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to load virtual networks from db: {}",
                    e.getMessage());
        } finally {
            System.setErr(ps);
        }
    }

    /**
     * Reads OVX switches from a list of maps in db format and registers them in
     * their manager.
     *
     * @param switches the list of switches
     * @param mngr the virtual network manager
     */
    @SuppressWarnings("unchecked")
    private void readOVXSwitches(List<Map<String, Object>> switches,
            OVXNetworkManager mngr) {
        if (switches == null) {
            return;
        }
        // Read explicit switch mappings (virtual to physical)
        for (Map<String, Object> sw : switches) {
            List<Long> physwitches = (List<Long>) sw.get(TenantHandler.DPIDS);
            for (Long physwitch : physwitches) {
                mngr.registerSwitch(physwitch);
                List<OVXNetworkManager> mngrs = this.dpidToMngr.get(physwitch);
                if (mngrs == null) {
                    this.dpidToMngr.put(physwitch,
                            new ArrayList<OVXNetworkManager>());
                }
                this.dpidToMngr.get(physwitch).add(mngr);
            }
        }
    }

    /**
     * Reads OVX links from a list of maps in db format and registers them in
     * their manager. Also reads switches that form a virtual link and registers
     * them.
     *
     * @param links the list of links
     * @param mngr the virtual network manager
     */
    @SuppressWarnings("unchecked")
    private void readOVXLinks(List<Map<String, Object>> links,
            OVXNetworkManager mngr) {
        if (links == null) {
            return;
        }
        // Register links in the appropriate manager
        for (Map<String, Object> link : links) {
            List<Map<String, Object>> path = (List<Map<String, Object>>) link
                    .get(TenantHandler.PATH);
            for (Map<String, Object> hop : path) {
                // Fetch link
                Long srcDpid = (Long) hop.get(TenantHandler.SRC_DPID);
                Short srcPort = ((Integer) hop.get(TenantHandler.SRC_PORT))
                        .shortValue();
                Long dstDpid = (Long) hop.get(TenantHandler.DST_DPID);
                Short dstPort = ((Integer) hop.get(TenantHandler.DST_PORT))
                        .shortValue();
                DPIDandPortPair dpp = new DPIDandPortPair(new DPIDandPort(
                        srcDpid, srcPort), new DPIDandPort(dstDpid, dstPort));
                // Register link in current manager
                mngr.registerLink(dpp);
                // Update list of managers that wait for this link
                List<OVXNetworkManager> mngrs = this.linkToMngr.get(dpp);
                if (mngrs == null) {
                    this.linkToMngr
                            .put(dpp, new ArrayList<OVXNetworkManager>());
                }
                this.linkToMngr.get(dpp).add(mngr);

                // Register src/dst switches of this link
                mngr.registerSwitch(srcDpid);
                mngr.registerSwitch(dstDpid);
                // Update list of managers that wait for these switches
                mngrs = this.dpidToMngr.get(srcDpid);
                if (mngrs == null) {
                    this.dpidToMngr.put(srcDpid,
                            new ArrayList<OVXNetworkManager>());
                }
                this.dpidToMngr.get(srcDpid).add(mngr);
                mngrs = this.dpidToMngr.get(dstDpid);
                if (mngrs == null) {
                    this.dpidToMngr.put(dstDpid,
                            new ArrayList<OVXNetworkManager>());
                }
                this.dpidToMngr.get(dstDpid).add(mngr);
            }
        }
    }

    /**
     * Reads OVX links from a list of maps in db format and registers them in
     * their manager. Also reads switches that form a virtual link and register
     * them.
     *
     * @param ports the list of ports
     * @param mngr the virtual network manager
     */
    private void readOVXPorts(List<Map<String, Object>> ports,
            OVXNetworkManager mngr) {
        if (ports == null) {
            return;
        }
        for (Map<String, Object> port : ports) {
            // Read dpid and port number
            Long dpid = (Long) port.get(TenantHandler.DPID);
            Short portNumber = ((Integer) port.get(TenantHandler.PORT))
                    .shortValue();
            DPIDandPort p = new DPIDandPort(dpid, portNumber);
            // Register port in current manager
            mngr.registerPort(p);
            // Update list of managers that wait for this port
            List<OVXNetworkManager> mngrs = this.portToMngr.get(p);
            if (mngrs == null) {
                this.portToMngr.put(p, new ArrayList<OVXNetworkManager>());
            }
            this.portToMngr.get(p).add(mngr);
        }
    }

    /**
     * Reads OVX routes from a list of maps in db format and registers the
     * switches and links in their manager.
     *
     * @param routes the list of routes
     * @param mngr the virtual network manager
     */
    @SuppressWarnings({ "unchecked" })
    private void readOVXRoutes(List<Map<String, Object>> routes,
            OVXNetworkManager mngr) {
        if (routes == null) {
            return;
        }
        for (Map<String, Object> route : routes) {
            List<Map<String, Object>> path = (List<Map<String, Object>>) route
                    .get(TenantHandler.PATH);
            for (Map<String, Object> hop : path) {
                Long srcDpid = (Long) hop.get(TenantHandler.SRC_DPID);
                Short srcPort = ((Integer) hop.get(TenantHandler.SRC_PORT))
                        .shortValue();
                Long dstDpid = (Long) hop.get(TenantHandler.DST_DPID);
                Short dstPort = ((Integer) hop.get(TenantHandler.DST_PORT))
                        .shortValue();
                DPIDandPortPair dpp = new DPIDandPortPair(new DPIDandPort(
                        srcDpid, srcPort), new DPIDandPort(dstDpid, dstPort));
                // Register links in the appropriate manager
                mngr.registerLink(dpp);
                List<OVXNetworkManager> mngrs = this.linkToMngr.get(dpp);
                if (mngrs == null) {
                    this.linkToMngr.put(dpp, new ArrayList<OVXNetworkManager>());
                }
                this.linkToMngr.get(dpp).add(mngr);

                // Register switches
                mngr.registerSwitch(srcDpid);
                mngr.registerSwitch(dstDpid);
                mngrs = this.dpidToMngr.get(srcDpid);
                if (mngrs == null) {
                    this.dpidToMngr.put(srcDpid, new ArrayList<OVXNetworkManager>());
                }
                this.dpidToMngr.get(srcDpid).add(mngr);
                mngrs = this.dpidToMngr.get(dstDpid);
                if (mngrs == null) {
                    this.dpidToMngr.put(dstDpid,
                            new ArrayList<OVXNetworkManager>());
                }
                this.dpidToMngr.get(dstDpid).add(mngr);
            }
        }
    }

    /**
     * Adds physical switch to the OVXNetworkManagers that are waiting for this
     * switch. Removes OVXNetworkManagers that were booted after adding this
     * switch. This method is called by the PhysicalSwitch.boot() method.
     *
     * @param dpid the swith dpid
     */
    public void addSwitch(final Long dpid) {
        // Disregard physical switch creation if OVX was started with --dbClear
        if (!this.clear) {
            List<OVXNetworkManager> completedMngrs = new ArrayList<OVXNetworkManager>();
            synchronized (this.dpidToMngr) {
                // Lookup virtual networks that use this physical switch
                List<OVXNetworkManager> mngrs = this.dpidToMngr.get(dpid);
                if (mngrs != null) {
                    for (OVXNetworkManager mngr : mngrs) {
                        mngr.setSwitch(dpid);
                        if (mngr.getStatus()) {
                            completedMngrs.add(mngr);
                        }
                    }
                }
            }
            this.removeOVXNetworkManagers(completedMngrs);
        }
    }

    /**
     * Deletes physical switch from the OVXNetworkManagers that are waiting for
     * this switch. This method is called by PhysicalNetwork when switch has
     * disconnected.
     *
     * @param dpid the switch dpid
     */
    public void delSwitch(final Long dpid) {
        // Disregard physical switch deletion if OVX was started with --dbClear
        if (!this.clear) {
            synchronized (this.dpidToMngr) {
                // Lookup virtual networks that use this physical switch
                List<OVXNetworkManager> mngrs = this.dpidToMngr.get(dpid);
                if (mngrs != null) {
                    for (OVXNetworkManager mngr : mngrs) {
                        mngr.unsetSwitch(dpid);
                    }
                }
            }
        }
    }

    /**
     * Adds physical link to the OVXNetworkManagers that are waiting for this
     * link. Removes OVXNetworkManagers that were booted after adding this link.
     *
     * @param dpp physical link given as a dpid and port pair
     */
    public void addLink(final DPIDandPortPair dpp) {
        // Disregard physical link creation if OVX was started with --dbClear
        if (!this.clear) {
            List<OVXNetworkManager> completedMngrs = new ArrayList<OVXNetworkManager>();
            synchronized (this.linkToMngr) {
                // Lookup virtual networks that use this physical link
                List<OVXNetworkManager> mngrs = this.linkToMngr.get(dpp);
                if (mngrs != null) {
                    for (OVXNetworkManager mngr : mngrs) {
                        mngr.setLink(dpp);
                        if (mngr.getStatus()) {
                            completedMngrs.add(mngr);
                        }
                    }
                }
            }
            this.removeOVXNetworkManagers(completedMngrs);
        }
    }

    /**
     * Deletes physical link from the OVXNetworkManagers that are waiting for
     * this switch.
     *
     * @param dpp the physical link given as a dpid and port pair
     */
    public void delLink(final DPIDandPortPair dpp) {
        // Disregard physical link deletion if OVX was started with --dbClear
        if (!this.clear) {
            synchronized (this.linkToMngr) {
                // Lookup virtual networks that use this physical link
                List<OVXNetworkManager> mngrs = this.linkToMngr.get(dpp);
                if (mngrs != null) {
                    for (OVXNetworkManager mngr : mngrs) {
                        mngr.unsetLink(dpp);
                    }
                }
            }
        }
    }

    /**
     * Adds physical port to the OVXNetworkManagers that are waiting for this
     * port. Remove OVXNetworkManagers that were booted after adding this port.
     *
     * @param port the port given as a dpid and port pair
     */
    public void addPort(final DPIDandPort port) {
        // Disregard physical port creation if OVX was started with --dbClear
        if (!this.clear) {
            List<OVXNetworkManager> completedMngrs = new ArrayList<OVXNetworkManager>();
            synchronized (this.portToMngr) {
                // Lookup virtual networks that use this physical port
                List<OVXNetworkManager> mngrs = this.portToMngr.get(port);
                if (mngrs != null) {
                    for (OVXNetworkManager mngr : mngrs) {
                        mngr.setPort(port);
                        if (mngr.getStatus()) {
                            completedMngrs.add(mngr);
                        }
                    }
                }
            }
            this.removeOVXNetworkManagers(completedMngrs);
        }
    }

    /**
     * Deletes physical port from the OVXNetworkManagers that are waiting for
     * this switch.
     *
     * @param port the port given as a dpid and port pair
     */
    public void delPort(final DPIDandPort port) {
        // Disregard physical link deletion if OVX was started with --dbClear
        if (!this.clear) {
            synchronized (this.dpidToMngr) {
                // Lookup virtual networks that use this physical link
                List<OVXNetworkManager> mngrs = this.portToMngr.get(port);
                if (mngrs != null) {
                    for (OVXNetworkManager mngr : mngrs) {
                        mngr.unsetPort(port);
                    }
                }
            }
        }
    }

    /**
     * Removes network managers that were waiting for switches, links or ports.
     *
     * @param mngrs list of virtual network managers
     */
    private void removeOVXNetworkManagers(List<OVXNetworkManager> mngrs) {
        for (OVXNetworkManager mngr : mngrs) {
            synchronized (this.dpidToMngr) {
                for (Long dpid : this.dpidToMngr.keySet()) {
                    this.dpidToMngr.get(dpid).remove(mngr);
                }
            }
            synchronized (this.linkToMngr) {
                for (DPIDandPortPair dpp : this.linkToMngr.keySet()) {
                    this.linkToMngr.get(dpp).remove(mngr);
                }
            }
            synchronized (this.portToMngr) {
                for (DPIDandPort dp : this.portToMngr.keySet()) {
                    this.portToMngr.get(dp).remove(mngr);
                }
            }
        }
    }
}
