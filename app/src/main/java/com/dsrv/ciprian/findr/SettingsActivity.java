package com.dsrv.ciprian.findr;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.preference.Preference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.SeekBarPreference;
import android.view.MenuItem;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pref_headers);
        setupActionBar();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat implements
            SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
            //addPreferencesFromResource(R.xml.pref_general);
            //setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            //bindPreferenceSummaryToValue(findPreference("filter_list"));
            //bindPreferenceSummaryToValue(findPreference("distance_slider"));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            //setPreferencesFromResource(R.xml.pref_general, rootKey);

            addPreferencesFromResource(R.xml.pref_general);

            SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

            PreferenceScreen preferenceScreen = getPreferenceScreen();

            int count = preferenceScreen.getPreferenceCount();

            for (int i = 0; i < count ; i++) {
                Preference p = preferenceScreen.getPreference(i);
                if (p instanceof ListPreference) {
                    String value = sharedPreferences.getString(p.getKey(), "");
                    setPreferenceSummery(p, Integer.parseInt(value));
                } else if (p instanceof SeekBarPreference) {
                    setPreferenceSummery(p, ((SeekBarPreference) p).getValue());
                }
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference p = findPreference(key);

            if (p != null) {
                if (p instanceof ListPreference) {
                    String value = sharedPreferences.getString(p.getKey(), "");
                    setPreferenceSummery(p, Integer.parseInt(value));
                } else if (p instanceof SeekBarPreference) {
                    setPreferenceSummery(p, ((SeekBarPreference) p).getValue());
                }
            }
        }

        private void setPreferenceSummery(android.support.v7.preference.Preference preference, Object value){

            String stringValue = value.toString();

            if (preference instanceof ListPreference){
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list (since they have separate labels/values).
                ListPreference listPreference = (ListPreference) preference;
                int prefIndex = listPreference.findIndexOfValue(stringValue);
                //same code in one line
                //int prefIndex = ((ListPreference) preference).findIndexOfValue(value);

                //prefIndex must be is equal or garter than zero because
                //array count as 0 to ....
                if (prefIndex >= 0){
                    listPreference.setSummary(listPreference.getEntries()[prefIndex]);
                }
            } else {
                // For other preferences, set the summary to the value's simple string representation.
                preference.setSummary(stringValue);
            }
        }
    }
}
