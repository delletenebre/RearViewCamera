package kg.delletenebre.rearviewcamera.UVC;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class UVCCameraView extends TextureView {

    private double mRequestedAspect = -1.0;

    public UVCCameraView(final Context context) {
        this(context, null, 0);
    }

    public UVCCameraView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UVCCameraView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
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

}