package kg.delletenebre.rearviewcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
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

import kg.delletenebre.rearviewcamera.UVC.UVCCameraView;

public class MainActivity extends Activity {
    private final String TAG = getClass().getName();
    private static boolean DEBUG = false;

    public static Activity activity;
    private static SharedPreferences settings;

    private EasycamView cameraView;
    private FrameLayout mainLayout;

    private AudioManager mAudioManager;

    private boolean pref_isMute;


    // ---- UVC ---- //
    // for thread pool
    private static final int CORE_POOL_SIZE = 1;		// initial/minimum threads
    private static final int MAX_POOL_SIZE = 4;			// maximum threads
    private static final int KEEP_ALIVE_TIME = 10;		// time periods while keep the idle thread
    protected static final ThreadPoolExecutor EXECUTER
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());


    // for accessing USB and USB camera
    private int[] mUVCResolution = new int[2];
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private UVCCameraView cameraViewUVC;
    // for open&start / stop&close camera preview
    private Surface mPreviewSurface;

    private UsbDevice mUsbDevice;
    private String USBDeviceName = "";
    // ---- END UVC ---- //





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        DEBUG = settings.getBoolean("pref_key_debug", false);

        cameraView = (EasycamView) findViewById(R.id.camera_view);
        cameraViewUVC = (UVCCameraView) findViewById(R.id.camera_view_uvc);
        cameraViewUVC.setSurfaceTextureListener(mSurfaceTextureListener);
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        if (isUVCMode()) {
            reconnectUVC(true);
        }

        mainLayout = (FrameLayout) findViewById(R.id.layout_main);
        mainLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onClick() {
                super.onClick();

                if (isUVCMode()) {
                    if (mUVCCamera != null) {
                        mUVCCamera.destroy();
                        mUVCCamera = null;
                    }

                    reconnectUVC(false);
                } else if (cameraView != null) {
                    cameraView.restart();
                }
            }

            @Override
            public void onLongClick() {
                super.onLongClick();

                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });


        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();

        pref_isMute = settings.getBoolean("pref_key_mute", true);

        setMute(true);
        activity = this;

        String[] mUVCResolutionString = settings.getString("pref_key_uvc_resolution",
                getString(R.string.pref_default_uvc_resolution)).split("x");
        for (int i = 0; i < mUVCResolution.length; i++) {
            mUVCResolution[i] = Integer.parseInt(mUVCResolutionString[i]);
        }
        boolean mirrored = settings.getBoolean("pref_key_mirrored", false);
        boolean isUVCMode = isUVCMode();


        if (mUSBMonitor == null) {
            mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        } else if (!mUSBMonitor.isRegistered()) {
            mUSBMonitor.register();
        }


        if (cameraView != null) {
            cameraView.setScaleX(mirrored ? -1 : 1);
            cameraView.setVisibility(!isUVCMode ? View.VISIBLE : View.INVISIBLE);
        }
        if (cameraViewUVC != null) {
            DisplayMetrics metrics = this.getResources().getDisplayMetrics();

            cameraViewUVC.setAspectRatio(
                ( settings.getBoolean("pref_key_keep_aspect_ratio", true) && mUVCResolution != null )
                    ? mUVCResolution[0] / (float) mUVCResolution[1]
                    : metrics.widthPixels / (float) metrics.heightPixels);
            cameraViewUVC.setScaleX(mirrored ? -1 : 1);
            cameraViewUVC.setVisibility(isUVCMode ? View.VISIBLE : View.INVISIBLE);
        }

        USBDeviceName = settings.getString("uvc_usb_device_name", "");
    }

    @Override
    public void onPause() {
        setMute(false);
        activity = null;
        if (mUVCCamera != null) {
            mUVCCamera.stopPreview();
        }

        if (mUSBMonitor != null && mUSBMonitor.isRegistered()) {
            mUSBMonitor.unregister();
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
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        (state) ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE,
                        0);
            } else {
                mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, state);
            }
        }
    }

    public static boolean isUVCMode() {
        String easycapType = "UTV007";
        boolean isManualType = false;
        if (settings != null) {
            easycapType = settings.getString("pref_select_easycap_type", "UTV007");
            isManualType = settings.getBoolean("pref_key_manual_set_type", false);
        }

        return isManualType && easycapType.equals("UVC");
    }

    private void reconnectUVC(boolean setDefaultDevice) {
        if (mUVCCamera != null) {
            mUVCCamera.destroy();
            mUVCCamera = null;
        }

        Iterator<UsbDevice> deviceIterator = mUSBMonitor.getDevices();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();

            if (USBDeviceName.isEmpty()) { //|| device.getDeviceName().equals(USBDeviceName)) {
                if (setDefaultDevice) {
                    mUsbDevice = device;
                }

                mUSBMonitor.requestPermission(device);
                break;
            }
        }
    }


    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
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
                    //if (DEBUG) Log.i(TAG, "supportedSize:" + mUVCCamera.getSupportedSize());
                    if (mPreviewSurface != null) {
                        mPreviewSurface.release();
                        mPreviewSurface = null;
                    }
                    try {
                        mUVCCamera.setPreviewSize(mUVCResolution[0], mUVCResolution[1],
                                UVCCamera.FRAME_FORMAT_MJPEG);
                    } catch (final IllegalArgumentException e) {
                        try {
                            // fallback to YUV mode
                            mUVCCamera.setPreviewSize(mUVCResolution[0], mUVCResolution[1],
                                    UVCCamera.DEFAULT_PREVIEW_MODE);
                        } catch (final IllegalArgumentException e1) {
                            mUVCCamera.destroy();
                            mUVCCamera = null;
                        }
                    }
                    if (mUVCCamera != null) {
                        final SurfaceTexture st = cameraViewUVC.getSurfaceTexture();
                        if (st != null) {
                            mPreviewSurface = new Surface(st);
                        }
                        mUVCCamera.setPreviewDisplay(mPreviewSurface);
                        mUVCCamera.startPreview();
                    }
                }
            });
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            // need to check whether the comming device equal to camera device that currently using
            if (mUVCCamera != null) {
                mUVCCamera.close();
                if (mPreviewSurface != null) {
                    mPreviewSurface.release();
                    mPreviewSurface = null;
                }
            }
        }

        @Override
        public void onDettach(final UsbDevice device) {
            if (DEBUG) {
                Log.d(TAG, "USB device detached: " + device.getDeviceName());
            }
            if (settings.getBoolean("pref_key_autodetect_usb_device", true)) {
                finish();
            }
        }

        @Override
        public void onCancel() { }
    };


    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
            if (mUVCCamera != null) {
                mUVCCamera.destroy();
                mUVCCamera = null;
            }

//            if (mUsbDevice != null && mUSBMonitor != null) {
//                mUSBMonitor.requestPermission(mUsbDevice);
//            }
            reconnectUVC(false);
        }

        @Override
        public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width, final int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(final SurfaceTexture surface) {

        }
    };


}
