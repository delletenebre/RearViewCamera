package kg.delletenebre.rearviewcamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;

public class NativeEasyCapture implements EasyCapture {

	
    private final String TAG = getClass().getName();
    private boolean DEBUG = false;
    private EasycapSettings deviceSets;
    boolean deviceConnected = false;

    private native int startDevice(String cacheDir, String deviceName,
                                   int width, int height, int devType, int regionStd, int numBufs);
    private native void getNextFrame(Surface mySurface);
    private native boolean isDeviceAttached();
    private native void stopDevice();
    private static native String detectDevice(String deviceName);
    

    static {
        System.loadLibrary("easycapture");
    }

    public NativeEasyCapture(SharedPreferences sharedPrefs, Context context) {
        DEBUG = sharedPrefs.getBoolean("pref_key_debug", false);

    	deviceSets = new EasycapSettings(sharedPrefs);

        if (DEBUG) {
            Toast.makeText(context, "Device set as " + deviceSets.devType.first
                    + " at " + deviceSets.devName, Toast.LENGTH_SHORT).show();
        }

        connect(context);
    }

    private void connect(Context context) {
        boolean deviceReady = true;

        File deviceFile = new File(deviceSets.devName);
        if (deviceFile.exists()) {
            if (!deviceFile.canRead()) {
                if (DEBUG) {
                    Log.d(TAG, "Insufficient permissions on " + deviceSets.devName +
                            " -- does the app have the CAMERA permission?");
                }
                deviceReady = false;
            }
        } else {
            if (DEBUG) {
                Log.w(TAG, deviceSets.devName + " does not exist");
            }

            deviceReady = false;
        }

        if (deviceReady) {
            if (DEBUG) {
                Log.i(TAG, "Preparing camera with device name " + deviceSets.devName);
            }

            deviceConnected = (-1 != startDevice(context.getCacheDir().toString(), deviceSets.devName,
                    deviceSets.frameWidth, deviceSets.frameHeight, deviceSets.devType.second,
                    deviceSets.devStandard.second, deviceSets.numBuffers));

        }
    }

    public void getFrame(Surface mySurface) {
        getNextFrame(mySurface);
    }

    public EasycapSettings getSettings() {
        return deviceSets;
    }

    public void stop() {
        stopDevice();
    }

    public boolean isAttached() {
        return isDeviceAttached();
    }

    public boolean isDeviceConnected() {return deviceConnected;}
    
    static public String autoDetectDev(String dName) {
    	return detectDevice(dName);
    }
}
