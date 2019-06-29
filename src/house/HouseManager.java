package house;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beans.Measurement;

public class HouseManager {
	
	private Map<String, HouseConnection> connections = new HashMap<String, HouseConnection>();
	private Map<String, List<Measurement>> measures = new HashMap<String, List<Measurement>>();
	
	private String localId;
	
	public HouseManager(String localId) {
		this.localId = localId;
		
		measures.put(localId, new ArrayList<Measurement>());
		
	}
	
	public void add(HouseConnection house) {
		connections.put(house.getId(), house);
		measures.put(house.getId(), new ArrayList<Measurement>());
	}
	
	public void remove(String id) {
		connections.remove(id);
		measures.remove(id);
	}
}
