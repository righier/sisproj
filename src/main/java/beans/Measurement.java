package beans;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.xml.bind.annotation.XmlRootElement;

import proto.HouseProto.Measure;

@XmlRootElement
public class Measurement implements Comparable<Measurement> {
	private String id;
	private double value;
	private long timestamp;

	public Measurement(Measure m) {
		this(m.getId(), m.getValue(), m.getTimestamp());
	}

	@JsonbCreator
	public Measurement(@JsonbProperty("id") String id, @JsonbProperty("value") double value, @JsonbProperty("timestamp") long timestamp) {
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
		if (res != 0)
			return res;
		return Long.compare(timestamp, o.timestamp);
	}
	
	@Override
	public String toString() {
		return "id "+id+", value "+value+", timestamp "+timestamp;
	}
	
	public Measure toProtobuf() {
		return Measure.newBuilder()
				.setId(id)
				.setValue(value)
				.setTimestamp(timestamp)
				.build();
	}
}
