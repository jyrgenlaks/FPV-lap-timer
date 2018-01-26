package laptimer.fpvtartu.eu.mocorp.fpvtartulaptimer;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by Myrka on 26-Jan-18.
 */

public class ESPConnectionManager {

    private static final String ESP_ADDRESS = "http://192.168.4.1";
    private static final String networkSSID = "Laptimer";
    private static final String networkPass = "FPV Tartu";
    private Context context;


    public ESPConnectionManager(Context context) {
        this.context = context;
    }

    public void connectToWifi(){
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";
        conf.preSharedKey = "\""+ networkPass +"\"";

        WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
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
            while(!isConnectedToWifi()){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }else{
            Log.e("LAPTIMER", "Error: connectToWifi: WifiManager is null");
        }
    }

    public boolean isConnectedToWifi() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifiManager != null && wifiManager.getConnectionInfo().getSSID().equals("\"" + networkSSID + "\"");
    }

    public String pollData(){
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(ESP_ADDRESS + "/get");
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            //read the stream to string
            java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
            String out = s.hasNext() ? s.next() : "";
            //close connection and return
            in.close();
            urlConnection.disconnect();
            return out;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
