package server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beans.House;
import beans.Measurement;

public class Houses {

	private static Map<String, House> houses = new HashMap<String, House>();
	private static Map<String, ArrayList<Measurement>> measures = new HashMap<String, ArrayList<Measurement>>();

	static {
		measures.put("", new ArrayList<Measurement>());
	}

	public static boolean add(House house) {
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

	public static List<House> addAndGetList(House house) {
		if (house.getId().equals("")) return null;
		synchronized(houses) {
			synchronized(measures) {
				if (houses.containsKey(house.getId())) return null;
				houses.put(house.getId(), house);
				measures.put(house.getId(), new ArrayList<Measurement>());
				return new ArrayList<House>(houses.values());
			}
		}
	}

	public static boolean remove(String id) {
		if (id.equals("")) return false;
		synchronized(houses) {
			synchronized(measures) {
				measures.remove(id);
				return houses.remove(id) != null;
			}
		}
	}

	public static boolean exists(String id) {
		synchronized(houses) {
			return houses.containsKey(id);
		}
	}

	public static House get(String id) {
		synchronized(houses) {
			return houses.get(id);
		}
	}

	public static List<House> getList() {
		synchronized(houses) {
			return new ArrayList<House>(houses.values());
		}
	}

	public static int addMeasurements(List<Measurement> ms) {
		Collections.sort(ms);
		int count = 0;
		synchronized(measures) {
			String activeId = null;
			List<Measurement> activeList = measures.get(activeId);
			for (Measurement m: ms) {

				if (!m.getId().equals(activeId)) {
					activeId = m.getId();
					activeList = measures.get(activeId);
				}
				System.out.println(m +" "+activeList);
				if (activeList == null) continue;
				if (activeList.isEmpty() || m.compareTo(activeList.get(activeList.size()-1)) > 0) {
					activeList.add(m);
					count++;
				}
			}
		}
		return count;
	}

	public static List<Measurement> getMeasurements(String id, int n) {
		synchronized(measures) {
			if (!measures.containsKey(id)) return null;
			List<Measurement> list = measures.get(id);
			int size = list.size();
			return new ArrayList<Measurement>(list.subList(Math.max(0, size-n), size));
		}
	}

	public static List<Measurement> getCondoMeasurements(int n) {
		return getMeasurements("", n);
	}

	public static List<Measurement> getAllMeasurements() {
		synchronized(measures) {
			List<Measurement> list = new ArrayList<>();
			measures.values().forEach(list::addAll);
			return list;
		}
	}
}
