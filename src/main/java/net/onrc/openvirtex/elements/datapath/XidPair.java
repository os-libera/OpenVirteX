package net.onrc.openvirtex.elements.datapath;

/**
 * Based on XidPair by capveg 
 */
public class XidPair {

    int xid;
    OVXSwitch sw;

    public XidPair(int x, OVXSwitch sw) {
        this.xid = x;
        this.sw = sw;
    }

    public void setXid(int x) {
        this.xid = x;
    }

    public int getXid() {
        return this.xid;
    }

    public void setSwitch(OVXSwitch sw) {
        this.sw = sw;
    }

    public OVXSwitch getSwitch() {
        return this.sw;
    }

}
