package house;

import java.util.Arrays;

import simulator.Buffer;
import simulator.Measurement;

public class SmartMeterBuffer implements Buffer {
	
	private final int size = 24;
	private final int overlap = 12;
	private double[] data = new double[size];
	private int missing = size;
	private int next = 0;
	
	@Override
	public synchronized void addMeasurement(Measurement m) {
		
		data[next] = m.getValue();
		next = (next + 1) % size;
		if (--missing == 0) {
			double avg = Arrays.stream(data).sum() / size;
			long timestamp = m.getTimestamp();
			System.out.println(avg+" "+timestamp);
			missing = overlap;
		}
	}

}
