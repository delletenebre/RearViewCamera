package kg.delletenebre.rearviewcamera.GuideLines;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PointF;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuideLinesStyle {

    public class GuideLine {
        protected int color, width, effect;
        protected PointF a, b;
        protected String text;

        public GuideLine(int color, PointF a, PointF b, int width, int effect) {
            this.color = color;
            this.a = a;
            this.b = b;
            this.width = width;
            this.effect = effect;
        }

    }

    protected static final String settingsPrefix = "gl_st";
    /*
    Example:
    gl_st0_l0_color: -16711936
    gl_st0_l0_ax: 0.29
    gl_st0_l0_bx: 0.29
    */

    SharedPreferences _settings;

    public GuideLinesStyle(Context context) {
        _settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public List getStyleFromSettings(int index) {

        List<List> defaultStyle = defaultStyles.get(index);
        List<GuideLine> result = new ArrayList<>();

        String prefix = settingsPrefix + index;
        for(int i = 0; i < defaultStyle.size(); i++) {
            GuideLine gline = (GuideLine) defaultStyle.get(i);
            String prefixGLine = prefix + "_l" + i;

            int color = _settings.getInt(prefixGLine + "_color", gline.color);

            PointF a = new PointF(
                _settings.getFloat(prefixGLine + "_ax", gline.a.x),
                _settings.getFloat(prefixGLine + "_ay", gline.a.y)
            );

            PointF b = new PointF(
                _settings.getFloat(prefixGLine + "_bx", gline.b.x),
                _settings.getFloat(prefixGLine + "_by", gline.b.y)
            );

            int width = _settings.getInt(prefixGLine + "_width", gline.width);
            int effect = _settings.getInt(prefixGLine + "_effect", gline.effect);

            result.add(new GuideLine(color, a, b, width, effect));

        }

        return result;
    }

    public Map getStyleMap() {
        int index = Integer.parseInt(_settings.getString("guide_lines_style", "0"));
        List<List> defaultStyle = defaultStyles.get(index);
        Map<String, Object> result = new HashMap<>();

        String prefix = settingsPrefix + index;
        for(int i = 0; i < defaultStyle.size(); i++) {
            GuideLine gline = (GuideLine) defaultStyle.get(i);
            String prefixGLine = prefix + "_l" + i;

            int color = _settings.getInt(prefixGLine + "_color", gline.color);
            result.put(prefixGLine + "_color", color);

            PointF a = new PointF(
                _settings.getFloat(prefixGLine + "_ax", gline.a.x),
                _settings.getFloat(prefixGLine + "_ay", gline.a.y)
            );
            result.put(prefixGLine + "_a", a);

            PointF b = new PointF(
                _settings.getFloat(prefixGLine + "_bx", gline.b.x),
                _settings.getFloat(prefixGLine + "_by", gline.b.y)
            );
            result.put(prefixGLine + "_b", b);

            int width = _settings.getInt(prefixGLine + "_width", gline.width);
            result.put(prefixGLine + "_width", width);

            int effect = _settings.getInt(prefixGLine + "_effect", gline.effect);
            result.put(prefixGLine + "_effect", effect);

        }

        return result;
    }

    public void resetCurrentStyleToDefaults() {
        SharedPreferences.Editor _settingsEditor = _settings.edit();

        int index = Integer.parseInt(_settings.getString("guide_lines_style", "0"));

        List<List> defaultStyle = defaultStyles.get(index);
        String prefix = settingsPrefix + index;
        for(int i = 0; i < defaultStyle.size(); i++) {
            GuideLine gline = (GuideLine) defaultStyle.get(i);
            String prefixGLine = prefix + "_l" + i + "_";

            _settingsEditor.putInt(prefixGLine + "color", gline.color);
            _settingsEditor.putInt(prefixGLine + "width", gline.width);
            _settingsEditor.putInt(prefixGLine + "effect", gline.effect);

            _settingsEditor.putFloat(prefixGLine + "ax", gline.a.x);
            _settingsEditor.putFloat(prefixGLine + "ay", gline.a.y);

            _settingsEditor.putFloat(prefixGLine + "bx", gline.b.x);
            _settingsEditor.putFloat(prefixGLine + "by", gline.b.y);

        }

        _settingsEditor.apply();
    }
//
//    private PointF getCoordinatesForY(PointF a, PointF b, float x) {
//        return new PointF( x, getPointY(a, b, x) );
//    }
//
//    private PointF getCoordinatesForX(PointF a, PointF b, float y) {
//        return new PointF( getPointX(a, b, y), y );
//    }
//
//    private float getPointY(PointF a, PointF b, float x) {
//        return ( (b.y - a.y) * (x - a.x) / (b.x - a.x) ) + a.y;
//    }
//
//    private float getPointX(PointF a, PointF b, float y) {
//        return ( (b.x - a.x) * (y - a.y) / (b.y - a.y) ) + a.x;
//    }

    private final List<List> defaultStyles = new ArrayList<List>() {{
        /* **** 0 **** */
        add( new ArrayList<GuideLine> () {{
            add( new GuideLine(
                    Color.rgb(0, 255, 0),
                    new PointF(0.2857f, 0.4f),
                    new PointF(0.7142f, 0.4f),
                    3,
                    0
            ));
            add( new GuideLine(
                    Color.rgb(255, 0, 0),
                    new PointF(0.2228f, 0.84f),
                    new PointF(0.7771f, 0.84f),
                    3,
                    0
            ));
            add( new GuideLine(
                    Color.rgb(255, 255, 0),
                    new PointF(0.2f, 1.0f),
                    new PointF(0.3f, 0.3f),
                    3,
                    0
            ));
            add( new GuideLine(
                    Color.rgb(255, 255, 0),
                    new PointF(0.8f, 1.0f),
                    new PointF(0.7f, 0.3f),
                    3,
                    0
            ));
        }});
        /* **** END 0 **** */
    }};

}
