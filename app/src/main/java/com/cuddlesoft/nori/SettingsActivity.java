/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import com.cuddlesoft.nori.service.ClearSearchHistoryService;

/** Main settings activity managing all the core preferences for the app, launched from {@link com.cuddlesoft.nori.SearchActivity}. */
@SuppressWarnings("deprecation")
// The non-fragment Preferences API is deprecated, but there is no alternative in the support library for API<11 support.
public class SettingsActivity extends PreferenceActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // This could be supported on API < 11 using android-support-v4-preferencefragment (https://github.com/kolavar/android-support-v4-preferencefragment)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      // Add "up" button to the action bar.
      @SuppressLint("AppCompatMethod") ActionBar actionBar = getActionBar();
      if (actionBar != null) {
        actionBar.setDisplayHomeAsUpEnabled(true);
      }
    }

    addPreferencesFromResource(R.xml.preferences);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // Make the action bar "up" button behave the same way as the physical "back" button.
        onBackPressed();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
    // Override default action for the clear search history preference item.
    if (preference.getKey() != null && preference.getKey().equals("preference_clearSearchHistory")) {
      // Start ClearSearchHistoryService.
      Intent intent = new Intent(this, ClearSearchHistoryService.class);
      startService(intent);
      return true;
    } else {
      return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
  }
}
