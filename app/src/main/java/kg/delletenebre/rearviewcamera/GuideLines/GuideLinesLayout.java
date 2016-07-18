package kg.delletenebre.rearviewcamera.GuideLines;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kg.delletenebre.rearviewcamera.R;

public class GuideLinesLayout extends RelativeLayout {

    SharedPreferences settings;

    private final static float LINE_MOVE_STEP = 0.005f;
    private final static int RADIOBUTTON_MARGIN = dpToPx(17);

    private List<RadioButton> radioButtons = new ArrayList<>();
    GuideLinesView glView;
    private Point layoutGLEditSize;

    public GuideLinesLayout(Context context) {
        super(context);

        init();
    }

    public GuideLinesLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    public GuideLinesLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public void init() {
        settings = PreferenceManager.getDefaultSharedPreferences(getContext());

        glView = new GuideLinesView(getContext());
        addView(glView);

        //this.setVisibility(RelativeLayout.VISIBLE);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeGlobalOnLayoutListener(this);

                int w = getMeasuredWidth(),
                    h = getMeasuredHeight();

//                if ( fullscreenMode == 2 ) {
//                    h += dpToPx(48);
//                }

                layoutGLEditSize = new Point(w, h);

                int current_style_index = Integer.parseInt(settings.getString("guide_lines_style", "0"));
                GuideLinesStyle gl_style = new GuideLinesStyle(getContext());
                List<GuideLinesStyle.GuideLine> style = gl_style.getStyleFromSettings( current_style_index );

                String settingsPrefix = GuideLinesStyle.settingsPrefix + current_style_index;
                for(int i = 0; i < style.size(); i++) {
                    GuideLinesStyle.GuideLine gline = style.get(i);

                    String settingsPrefixGLine = settingsPrefix + "_l" + i;

                    PointF a = gline.a,
                            b = gline.b;

                    RadioButton rb = new RadioButton(getContext());
                    rb.setX(Math.round(a.x * w - RADIOBUTTON_MARGIN));
                    rb.setY(Math.round(a.y * h - RADIOBUTTON_MARGIN));
                    rb.setContentDescription(settingsPrefixGLine + "_a");
                    radioButtons.add(rb);

                    rb = new RadioButton(getContext());
                    rb.setX(Math.round(b.x * w - RADIOBUTTON_MARGIN));
                    rb.setY(Math.round(b.y * h - RADIOBUTTON_MARGIN));
                    rb.setContentDescription(settingsPrefixGLine + "_b");
                    radioButtons.add(rb);
                }

                for (RadioButton rb : radioButtons) {
                    addView(rb);

                    rb.setOnClickListener(new RadioButton.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            for (RadioButton rb : radioButtons) {
                                rb.setChecked(false);
                            }

                            ((RadioButton) v).setChecked(true);
                        }
                    });
                }
            }
        });

        TextView btnUp = createIconButton("\ue001", "#88ffffff",
                (new int[]{ALIGN_PARENT_TOP, CENTER_HORIZONTAL}));
        TextView btnDown = createIconButton("\ue000", "#88ffffff",
                (new int[]{ALIGN_PARENT_BOTTOM, CENTER_HORIZONTAL}));
        TextView btnLeft = createIconButton("\ue002", "#88ffffff",
                (new int[]{ALIGN_PARENT_START, CENTER_VERTICAL}));
        TextView btnRight = createIconButton("\ue004", "#88ffffff",
                (new int[]{ALIGN_PARENT_END, CENTER_VERTICAL}));
        TextView btnColor = createIconButton("\ue003", "#88ffff00",
                (new int[]{ALIGN_PARENT_END, ALIGN_PARENT_TOP}));
        TextView btnReset = createIconButton("\ue005", "#88ff0000",
                (new int[]{ALIGN_PARENT_START, ALIGN_PARENT_TOP}));

        addView(btnUp);
        addView(btnDown);
        addView(btnLeft);
        addView(btnRight);
        addView(btnColor);
        addView(btnReset);

        //**** Нажатие кнопки ВВЕРХ
        btnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveGuideLineByRadioButton(getActiveRadioButton(), "up", false);
            }
        });

        //**** Нажатие (долгое) кнопки ВВЕРХ
        btnUp.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                moveGuideLineByRadioButton(getActiveRadioButton(), "up", true);
                return true;
            }
        });

        //**** Нажатие кнопки ВНИЗ
        btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveGuideLineByRadioButton(getActiveRadioButton(), "down", false);
            }
        });
        //**** Нажатие (долгое) кнопки ВНИЗ
        btnDown.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                moveGuideLineByRadioButton(getActiveRadioButton(), "down", true);
                return true;
            }
        });

        //**** Нажатие кнопки ВЛЕВО
        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveGuideLineByRadioButton(getActiveRadioButton(), "left", false);
            }
        });
        //**** Нажатие (долгое) кнопки ВЛЕВО
        btnLeft.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                moveGuideLineByRadioButton(getActiveRadioButton(), "left", true);
                return true;
            }
        });

        //**** Нажатие кнопки ВПРАВО
        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveGuideLineByRadioButton(getActiveRadioButton(), "right", false);
            }
        });
        //**** Нажатие (долгое) кнопки ВПРАВО
        btnRight.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                moveGuideLineByRadioButton(getActiveRadioButton(), "right", true);
                return true;
            }
        });

        btnColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioButton rb = getActiveRadioButton();
