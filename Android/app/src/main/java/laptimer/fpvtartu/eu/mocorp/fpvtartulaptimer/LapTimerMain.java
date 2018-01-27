package laptimer.fpvtartu.eu.mocorp.fpvtartulaptimer;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

public class LapTimerMain extends AppCompatActivity{

	private ESPConnectionManager esp;
	private boolean running = true;
	private LapLog lapLog;

	private Button bThresholding, bReset, bNrMinus, bNrPlus;
	private TextView tvAircraftNumber, tvStatus, tvNrOfLaps, tvLapTimes;
	private EditText etSpeech;

	private TTSManager tts;
	private int currentAircraft = 0;

	private ThresholdingView thresholdingView;
	private BroadcastReceiver localBroadcastReceiver;

	//tasks
	boolean shouldReset = false;
	boolean shouldThreshold = false;
	int newThreshold;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lap_timer_main);

		thresholdingView = new ThresholdingView();
		localBroadcastReceiver = new LocalBroadcastReceiver();

		tts = new TTSManager(getApplicationContext());
		esp = new ESPConnectionManager(getApplicationContext());
		lapLog = new LapLog();

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
			bReset.setEnabled(false);
			shouldReset = true;
        });

		bThresholding.setOnClickListener(v -> {
            //esp.setThreshold(2, 1234);
            //tts.speak("This is a longer sentence");
			FragmentManager fm = getFragmentManager();
			thresholdingView.setThreshold(lapLog.getLatestThreshold());
			thresholdingView.show(fm, "some txt");
        });

		new Thread(){
			@Override
			public void run() {
				super.run();
				while(running){
					try {
						mainLoop();
						sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
			}
		}.start();
	}

	@SuppressLint("SetTextI18n")
	private void mainLoop() throws InterruptedException {

		if(!esp.isConnectedToWifi()){
			runOnUiThread(() -> tvStatus.setText("Not connected to the lap timer!\nConnecting to wifi ..." + System.currentTimeMillis()%1000000));
			esp.connectToWifi();
			Thread.sleep(3000);
		}else{
			if(shouldReset){
				esp.resetCounters();
				lapLog.reset();
				shouldReset = false;
				runOnUiThread(() -> bReset.setEnabled(true));
			}
			if(shouldThreshold){
				esp.setThreshold(currentAircraft, newThreshold);
				shouldThreshold = false;
				runOnUiThread(() -> bThresholding.setEnabled(true));
			}

			String data = esp.pollData();
			//Log.d("LAPTIMER", "Polled data from ESP: " + data);

			if(data != null) {
				boolean lapFinished = lapLog.parse(data, currentAircraft);
				currentAircraft = lapLog.getCorrectedAircraftNumber();
				thresholdingView.setRaw(lapLog.getLatestRawADC());

				runOnUiThread(() -> tvLapTimes.setText(lapLog.getLogs(currentAircraft)));
				runOnUiThread(() -> tvAircraftNumber.setText("Current aircraft: " + (currentAircraft==-1?"ALL":currentAircraft)));

				if(lapFinished){
					tts.speak(etSpeech.getText().toString().replace("%", ""+lapLog.getNewLapTime()));
				}

				String rows[] = data.split("&");
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

	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(this).registerReceiver(localBroadcastReceiver, new IntentFilter("THRESHOLD_UPDATE"));
	}

	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(localBroadcastReceiver);
	}

	private class LocalBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// safety check
			if (intent == null || intent.getAction() == null) {
				return;
			}

			if (intent.getAction().equals("THRESHOLD_UPDATE")) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					bThresholding.setEnabled(false);
					String state = extras.getString(TelephonyManager.EXTRA_STATE);
					newThreshold = extras.getInt("THRESHOLD");
					Log.d("LAPTIMER", "Threshold val: " + newThreshold);
					shouldThreshold = true;
				}
			}
		}
	}
}
