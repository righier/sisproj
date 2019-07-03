package beans;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IntegerCount {
	private final int count;
	
	@JsonbCreator
	public IntegerCount(@JsonbProperty("count") int count) {
		this.count = count;
	}
	
	public int getCount() {
		return count;
	}

}
