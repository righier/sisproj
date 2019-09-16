package beans;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class House implements Comparable<House> {
	private final String id;
	private final String addr;
	private final int port;

	@JsonbCreator
	public House(@JsonbProperty("id") String id, @JsonbProperty("addr") String addr, @JsonbProperty("port") int port) {
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

	@Override
	public int compareTo(House h) {
		return this.id.compareTo(h.id);
	}

	@Override
	public String toString() {
		return id+" "+addr+":"+port;
	}
}
