package laptimer.fpvtartu.eu.mocorp.fpvtartulaptimer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Myrka on 27-Jan-18.
 */

public class LapLog {

	private List<List<LapData>> lapLogs = new ArrayList<>();
	private int correctedAircraftNumber, latestRawADC, latestThreshold;
	private double newLapTime;

	public LapLog() {

	}

	public boolean parse(String data, int currentAircraft){
		if(data == null)
			return false;

		String rows[] = data.trim().split("&");
		if(currentAircraft < -1) currentAircraft = rows.length-1;
		if(currentAircraft > rows.length-1) currentAircraft = -1;

		boolean isThereAnUpdateForTheCurrentAircraft = false;
		for (int aircraftNumber = 0; aircraftNumber < rows.length; aircraftNumber++) {
			String row = rows[aircraftNumber];
			String fields[] = row.split("\\|");
			if(fields.length == 4) {
				try {
					int lastLapTime = Integer.parseInt(fields[0]);
					int lastLapNumber = Integer.parseInt(fields[1]);
					int rawADC = Integer.parseInt(fields[2]);
					int thresholdValue = Integer.parseInt(fields[3]);

					if(currentAircraft == aircraftNumber){
						latestRawADC = rawADC;
						latestThreshold = thresholdValue;
					}

					if(lastLapTime > 0) {
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
							if (currentAircraft == -1 || currentAircraft == aircraftNumber) {
								//A new lap was recorded
								newLapTime = Math.round(lastLapTime / 10.0) / 100.0;
								isThereAnUpdateForTheCurrentAircraft = true;
							}
						}
					}
				}catch (NumberFormatException e){
					e.printStackTrace();
				}
			}
		}
		correctedAircraftNumber = currentAircraft;
		return isThereAnUpdateForTheCurrentAircraft;
	}

	public void reset(){
		lapLogs = new ArrayList<>();
	}

	public String getLogs(int aircraft){
		StringBuilder s = new StringBuilder();
		if(aircraft == -1) {
			s.append("All quadcopters:\n");
		}
		for(int i = 0; i < lapLogs.size(); i++) {
			List<LapData> list = lapLogs.get(i);

			if(aircraft == -1) {
				s.append("\tQuadcopter #" + i + "\n");
			}
			if(aircraft == -1 || aircraft == i) {
				for (LapData data : list) {
					if (aircraft == -1) {
						s.append("\t\t");
					}
					s.append("Lap " + data.getLapNumber() + ": " + data.getLapTime() / 1000.0 + " seconds\n");
				}
			}
		}
		return s.toString();
	}

	public int getCorrectedAircraftNumber() {
		return correctedAircraftNumber;
	}

	public double getNewLapTime() {
		return newLapTime;
	}

	public int getLatestRawADC() {
		return latestRawADC;
	}

	public int getLatestThreshold(){
		return latestThreshold;
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
