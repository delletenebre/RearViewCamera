package kg.delletenebre.rearviewcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import java.util.HashMap;

public class MainActivity extends Activity {
    private final String TAG = getClass().getName();
    private boolean DEBUG = false;

    public static Activity activity;
    private SharedPreferences settings;

    private EasycamView cameraView;
    private FrameLayout mainLayout;

    private Runnable setAspectRatio = new Runnable() {
        @Override
        public void run() {
            setViewLayout();
        }
    };

    private AudioManager mAudioManager;

    private boolean pref_isMute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        DEBUG = settings.getBoolean("pref_key_debug", false);

        mainLayout = (FrameLayout) findViewById(R.id.layout_main);
        mainLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onClick() {
                super.onClick();

                if (cameraView != null) {
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

        cameraView = (EasycamView) findViewById(R.id.camera_view);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();

        pref_isMute = settings.getBoolean("pref_key_mute", true);

        setMute(true);
        activity = this;

        if (cameraView != null) {
            boolean mirrored = settings.getBoolean("pref_key_mirrored", false);
            cameraView.setScaleX(mirrored ? -1 : 1);
        }

        if (settings.getBoolean("pref_key_autodetect_signal", true)
                && DetectSignalService.service == null) {
            startService(new Intent(this, DetectSignalService.class));
        } else {
            Log.d(TAG, "Detect Signal Service already started or disabled");
        }
    }

    @Override
    public void onPause() {
        setMute(false);
        activity = null;

        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        boolean pref_isFullscreen = settings.getBoolean("pref_key_fullscreen", false);
        if (pref_isFullscreen && hasFocus) {
            getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        }

        if (hasFocus) {
            mainLayout.post(setAspectRatio);
        }
    }

    private void setViewLayout() {
        FrameLayout.LayoutParams params;

        boolean pref_isKeepAspectRatio = settings.getBoolean("pref_key_keep_aspect_ratio", true);

        if (pref_isKeepAspectRatio) {
            HashMap<String, Integer> screenSize = cameraView.getAspectRatioHeight();
            params = new FrameLayout.LayoutParams(screenSize.get("width"),
                    screenSize.get("height"), Gravity.CENTER);
        } else {
            params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        }

        cameraView.setLayoutParams(params);
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


}
