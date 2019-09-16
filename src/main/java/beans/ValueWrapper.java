package beans;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ValueWrapper {
	private double value;
	
	@JsonbCreator
	public ValueWrapper(@JsonbProperty("value") double value) {
		this.value = value;
	}
	
	public double getValue() {
		return value;
	}
}
