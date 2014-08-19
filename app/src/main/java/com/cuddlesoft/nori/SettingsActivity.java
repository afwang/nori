/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori;

import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

/** Main settings activity managing all the core preferences for the app, launched from {@link com.cuddlesoft.nori.SearchActivity}. */
@SuppressWarnings("deprecation")
// The non-fragment Preferences API is deprecated, but there is no alternative in the support library for API<11 support.
public class SettingsActivity extends PreferenceActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // TODO: This could be supported on API < 11 using android-support-v4-preferencefragment (https://github.com/kolavar/android-support-v4-preferencefragment)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      // Add "up" button to the action bar.
      ActionBar actionBar = getActionBar();
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
}
