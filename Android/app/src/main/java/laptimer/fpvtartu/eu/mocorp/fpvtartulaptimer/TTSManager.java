package laptimer.fpvtartu.eu.mocorp.fpvtartulaptimer;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Created by Myrka on 26-Jan-18.
 */

public class TTSManager implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;

    public TTSManager(Context context) {
        tts = new TextToSpeech(context, this);
        Log.d("LAPTIMER", "construktoris");
    }

    @Override
    public void onInit(int status) {
        Log.d("LAPTIMER", "OnInitis");
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }else{
                speak("TTS is ready!");
            }
            //TODO check for errors and handle them correctly
        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    public void onDestroy(){
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    public void speak(String text){
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
}
