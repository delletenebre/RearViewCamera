package kg.delletenebre.rearviewcamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.HashMap;

public class EasycamView extends TextureView implements Runnable {

	private final String TAG = getClass().getName();
    private boolean DEBUG = false;
    EasycamView _this;

    private EasyCapture capDevice;

	private Thread mThread = null;

    private Rect mViewWindow;
    private Context appContext;
    private volatile boolean mRunning = true;
    private volatile Surface mPreviewSurface;

    SharedPreferences settings;


    public EasycamView(Context context) {
        this(context, null);
    }

    public EasycamView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EasycamView(Context context, AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        appContext = context;

        init();
    }

    private void init() {
        settings = PreferenceManager.getDefaultSharedPreferences(appContext);
        DEBUG = settings.getBoolean("pref_key_debug", false);

        _this = this;

        if (DEBUG) {
            Log.d(TAG, "EasycamView constructed");
        }

        setFocusable(true);
        setBackgroundColor(0);
        this.setSurfaceTextureListener(mSurfaceTextureListener);

    }



    @Override
    public void run() {
        if (mPreviewSurface != null) {
            mPreviewSurface.release();
            mPreviewSurface = null;
        }

        final SurfaceTexture st = getSurfaceTexture();
        if (st != null) {
            mPreviewSurface = new Surface(st);
        }

        while (mRunning) {
            if (capDevice != null && capDevice.isAttached()) {
                capDevice.getFrame(mPreviewSurface);

            } else {
                mRunning = false;
            }
        }

    }

    public HashMap<String, Integer> getAspectRatioHeight() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        HashMap<String, Integer> result = new HashMap<>();

        if (capDevice != null) {
            EasycapSettings deviceSettings = capDevice.getSettings();
            if (deviceSettings != null) {
                float width = (deviceSettings.frameWidth * 1f / deviceSettings.frameHeight)
                        * metrics.heightPixels;
                float height = (deviceSettings.frameHeight * 1f / deviceSettings.frameWidth)
                        * metrics.widthPixels;

                if (metrics.heightPixels < height) {
                    result.put("width", Math.round(width));
                    result.put("height", FrameLayout.LayoutParams.MATCH_PARENT);
                } else {
                    result.put("width", FrameLayout.LayoutParams.MATCH_PARENT);
                    result.put("height", Math.round(height));
                }

                if (DEBUG) {
                    Log.d("Device screen size: ",
                           String.format("%1$dx%2$d", metrics.widthPixels, metrics.heightPixels));
                    Log.d("Calc view size (width)",
                           String.format("%1$fx%2$d", width, metrics.heightPixels));
                    Log.d("Calc view size (height)",
                            String.format("%1$dx%2$f", metrics.widthPixels, height));
                }
            }
        }
        return result;
    }

    protected Rect getViewingWindow() {
        return mViewWindow;
    }

    private void setViewingWindow(int width, int height) {
        mViewWindow = new Rect(0, 0, width, height);
    }

    public void restart() {
        pause();
        resume();
    }

    public void resume() {

        if (mThread != null && mThread.isAlive()) {
            mRunning = false;
            boolean retry = true;
            while (retry) {
                try {
                    mThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        capDevice = new NativeEasyCapture(settings, appContext);
        if (capDevice.isDeviceConnected()) {
            if (DEBUG) {
                Log.i(TAG, "View resumed");
            }

            mRunning = true;
            mThread = new Thread(this);
            mThread.start();

        } else {
            if (DEBUG) {
                Log.e(TAG, "Error connecting device");
            }

            mRunning = false;
            Toast.makeText(appContext, "Error connecting to device", Toast.LENGTH_SHORT).show();
        }

    }

    public void pause() {
        mRunning = false;

        if (mThread != null) {
            boolean retry = true;
            while (retry) {
                try {
                    mThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
        }
        if (capDevice != null) {
            capDevice.stop();
            capDevice = null;
        }

        if (DEBUG) {
            Log.i(TAG, "View paused");
        }
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width,
                                              final int height) {
            resume();
        }

        @Override
        public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width,
                                                final int height) {
            setViewingWindow(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
            pause();

            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(final SurfaceTexture surface) {}
    };
}