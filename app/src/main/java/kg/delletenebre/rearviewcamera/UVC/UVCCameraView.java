package kg.delletenebre.rearviewcamera.UVC;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import kg.delletenebre.rearviewcamera.MainActivity;
import kg.delletenebre.rearviewcamera.R;

public class UVCCameraView extends TextureView implements TextureView.SurfaceTextureListener {
    private static final String TAG = "UVCCameraView";
    private static boolean DEBUG = false;

    public static volatile Surface previewSurface;
    public Surface initPreviewSurface() {
        final SurfaceTexture surfaceTexture = getSurfaceTexture();
        if (surfaceTexture != null) {
            previewSurface = new Surface(surfaceTexture);
        }

        return previewSurface;
    }
    public static void releasePreviewSurface() {
        if (previewSurface != null) {
            previewSurface.release();
            previewSurface = null;
        }
    }

    private static int[] resolution = {640, 480};
    public static int[] getResolution() {
        return resolution;
    }

    private double mRequestedAspect = -1.0;
    SharedPreferences settings;

    public UVCCameraView(final Context context) {
        this(context, null, 0);
    }

    public UVCCameraView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UVCCameraView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        DEBUG = settings.getBoolean("pref_key_debug", false);

        this.setSurfaceTextureListener(this);
    }




    public void setAspectRatio(final double aspectRatio) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }
        if (mRequestedAspect != aspectRatio) {
            mRequestedAspect = aspectRatio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (mRequestedAspect > 0) {
            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);

            final int horPadding = getPaddingLeft() + getPaddingRight();
            final int verPadding = getPaddingTop() + getPaddingBottom();
            initialWidth -= horPadding;
            initialHeight -= verPadding;

            final double viewAspectRatio = (double)initialWidth / initialHeight;
            final double aspectDiff = mRequestedAspect / viewAspectRatio - 1;

            if (Math.abs(aspectDiff) > 0.01) {
                if (aspectDiff > 0) {
                    initialHeight = (int) (initialWidth / mRequestedAspect);
                } else {
                    initialWidth = (int) (initialHeight * mRequestedAspect);
                }
                initialWidth += horPadding;
                initialHeight += verPadding;
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }



    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface,
                                          final int width, final int height) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        String[] mUVCResolutionString = settings.getString("pref_key_uvc_resolution",
                getResources().getString(R.string.pref_default_uvc_resolution)).split("x");
        for (int i = 0; i < resolution.length; i++) {
            resolution[i] = Integer.parseInt(mUVCResolutionString[i]);
        }
        setAspectRatio((settings.getBoolean("pref_key_keep_aspect_ratio", true))
                        ? resolution[0] / (float) resolution[1]
                        : metrics.widthPixels / (float) metrics.heightPixels);

        MainActivity.reconnectUVC();
    }

    @Override
    public void onSurfaceTextureSizeChanged(final SurfaceTexture surface,
                                            final int width, final int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");

        releasePreviewSurface();

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(final SurfaceTexture surface) {}

}