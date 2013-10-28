package net.onrc.openvirtex.elements.datapath;

public class DPIDandPortPair {
	private DPIDandPort src;
	private DPIDandPort dst;
	
	public DPIDandPortPair(DPIDandPort src, DPIDandPort dst) {
		this.src = src;
		this.dst = dst;
	}

	public DPIDandPort getSrc() {
		return src;
	}
	
	public DPIDandPort getDst() {
		return dst;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DPIDandPortPair other = (DPIDandPortPair) obj;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		return result;
	}
}
