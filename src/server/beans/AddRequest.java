package server.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AddRequest {
	public int id;
	public String ip;
	public int port;

	public AddRequest() {

	}

	public AddRequest(int id, String ip, int port) {
		this.id = id;
		this.ip = ip;
		this.port = port;
	}
}
