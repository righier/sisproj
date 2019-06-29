package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class House {
	private final String id;
	private final String addr;
	private final int port;
	
	public House(String id, String addr, int port) {
		this.id = id;
		this.addr = addr;
		this.port = port;
	}

	public String getId() {
		return id;
	}

	public String getAddr() {
		return addr;
	}

	public int getPort() {
		return port;
	}

}
