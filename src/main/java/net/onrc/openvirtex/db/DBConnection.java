package net.onrc.openvirtex.db;

public interface DBConnection {
	public void connect(String host, Integer port);
	
	public void disconnect();
}
