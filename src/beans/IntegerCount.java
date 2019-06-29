package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IntegerCount {
	private final int count;
	
	public IntegerCount(int count) {
		this.count = count;
	}
	
	public int getCount() {
		return count;
	}

}
