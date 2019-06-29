package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Measurement implements Comparable<Measurement> {
	private final String id;
	private final double value;
	private final long timestamp;

	public Measurement(String id, double value, long timestamp) {
		this.id = id;
		this.value = value;
		this.timestamp = timestamp;
	}
	
	public String getId() {
		return id;
	}

	public double getValue() {
		return value;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public int compareTo(Measurement o) {
		int res = id.compareTo(o.id);
		if (res != 0) return res;
		return Long.compare(timestamp, o.timestamp);
	}
}
