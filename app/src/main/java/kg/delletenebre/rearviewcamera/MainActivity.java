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

import kg.delletenebre.rearviewcamera.UVC.UVCCameraView;

public class MainActivity extends Activity {
    private final String TAG = getClass().getName();
    private static boolean DEBUG = false;

    public static Activity activity;
    private static SharedPreferences settings;

    private EasycamView cameraView;

    private AudioManager mAudioManager;

    private boolean pref_isMute;

    private static int currentVolume, maximumVolume;
    private static final int VOLUME_STREAM = AudioManager.STREAM_MUSIC;


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
    // ---- END UVC ---- //

    private int __volumeSteps, __volumeDelay;
    private boolean __volumeShowUI;
    private Handler __volumeHandler;
    private Runnable __volumeRunnable;




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

                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });


        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        currentVolume = mAudioManager.getStreamVolume(VOLUME_STREAM);
        maximumVolume = mAudioManager.getStreamMaxVolume(VOLUME_STREAM);
    }

    @Override
    public void onResume() {
        super.onResume();

        mUSBMonitor.register();

        pref_isMute = settings.getBoolean("pref_key_mute", true);

        currentVolume = mAudioManager.getStreamVolume(VOLUME_STREAM);
        setMute(true);
        activity = this;

        String[] mUVCResolutionString = settings.getString("pref_key_uvc_resolution",
                getString(R.string.pref_default_uvc_resolution)).split("x");
        for (int i = 0; i < mUVCResolution.length; i++) {
            mUVCResolution[i] = Integer.parseInt(mUVCResolutionString[i]);
        }
        boolean mirrored = settings.getBoolean("pref_key_mirrored", false);
        boolean isUVCMode = isUVCMode();


//        if (mUSBMonitor == null) {
//            mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
//        } else if (!mUSBMonitor.isRegistered()) {
//            mUSBMonitor.register();
//        }


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
    }

    @Override
    public void onPause() {
        setMute(false);
        activity = null;
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
            if (settings.getBoolean("pref_key_mute_ease", true)) {
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            while (__volumeSteps > 0 || !Thread.currentThread().isInterrupted()) {
                                TimeUnit.MILLISECONDS.sleep(__volumeDelay);
                                __volumeHandler.post(__volumeRunnable);
                                __volumeSteps--;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                __volumeHandler = new Handler();
                int steps;
                __volumeShowUI = DEBUG;

                if (state) {
                    int vol = Math.round(maximumVolume * 0.15f);
                    steps = currentVolume - vol;
                    __volumeRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mAudioManager.setStreamVolume(VOLUME_STREAM,
                                    mAudioManager.getStreamVolume(VOLUME_STREAM) - 1,
                                    __volumeShowUI ? AudioManager.FLAG_SHOW_UI : 0);
                        }
                    };
                } else {
                    steps = currentVolume - mAudioManager.getStreamVolume(VOLUME_STREAM);
                    __volumeRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mAudioManager.setStreamVolume(VOLUME_STREAM,
                                    mAudioManager.getStreamVolume(VOLUME_STREAM) + 1,
                                    __volumeShowUI ? AudioManager.FLAG_SHOW_UI : 0);
                        }
                    };
                }

                if (steps > 0) {
                    __volumeSteps = steps;
                    __volumeDelay = 750 / steps;

                    t.interrupt();
                    t.start();
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mAudioManager.adjustStreamVolume(VOLUME_STREAM,
                            (state) ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE,
                            0);
                } else {
                    mAudioManager.setStreamMute(VOLUME_STREAM, state);
                }
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

    private void reconnectUVC() {
        Iterator<UsbDevice> deviceIterator = mUSBMonitor.getDevices();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();

            if (DEBUG) {
                Log.d(TAG, "--------------------");
                Log.d(TAG, String.valueOf(device.getDeviceClass()));
                Log.d(TAG, String.valueOf(device.getDeviceSubclass()));
                Log.d(TAG, device.getDeviceName());
                Log.d(TAG, String.valueOf(device.getProductId()));
                Log.d(TAG, String.valueOf(device.getVendorId()));
                Log.d(TAG, "--------------------");
            }

            if ( (device.getProductId() == 22608 && device.getVendorId() == 6380) ||
                 (device.getDeviceClass() == 239 && device.getDeviceSubclass() == 2)) {

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
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock){
            if (mUVCCamera != null && mUVCCamera.getDevice().equals(device)) {
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


    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(final SurfaceTexture surface,
                                              final int width, final int height) {
            reconnectUVC();
        }

        @Override
        public void onSurfaceTextureSizeChanged(final SurfaceTexture surface,
                                                final int width, final int height) {
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