//                if ( rb != null ) {
//                    String settingKeyTemp = rb.getContentDescription().toString();
//                    final String settingKeyColor = settingKeyTemp.substring(0, settingKeyTemp.length()-1) + "color";
//                    final String settingKeyEffect = settingKeyTemp.substring(0, settingKeyTemp.length()-1) + "effect";
//                    final String settingKeyWidth = settingKeyTemp.substring(0, settingKeyTemp.length()-1) + "width";
//
//                    int initialColor = (int) getCurrentStyleMap().get(settingKeyColor);
//                    int initialEffect = (int) getCurrentStyleMap().get(settingKeyEffect);
//                    int initialWidth = (int) getCurrentStyleMap().get(settingKeyWidth);
//
//                    final ColorPickerDialog colorDialog = new ColorPickerDialog(getContext(), initialColor, initialEffect, initialWidth);
//
//                    colorDialog.setAlphaSliderVisible(true);
//
//                    colorDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
//
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            SharedPreferences.Editor settingsEditor = settings.edit();
//                            settingsEditor.putInt(settingKeyColor, colorDialog.getColor());
//                            settingsEditor.putInt(settingKeyEffect, colorDialog.getLineStyle());
//                            settingsEditor.putInt(settingKeyWidth, colorDialog.getLineWidth());
//                            settingsEditor.apply();
//
//                            if (glView != null) {
//                                glView.invalidate();
//                            }
//                        }
//                    });
//
////                    colorDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
////
////                        @Override
////                        public void onClick(DialogInterface dialog, int which) {
////                            // Делать нечего
////                        }
////                    });
//
//                    colorDialog.show();
//
//                } else {
//                    Toast.makeText(getContext(), R.string.choose_line, Toast.LENGTH_SHORT).show();
//                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.title_confirm_reset_guide_line_style)
                        .setMessage(R.string.confirm_reset_guide_line_style)
                        .setCancelable(true)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                GuideLinesStyle gl_style = new GuideLinesStyle(getContext());
                                gl_style.resetCurrentStyleToDefaults();
                                Map currentStyle = gl_style.getStyleMap();

                                if (glView != null) {
//                                    if (radioButtons.size() > 0) {
//                                        for (int i = 0; i < radioButtons.size(); i++) {
//                                            RadioButton rb = radioButtons.get(i);
//                                            PointF point = (PointF) currentStyle.get(rb.getContentDescription().toString());
//                                            rb.setX(Math.round(point.x * layoutGLEdit.getMeasuredWidth() - radioButtonMargin));
//                                            rb.setY(Math.round(point.y * layoutGLEdit.getMeasuredHeight() - radioButtonMargin));
//                                        }
//                                    }

                                    glView.invalidate();
                                }

                                Toast.makeText(getContext(), R.string.guide_lines_style_was_reset, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
            }
        });
        /**** END Display: Calibrate guide lines ****/

    }


    private TextView createIconButton(String text, String color, int[] rules) {
        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "icons.ttf");


        TextView button = new TextView(new ContextThemeWrapper(getContext(), R.style.iconButtonStyle));
        button.setTypeface(typeface);
        button.setTextColor(Color.parseColor(color));
        //paramsExample.setMargins(20, 20, 20, 20);
        button.setText(text);

        LayoutParams layoutParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        for (int rule : rules) {
            layoutParams.addRule(rule);
        }
        button.setLayoutParams(layoutParams);

        return button;
    }



    private static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    private Map getCurrentStyleMap() {
        GuideLinesStyle gl_style = new GuideLinesStyle(getContext());
        return gl_style.getStyleMap();
    }

    private PointF getCoordinatesForRB(RadioButton rb) {
        Map style = getCurrentStyleMap();

        return (PointF) style.get(String.valueOf(rb.getContentDescription()));
    }


    private void saveCoordinatesForRB(CharSequence settingKey, PointF point) {
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putFloat(String.valueOf(settingKey) + "x", point.x);
        settingsEditor.putFloat(String.valueOf(settingKey) + "y", point.y);
        settingsEditor.apply();
    }

    private void moveGuideLineByRadioButton(RadioButton rb, String direction, boolean longClick) {
        if (rb != null && layoutGLEditSize != null) {
            float step = longClick ? LINE_MOVE_STEP * 10 : LINE_MOVE_STEP;
            PointF point = getCoordinatesForRB(rb);

            switch (direction) {
                case "up":
                    point.y -= step;

                    break;
                case "down":
                    point.y += step;

                    break;
                case "left":
                    point.x -= step;

                    break;
                case "right":
                    point.x += step;

                    break;
            }
            point = checkPointForMinMax(point);

            rb.setX(Math.round(point.x * layoutGLEditSize.x - RADIOBUTTON_MARGIN));
            rb.setY(Math.round(point.y * layoutGLEditSize.y - RADIOBUTTON_MARGIN));

            saveCoordinatesForRB(rb.getContentDescription(), point);

            if (glView != null) {
                glView.invalidate();
            }
        }
    }


    private PointF checkPointForMinMax(PointF point) {
        point.x = (point.x > 1.0f) ? 1.0f : point.x;
        point.y = (point.y > 1.0f) ? 1.0f : point.y;
        point.x = (point.x < 0.0f) ? 0.0f : point.x;
        point.y = (point.y < 0.0f) ? 0.0f : point.y;

        return point;
    }

    private RadioButton getActiveRadioButton() {
        for (RadioButton rb : radioButtons) {
            if (rb.isChecked()) {
                return rb;
            }
        }

        return null;
    }
}
