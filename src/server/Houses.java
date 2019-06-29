package server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beans.House;
import beans.Measurement;

public class Houses {
	
	public static final Houses instance = new Houses();
	
	private Map<String, House> houses = new HashMap<String, House>();
	
	private Map<String, List<Measurement>> measures = new HashMap<String, List<Measurement>>();

	private Houses() {
		measures.put("", new ArrayList<Measurement>());
	}
	
	public boolean add(House house) {
		if (house.getId().equals("")) return false;
		synchronized(houses) {
			synchronized(measures) {
				if (houses.containsKey(house.getId())) return false;
				houses.put(house.getId(), house);
				measures.put(house.getId(), new ArrayList<Measurement>());
				return true;
			}
		}
	}

	public House[] addAndGetList(House house) {
		if (house.getId().equals("")) return null;
		synchronized(houses) {
			synchronized(measures) {
				if (houses.containsKey(house.getId())) return null;
				houses.put(house.getId(), house);
				measures.put(house.getId(), new ArrayList<Measurement>());
				return (House[])houses.values().toArray();
			}
		}
	}	

	public boolean remove(String id) {
		if (id.equals("")) return false;
		synchronized(houses) {
			synchronized(measures) {
				measures.remove(id);
				return houses.remove(id) != null;
			}
		}
	}
	
	public boolean exists(String id) {
		synchronized(houses) {
			return houses.containsKey(id);
		}
	}
	
	public House get(String id) {
		synchronized(houses) {
			return houses.get(id);
		}
	}
	
	public House[] getList() {
		synchronized(houses) {
			return (House[])houses.values().toArray();
		}
	}
	
	public int addMeasurements(Measurement[] ms) {
		Arrays.sort(ms);
		int count = 0;
		synchronized(measures) {
			String activeId = "";
			List<Measurement> activeList = measures.get(activeId);
			for (Measurement m: ms) {
				if (!activeId.equals(m.getId())) {
					activeId = m.getId();
					activeList = measures.get(activeId);
				}
				if (activeList == null) continue;
				if (activeList.isEmpty() || m.compareTo(activeList.get(activeList.size()-1)) > 0) {
					activeList.add(m);
					count++;
				}
			}
		}
		return count;
	}
	
	public Measurement[] getMeasurements(String id, int n) {
		synchronized(measures) {
			if (!measures.containsKey(id)) return null;
			List<Measurement> list = measures.get(id);
			int size = list.size();
			return (Measurement[])list.subList(Math.max(0, size-10), size).toArray();
		}
	}
	
	public Measurement[] getCondoMeasurements(int n) {
		return getMeasurements("", n);
	}
}
