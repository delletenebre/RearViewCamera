package kg.delletenebre.rearviewcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kg.delletenebre.rearviewcamera.GuideLines.GuideLinesLayout;
import kg.delletenebre.rearviewcamera.GuideLines.GuideLinesView;
import kg.delletenebre.rearviewcamera.UVC.UVCCameraView;

public class MainActivity extends Activity {
    private final String TAG = getClass().getName();
    private static boolean DEBUG = false;

    public static Activity activity;
    private static SharedPreferences settings;

    private EasycamView cameraView;

    private AudioManager mAudioManager;

    private boolean pref_isMute;

    private static final int VOLUME_STREAM = AudioManager.STREAM_MUSIC;


    // ---- UVC ---- //
    // for thread pool
    private static final int CORE_POOL_SIZE = 1;	// initial/minimum threads
    private static final int MAX_POOL_SIZE = 4;		// maximum threads
    private static final int KEEP_ALIVE_TIME = 10;	// time periods while keep the idle thread
    protected static final ThreadPoolExecutor EXECUTER
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());


    // for accessing USB and USB camera
    private static USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private UVCCameraView cameraViewUVC;
    private UsbDevice connectedUsdDevice;
    // ---- END UVC ---- //

    GuideLinesLayout guideLinesLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        DEBUG = settings.getBoolean("pref_key_debug", false);

        cameraView = (EasycamView) findViewById(R.id.camera_view);
        cameraViewUVC = (UVCCameraView) findViewById(R.id.camera_view_uvc);
        guideLinesLayout = (GuideLinesLayout) findViewById(R.id.guide_lines);
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);

        FrameLayout mainLayout = (FrameLayout) findViewById(R.id.layout_main);
        mainLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onClick() {
                super.onClick();

                if (isUVCMode()) {
                    reconnectUVC();

                } else if (cameraView != null) {
                    cameraView.restart();
                }
            }

            @Override
            public void onLongClick() {
                super.onLongClick();

                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });


        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);



//        Intent intent = getIntent();
//        if (intent != null) {
//            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//            if (usbDevice != null) {
//                mUSBMonitor.requestPermission(usbDevice);
//            }
//        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mUSBMonitor.register();

        pref_isMute = settings.getBoolean("pref_key_mute", true);

        setMute(true);
        activity = this;


        boolean mirrored = settings.getBoolean("pref_key_mirrored", false);
        boolean isUVCMode = isUVCMode();

        if (cameraView != null) {
            cameraView.setScaleX(mirrored ? -1 : 1);
            cameraView.setVisibility(!isUVCMode ? View.VISIBLE : View.INVISIBLE);
        }
        if (cameraViewUVC != null) {
            cameraViewUVC.setScaleX(mirrored ? -1 : 1);
            cameraViewUVC.setVisibility(isUVCMode ? View.VISIBLE : View.INVISIBLE);
        }

    }

    @Override
    public void onPause() {
        setMute(false);
        activity = null;

        mUSBMonitor.unregister();

        if (mUVCCamera != null) {
            mUVCCamera.stopPreview();
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mUVCCamera != null) {
            mUVCCamera.destroy();
            mUVCCamera = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }

        cameraView = null;
        cameraViewUVC = null;
        connectedUsdDevice = null;

        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (settings.getBoolean("pref_key_fullscreen", false)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                getWindow().getDecorView()
                        .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                getWindow().getDecorView()
                        .setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }

    }

    private void setMute(boolean state) {
        if (pref_isMute) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mAudioManager.adjustStreamVolume(VOLUME_STREAM,
                        (state) ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE,
                        0);
            } else {
                mAudioManager.setStreamMute(VOLUME_STREAM, state);
            }
        }
    }

    public static boolean isUVCMode() {
        String easycapType = "";
        boolean isManualType = false;
        if (settings != null) {
            easycapType = settings.getString("pref_select_easycap_type", "");
            isManualType = settings.getBoolean("pref_key_manual_set_type", false);
        }

        return isManualType && easycapType.equals("UVC");
    }

    public static void reconnectUVC() {
        Iterator<UsbDevice> deviceIterator = mUSBMonitor.getDevices();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();

            if (!isSerialDevice(device)
                    && device.getDeviceClass() == 239 && device.getDeviceSubclass() == 2) {
                mUSBMonitor.requestPermission(device);
                break;
            }
        }
    }


    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener =
            new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            if (DEBUG) {
                Log.d(TAG, "USB device attached: " + device.getDeviceName());
            }
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock,
                              final boolean createNew) {
            if (mUVCCamera != null) {
                mUVCCamera.destroy();
            }
            mUVCCamera = new UVCCamera();
            EXECUTER.execute(new Runnable() {
                @Override
                public void run() {
                    mUVCCamera.open(ctrlBlock);
                    UVCCameraView.releasePreviewSurface();

                    int[] resolution = UVCCameraView.getResolution();
                    try {
                        mUVCCamera.setPreviewSize(resolution[0], resolution[1],
                                UVCCamera.FRAME_FORMAT_MJPEG);
                    } catch (final IllegalArgumentException e) {
                        try {
                            // fallback to YUV mode
                            mUVCCamera.setPreviewSize(resolution[0], resolution[1],
                                    UVCCamera.DEFAULT_PREVIEW_MODE);
                        } catch (final IllegalArgumentException e1) {
                            mUVCCamera.destroy();
                            mUVCCamera = null;
                        }
                    }
                    if (mUVCCamera != null) {
                        mUVCCamera.setPreviewDisplay(cameraViewUVC.initPreviewSurface());
                        mUVCCamera.startPreview();

                        connectedUsdDevice = mUVCCamera.getDevice();
                    }
                }
            });
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock){
            if (mUVCCamera != null && mUVCCamera.getDevice().equals(device)) {
                mUVCCamera.close();

                UVCCameraView.releasePreviewSurface();
            }
        }

        @Override
        public void onDettach(final UsbDevice detachedDevice) {
            if (DEBUG) {
                Log.d(TAG, "USB device detached: " + detachedDevice.getDeviceName());
            }
            if (settings.getBoolean("pref_key_autodetect_usb_device", true)
                    && connectedUsdDevice != null
                    && connectedUsdDevice.equals(detachedDevice)) {
                finish();
            }

        }

        @Override
        public void onCancel() {}
    };

    public static boolean isSerialDevice(UsbDevice usbDevice) {
        int pid = usbDevice.getProductId();
        int vid = usbDevice.getVendorId();

        return     (vid == 65535 && (pid == 17 || pid == 18))
                || (vid == 1027 && (pid == 24577 || pid == 24597))
                || (vid == 9025)
                || (vid == 5824 && pid == 1155)
                || (vid == 4292 && pid == 60000)
                || (vid == 1659 && pid == 8963)
                || (vid == 6790 && pid == 29987);
    }

}
