package kg.delletenebre.rearviewcamera;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class HiddenActivity extends Activity {
    private final String TAG = getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        if (settings.getBoolean("pref_key_autodetect_signal", true)
                && DetectSignalService.service == null) {
            startService(new Intent(this, DetectSignalService.class));
        }

        finish();
    }
}

