package kg.delletenebre.rearviewcamera;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class DetectSignalService extends Service {
    private final String TAG = getClass().getName();
    private boolean DEBUG = false;
    public static Service service;
    public static int NOTIFICATION_ID = 13;


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
    private NotificationManager mNotificationManager;

    @Override
    public  void onCreate() {
        super.onCreate();

        service = this;

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        DEBUG = settings.getBoolean("pref_key_debug", false);

        receiver = new CommandsReceiver();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, intentFilter);

        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED );

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                .setSmallIcon(getNotificationIcon())
                .setContentTitle(getString(R.string.notification_bar_title))
                .setContentText(getResources().getString(R.string.notification_bar_subtitle))
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOngoing(true);

        mNotificationManager.notify(NOTIFICATION_ID, notification.build());

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

        mNotificationManager.cancel(NOTIFICATION_ID);

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

    private int getNotificationIcon() {
//        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//                ? R.mipmap.ic_launcher
//                : R.mipmap.ic_launcher;
        return R.drawable.ic_notification_5;
    }
}
