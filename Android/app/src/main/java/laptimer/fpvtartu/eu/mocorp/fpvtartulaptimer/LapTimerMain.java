package laptimer.fpvtartu.eu.mocorp.fpvtartulaptimer;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.R.attr.start;
import static android.os.Build.VERSION_CODES.M;

public class LapTimerMain extends AppCompatActivity implements TextToSpeech.OnInitListener {

	Button bTest, bWeb;
	TextView tvTest;

	private TextToSpeech tts;
	//private Button btnSpeak;
	private EditText txtText;
	WebHandler webHandler;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lap_timer_main);

		tvTest = (TextView) findViewById(R.id.tvStatus);
		txtText = (EditText) findViewById(R.id.etTest);
		bTest = (Button) findViewById(R.id.bTest);
		bTest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tvTest.setText(System.currentTimeMillis() + "");
				speakOut();
			}
		});

		bWeb = (Button) findViewById(R.id.bWeb);
		bWeb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				WifiManager wifiManager = (WifiManager) getSystemService (Context.WIFI_SERVICE);
				WifiInfo info = wifiManager.getConnectionInfo ();
				String ssid  = info.getSSID();

				Log.d("LapTimer", "ssid: " + ssid);
				Log.d("LapTimer", "info: " + info);

				if(!ssid.equals("\"Laptimer\"")){
					tvTest.setText("Connecting to wifi ...");
					connectToWifi();
					tvTest.setText("Connected to wifi");
				}else{
					if(webHandler.running){
						webHandler.running = false;
					}else{
						webHandler = new WebHandler();
						webHandler.execute();
					}
				}
			}
		});



		tts = new TextToSpeech(this, this);

		webHandler = new WebHandler();
		webHandler.execute();

	}

	@Override
	public void onDestroy() {
		// Don't forget to shutdown tts!
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}
		super.onDestroy();
	}

	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {

			int result = tts.setLanguage(Locale.US);

			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e("TTS", "This Language is not supported");
			} else {
				bTest.setEnabled(true);
				speakOut();
			}

		} else {
			Log.e("TTS", "Initilization Failed!");
		}
	}

	private void connectToWifi(){
		String networkSSID = "Laptimer";
		String networkPass = "FPV Tartu";

		WifiConfiguration conf = new WifiConfiguration();
		conf.SSID = "\"" + networkSSID + "\"";
		conf.preSharedKey = "\""+ networkPass +"\"";
		/*conf.wepKeys[0] = "\"" + networkPass + "\"";
		conf.wepTxKeyIndex = 0;
		conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);*/

		WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		wifiManager.addNetwork(conf);

		List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
		for( WifiConfiguration i : list ) {
			if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
				wifiManager.disconnect();
				wifiManager.enableNetwork(i.networkId, true);
				wifiManager.reconnect();
				break;
			}
		}
	}

	private void speakOut() {

		String text = txtText.getText().toString();

		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}

	public class WebHandler extends AsyncTask<Integer, String, Integer>{
		int failcounter = 0;
		boolean running = true;
		List<Integer> times = new ArrayList<>();

		@Override
		protected Integer doInBackground(Integer... params) {

			while(running) {
				long startTime = System.currentTimeMillis();
				HttpURLConnection urlConnection = null;

				try {
					URL url = new URL("http://192.168.4.1/get");
					urlConnection = (HttpURLConnection) url.openConnection();
					InputStream in = new BufferedInputStream(urlConnection.getInputStream());

					java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
					String out = s.hasNext() ? s.next() : "";

					long endTime = System.currentTimeMillis();
					long diff = endTime - startTime;
					times.add((int)diff);
					if(times.size() > 30){
						times.remove(0);
					}
					int min = times.get(0);
					int max = min;
					for(Integer i : times){
						if(i < min) min = i;
						if(i > max) max = i;
					}
					System.out.println("Got from web: " + out);
					publishProgress("Got from web: " + out + " (" + diff + " ms)\nMax: " + max + "\nMin: " + min + "\nsize: " + times.size() + "#" + times.get(0));
					in.close();

				} catch (IOException e) {
					e.printStackTrace();
					publishProgress("FAILED " + failcounter++);
				} finally {
					if (urlConnection != null) {
						urlConnection.disconnect();
					}

				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			tvTest.setText(values[0]);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			tvTest.setText("starting");
		}
	}

}
