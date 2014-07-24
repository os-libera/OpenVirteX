package net.onrc.openvirtex.elements.datapath;



import net.onrc.openvirtex.elements.network.Dom0;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Dom0Switch extends OVXBigSwitch {



    private static Logger log = LogManager.getLogger(OVXSwitch.class.getName());



    public Dom0Switch(long switchId, int tenantId, Dom0 dom0) {
        super(switchId, tenantId);
        this.roleMan = new Dom0Manager(dom0);
        this.setConnected(true);
    }

}
