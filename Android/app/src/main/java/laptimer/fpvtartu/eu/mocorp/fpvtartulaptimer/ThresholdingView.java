package laptimer.fpvtartu.eu.mocorp.fpvtartulaptimer;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Myrka on 27-Jan-18.
 */

public class ThresholdingView extends DialogFragment{

	private LinearLayout ll;
	private ProgressBar pb;
	private SeekBar sb;
	private TextView tv;
	private boolean running = true;

	private int threshold, rawADC;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		ll = new LinearLayout(getActivity());
		ll.setOrientation(LinearLayout.VERTICAL);

		tv = new TextView(getActivity());
		tv.setGravity(Gravity.CENTER_HORIZONTAL);

		pb = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleHorizontal);
		pb.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		pb.setPadding(50, 50, 50, 50);
		pb.setMax(4096);

		sb = new SeekBar(getActivity());
		sb.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		sb.setMax(4096);
		sb.setProgress(threshold);

		ll.addView(tv);
		ll.addView(pb);
		ll.addView(sb);
		builder.setView(ll);

		builder.setMessage("Choose the threshold value")
				.setPositiveButton("Save", (dialog, id) -> {
					running = false;
					Intent intent = new Intent("THRESHOLD_UPDATE");
					intent.putExtra("THRESHOLD", sb.getProgress());
					LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

				})
				.setNegativeButton("Cancel", (dialog, id) -> {
					running = false;
					this.dismiss();

				});
		// Create the AlertDialog object and return it
		new Thread(){
			@Override
			public void run() {
				super.run();
				while(true){

					if(getActivity() != null) {
						getActivity().runOnUiThread(() -> updateUI());
					}
					try {
						sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		updateUI();
		return builder.create();
	}

	private void updateUI(){
		threshold = sb.getProgress();
		pb.setProgress(rawADC);
		tv.setText("Threshold: " + threshold + "\t\tRAW ADC: " + rawADC);
	}

	public void setRaw(int raw){
		rawADC = raw;
	}

	public void setThreshold(int newThreshold){
		this.threshold = newThreshold;
	}

}
