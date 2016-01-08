package kg.delletenebre.rearviewcamera;

import android.app.Activity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity {

    public static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .add(android.R.id.content, new SettingsFragment())
                .commit();

    }

    @Override
    public void onResume() {
        super.onResume();

        activity = this;
    }

    @Override
    public void onPause() {
        activity = null;

        super.onPause();
    }


    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            bindPreferenceSummaryToValue(findPreference("pref_select_dev_loc"));
            bindPreferenceSummaryToValue(findPreference("pref_key_manual_set_dev_loc_interval_min"));
            bindPreferenceSummaryToValue(findPreference("pref_key_manual_set_dev_loc_interval_max"));
            bindPreferenceSummaryToValue(findPreference("pref_select_easycap_type"));
            bindPreferenceSummaryToValue(findPreference("pref_select_standard"));
            bindPreferenceSummaryToValue(findPreference("pref_key_autodetect_usb_device_interval"));
            bindPreferenceSummaryToValue(findPreference("pref_key_autodetect_rim_command"));
            bindPreferenceSummaryToValue(findPreference("pref_key_autodetect_rim_args"));
        }
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                preference.setSummary(stringValue);
            }

            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }
}
