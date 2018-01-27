package laptimer.fpvtartu.eu.mocorp.fpvtartulaptimer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Myrka on 27-Jan-18.
 */

public class LapLog {

	private List<List<LapData>> lapLogs = new ArrayList<>();

	public LapLog() {

	}

	public void parse(String data){
		if(data == null)
			return;

		String rows[] = data.trim().split("&");
		for (int aircraftNumber = 0; aircraftNumber < rows.length; aircraftNumber++) {
			String row = rows[aircraftNumber];
			String fields[] = row.split("\\|");
			if(fields.length == 4) {
				try {
					int lastLapTime = Integer.parseInt(fields[0]);
					int lastLapNumber = Integer.parseInt(fields[1]);
					int rawADC = Integer.parseInt(fields[2]);
					int thresholdValue = Integer.parseInt(fields[3]);

					while (lapLogs.size() <= aircraftNumber) {
						lapLogs.add(new ArrayList<>());
					}
					List<LapData> lapData = lapLogs.get(aircraftNumber);
					boolean isAnUpdate = true;
					for (LapData d : lapData) {
						if (d.getLapNumber() == lastLapNumber) {
							isAnUpdate = false;
						}
					}
					if (isAnUpdate) {
						//a log with this lap number does not exist, creating it
						lapLogs.get(aircraftNumber).add(new LapData(lastLapNumber, lastLapTime));
					}
				}catch (NumberFormatException e){
					e.printStackTrace();
				}
			}
		}
	}

	public String getLogs(){
		StringBuilder s = new StringBuilder();
		for(int i = 0; i < lapLogs.size(); i++) {
			List<LapData> list = lapLogs.get(i);

			s.append("Quadcopter #" + i + "\n");
			for(LapData data : list){
				s.append("\t\tLap " + data.getLapNumber() + ": " + data.getLapTime()/1000.0 + " seconds\n");
			}
		}
		return s.toString();
	}

	public class LapData{
		private int lapNumber, lapTime;

		public LapData(int lapNumber, int lapTime) {
			this.lapNumber = lapNumber;
			this.lapTime = lapTime;
		}

		public int getLapNumber() {
			return lapNumber;
		}

		public void setLapNumber(int lapNumber) {
			this.lapNumber = lapNumber;
		}

		public int getLapTime() {
			return lapTime;
		}

		public void setLapTime(int lapTime) {
			this.lapTime = lapTime;
		}
	}

}
