package kg.delletenebre.rearviewcamera;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class CommandsReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getName();

    private static final String RIM = "org.kangaroo.rim.action.ACTION_DATA_RECEIVE";

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean DEBUG = settings.getBoolean("pref_key_debug", false);
        String action = intent.getAction();

        if ( action.equals(RIM) && settings.getBoolean("pref_key_autodetect_rim", false) ) {
            String command = intent.getStringExtra("org.kangaroo.rim.device.EXTRA_COMMAND").toLowerCase();
            String args = intent.getStringExtra("org.kangaroo.rim.device.EXTRA_ARGS").toLowerCase();

            if (DEBUG) {
                Log.d(TAG, "EXTRA_COMMAND: " + command);
                Log.d(TAG, "EXTRA_ARGS: " + args);
            }

            if (command.equals(settings.getString("pref_key_autodetect_rim_command",
                            "transmissiongearposition").toLowerCase())) {
                Activity mainActivity = MainActivity.activity;

                String argsSetted = settings.getString("pref_key_autodetect_rim_args",
                        "reverse").toLowerCase();

                if (args.equals(argsSetted) && mainActivity == null) {
                    Intent mainIntent = new Intent(context.getApplicationContext(), MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.getApplicationContext().startActivity(mainIntent);

                } else if (!args.equals(argsSetted) && mainActivity != null ) {
                    mainActivity.finish();
                }
            }
        }
    }
}

