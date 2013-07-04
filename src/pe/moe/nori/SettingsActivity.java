package pe.moe.nori;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import pe.moe.nori.preferences.NumberPickerPreference;

public class SettingsActivity extends SherlockPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Register and initialize shared preference listener.
    // It's used for updating the preference summary with its current value.
    SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
    sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    for (String key : sharedPreferences.getAll().keySet()) {
      onSharedPreferenceChanged(sharedPreferences, key);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Unregister SharedPreferenceChangeListener.
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    Preference preference = findPreference(key);

    // Update summary.
    if (preference instanceof ListPreference) {
      ListPreference listPreference = (ListPreference) preference;
      preference.setSummary(listPreference.getEntry());
    } else if (preference instanceof EditTextPreference) {
      EditTextPreference editTextPreference = (EditTextPreference) preference;
      preference.setSummary(editTextPreference.getText());
    } else if (key.equals("slideshow_delay")) {
      NumberPickerPreference numberPickerPreference = (NumberPickerPreference) preference;
      preference.setSummary(String.format(getString(R.string.preference_slideshowDelay_summary),
          numberPickerPreference.getValue()));
    }
  }
}
