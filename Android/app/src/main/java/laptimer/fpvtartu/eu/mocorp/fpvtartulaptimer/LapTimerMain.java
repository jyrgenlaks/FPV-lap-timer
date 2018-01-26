package laptimer.fpvtartu.eu.mocorp.fpvtartulaptimer;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LapTimerMain extends AppCompatActivity{

	private ESPConnectionManager esp;
	private boolean running = true;

	private Button bThresholding, bReset, bNrMinus, bNrPlus;
	private TextView tvAircraftNumber, tvStatus, tvNrOfLaps, tvLapTimes;
	private EditText etSpeech;

	private TTSManager tts;
	private int currentAircraft = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lap_timer_main);

		tts = new TTSManager(getApplicationContext());
		esp = new ESPConnectionManager(getApplicationContext());

		tvAircraftNumber = (TextView) findViewById(R.id.tvAircraftNr);
		tvStatus= (TextView) findViewById(R.id.tvStatus);
		tvNrOfLaps = (TextView) findViewById(R.id.tvNrOfLaps);
		tvLapTimes = (TextView) findViewById(R.id.tvLaptimes);

		etSpeech = (EditText) findViewById(R.id.etSpeech);

		bNrMinus = (Button) findViewById(R.id.bNrMinus);
		bNrPlus = (Button) findViewById(R.id.bNrPlus);
		bReset = (Button) findViewById(R.id.bReset);
		bThresholding = (Button) findViewById(R.id.bThresholding);

		bNrMinus.setOnClickListener((v) -> currentAircraft--);
		bNrPlus.setOnClickListener((v) -> currentAircraft++);

		bReset.setOnClickListener(v -> {
            tts.speak("Test");
        });

		bThresholding.setOnClickListener(v -> {
            tts.speak("This is a longer sentence");
        });

		new Thread(){
			@Override
			public void run() {
				super.run();
				while(running){
					mainLoop();
					try {
						sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
			}
		}.start();
	}

	@SuppressLint("SetTextI18n")
	private void mainLoop(){
		if(!esp.isConnectedToWifi()){
			runOnUiThread(() -> tvStatus.setText("Not connected to the lap timer!\nConnecting to wifi ..."));
			esp.connectToWifi();
		}else{
			String data = esp.pollData();
			Log.d("LAPTIMER", "Polled data from ESP: " + data);

			if(data != null) {
				String rows[] = data.split("&");
				if(currentAircraft < 0) currentAircraft = rows.length-1;
				if(currentAircraft > rows.length-1) currentAircraft = 0;
				runOnUiThread(() -> tvAircraftNumber.setText("Current aircraft: " + currentAircraft));
				for (int i = 0; i < rows.length; i++) {
					String row = rows[i];
					String fields[] = row.split("\\|");
					if (i == currentAircraft) {
						runOnUiThread(() -> tvStatus.setText("Current aircraft data: \n" + row));
					}
				}
			}else{
				runOnUiThread(() -> tvStatus.setText("The returned data was null!"));
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		running = false;
		tts.onDestroy();
	}
}
