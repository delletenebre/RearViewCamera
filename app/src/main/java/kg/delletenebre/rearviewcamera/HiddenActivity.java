package kg.delletenebre.rearviewcamera;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class HiddenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = getIntent();
        if (intent != null && settings.getBoolean("pref_key_autodetect_usb_device", true)) {
            String action = intent.getAction();
            if (action != null && action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                int deviceClass = usbDevice.getDeviceClass();
                int deviceSubclass = usbDevice.getDeviceSubclass();

                if ( !MainActivity.isSerialDevice(usbDevice)
                        && deviceClass == 239 && deviceSubclass == 2) {
                    Intent usbDeviceIntent = new Intent(getApplicationContext(), MainActivity.class);
                    usbDeviceIntent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice);

                    startActivity(usbDeviceIntent);
                }
            }
        }

        finish();
    }
}

