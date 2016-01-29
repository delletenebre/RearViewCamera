package kg.delletenebre.rearviewcamera;

import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import java.io.File;


public class EasycapSettings {
	
	private String TAG = getClass().getName();
    private boolean DEBUG;
	
	public final Pair<String, Integer> devType;
	public final Pair<String, Integer> devStandard;
	public final String devName;
	public final int frameWidth;
	public final int frameHeight;
	public int numBuffers;

	EasycapSettings(SharedPreferences sharedPrefs) {
        DEBUG = sharedPrefs.getBoolean("pref_key_debug", false);

		devName = checkDevices(sharedPrefs);

		boolean prefSetDevTypeManual = sharedPrefs.getBoolean("pref_key_manual_set_type", false);
		String prefDevice = (prefSetDevTypeManual)
							? sharedPrefs.getString("pref_select_easycap_type", "UTV007")
							: NativeEasyCapture.autoDetectDev(devName);

		numBuffers = 2;
        switch (prefDevice) {
            case "UTV007":
                devType = Pair.create("UTV007", 0);
                break;
            case "EMPIA":
                devType = Pair.create("EMPIA", 1);
                break;
            case "STK1160":
                devType = Pair.create("STK1160", 2);
                break;
            case "SOMAGIC":
                devType = Pair.create("SOMAGIC", 3);
                numBuffers = 4;
                break;
            default:
                devType = Pair.create("Default", 4);
        }
		
		String prefStandard = sharedPrefs.getString("pref_select_standard", "0");
		switch (Integer.valueOf(prefStandard)) {
            case 0:
                devStandard = Pair.create("NTSC", 0);
                // Empia devices like a frame width of 640 pixels
                frameWidth = (devType.second == 1) ? 640 : 720;
                // Somagic devices have 484 NTSC lines
                frameHeight = (devType.second == 3) ? 484 : 480;
                break;

            case 1:
                devStandard = Pair.create("PAL", 1);
                frameWidth = 720;
                frameHeight = 576;
                break;

            default:
                devStandard = Pair.create("NTSC", 0);
                frameWidth = 720;
                frameHeight = 480;
		}

		if (DEBUG) {
            Log.i(TAG, "Currently set device file: " + devName);
            Log.i(TAG, "Currently set device type: " + devType.first);
            Log.i(TAG, "Currently set tv standard: " + devStandard.first);
            Log.i(TAG, "Currently set frame width: " + frameWidth);
            Log.i(TAG, "Currently set frame height: " + frameHeight);
        }
	}	
	
    // iterate through the video devices and choose the first one
    public static String checkDevices(SharedPreferences settings) {
		String fName;

        if (settings.getBoolean("pref_key_manual_set_dev_loc", false)) {
            fName = settings.getString("pref_select_dev_loc", "/dev/video0");

            if (settings.getBoolean("pref_key_manual_set_dev_loc_interval", false)) {
                int i = fName.length() - 1;
                while (Character.isDigit(fName.charAt(i))) {
                    fName = fName.substring(0, i);
                    i--;
                }

                int min = Integer.parseInt(
                        settings.getString("pref_key_manual_set_dev_loc_interval_min", "0"));
                int max = Integer.parseInt(
                        settings.getString("pref_key_manual_set_dev_loc_interval_max", "3"));
                if (min < 0) {
                    min = 0;
                }
                if (max < 1) {
                    max = 1;
                }
                if (max < min) {
                    max = min + 1;
                }

                for (i = min; i <= max; i++) {
                    String tempName = fName + String.valueOf(i);

                    if (checkFileExists(tempName)) {
                        return tempName;
                    }
                }
            } else {
                return fName;
            }

        } else {
            String path[] = {
                    "/dev/ec_video",
                    "/dev/video",
                    "/dev/easycap"
            };

            for (String aPath : path) {
                for (int i = 0; i < 10; i++) {
                    fName = aPath + String.valueOf(i);

                    if (checkFileExists(fName)) {
                        return fName;
                    }
                }
            }
        }

        fName = "/dev/video0";
        return fName;
	}

    public static boolean checkFileExists(String path) {
        File file = new File(path);

        return file.exists() && file.canRead();
    }

}
