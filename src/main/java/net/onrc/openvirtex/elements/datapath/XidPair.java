package net.onrc.openvirtex.elements.datapath;

/**
 * Based on XidPair by capveg
 */
public class XidPair {

	int xid;
	OVXSwitch sw;

	public XidPair(final int x, final OVXSwitch sw) {
		this.xid = x;
		this.sw = sw;
	}

	public void setXid(final int x) {
		this.xid = x;
	}

	public int getXid() {
		return this.xid;
	}

	public void setSwitch(final OVXSwitch sw) {
		this.sw = sw;
	}

	public OVXSwitch getSwitch() {
		return this.sw;
	}

}
