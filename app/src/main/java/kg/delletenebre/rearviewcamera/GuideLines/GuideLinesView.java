package kg.delletenebre.rearviewcamera.GuideLines;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public final class GuideLinesView extends View {
    private static final String TAG = "GuideLinesView";

    SharedPreferences settings;


    // drawing tools
    private RectF glRectf;
    private Paint glPaint;
    private Path  glPath;
    private GuideLinesStyle glStyle;

    public GuideLinesView(Context context) {
        super(context);

        initDrawingTools();
    }

    public GuideLinesView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initDrawingTools();
    }


    private void initDrawingTools() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        settings = PreferenceManager.getDefaultSharedPreferences(getContext());

        glRectf = new RectF();
        glPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glPaint.setStyle(Paint.Style.STROKE);

        glStyle = new GuideLinesStyle(getContext());
        glPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if( settings.getBoolean("guide_lines_show", true) ) {

            List<GuideLinesStyle.GuideLine> style = glStyle.getStyleFromSettings(Integer.parseInt(settings.getString("guide_lines_style", "0")));//get current guide lines style

            int w = getWidth(),
                h = getHeight();

            glRectf.set(0,0, w,h);

            for(int i = 0; i < style.size(); i++) {
                GuideLinesStyle.GuideLine gline = style.get(i);

                int color = gline.color,
                    width = gline.width,
                    effect = gline.effect;

                PointF a = gline.a,
                       b = gline.b;


                glPaint.setStrokeWidth(width);
                glPaint.setColor(color);

                glPaint.setPathEffect(getPathEffect(effect, width));

                glPath.moveTo(a.x * w, a.y * h);
                glPath.lineTo(b.x * w, b.y * h);

                canvas.drawPath(glPath, glPaint);
                glPath.reset();
            }
        }
    }

    private DashPathEffect getPathEffect(int index, int width) {
        DashPathEffect effect;

        switch(index) {
            case 1:
                effect = new DashPathEffect(new float[] { dpToPx(width * 5), dpToPx(width * 3) }, 0);//dashed
                break;
            case 2:
                effect = new DashPathEffect(new float[] { dpToPx(width), dpToPx(width * 2) }, 0);//dotted
                break;
            case 3:
                effect = new DashPathEffect(new float[] { dpToPx(width * 5), dpToPx(width * 3), dpToPx(width), dpToPx(width * 3) }, 0);//dash-dotted
                break;
            default:
                effect = null;
                break;
        }

        return effect;
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

}

