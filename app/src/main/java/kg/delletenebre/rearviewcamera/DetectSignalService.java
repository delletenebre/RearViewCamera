package kg.delletenebre.rearviewcamera;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class DetectSignalService extends Service {
    private final String TAG = getClass().getName();
    private boolean DEBUG = false;
    public static Service service;

    private SharedPreferences settings;

    private CommandsReceiver receiver;
    private Handler mHandler;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            checkEasycapSignal();
        }
    };

    private boolean deviceDetected;

    @Override
    public  void onCreate() {
        super.onCreate();

        service = this;

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        DEBUG = settings.getBoolean("pref_key_debug", false);

        receiver = new CommandsReceiver();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, intentFilter);

        mHandler = new Handler();
        deviceDetected = false;

        if (DEBUG) {
            Log.d(TAG, "**** Detect Signal Service CREATED ****");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (receiver != null) {
            unregisterReceiver(receiver);
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
            mHandler = null;
        }

        service = null;

        if (DEBUG) {
            Log.d(TAG, "**** Detect Signal Service DESTROYED ****");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkEasycapSignal();

        return START_STICKY;
    }

    private void checkEasycapSignal() {
        if (DEBUG) {
            Log.d(TAG, "Checking signal");
        }

        String deviceName = EasycapSettings.checkDevices(settings);
        deviceDetected = EasycapSettings.checkFileExists(deviceName);

        if (deviceDetected) {
            if (DEBUG) {
                Log.d(TAG, "Find device: " + deviceName);
            }

            if (MainActivity.activity == null && SettingsActivity.activity == null) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
            }

        } else {
            if (DEBUG) {
                Log.d(TAG, "Device NOT connected");
            }

            if (MainActivity.activity != null
                    && settings.getBoolean("pref_key_autodetect_usb_device", true)) {
                MainActivity.activity.finish();
            }
        }

        next();
    }

    private void next() {
        if (mHandler != null && settings.getBoolean("pref_key_autodetect_usb_device", true)) {
            int delay = Integer.parseInt(
                    settings.getString("pref_key_autodetect_usb_device_interval", "1000"));
            if (delay < 100) {
                delay = 100;
            }
            mHandler.postDelayed(mRunnable, delay);
        } else {
            stopSelf();
        }
    }
}
