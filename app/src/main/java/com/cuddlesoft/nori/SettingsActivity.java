/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/** Main settings activity managing all the core preferences for the app, launched from {@link com.cuddlesoft.nori.SearchActivity}. */
@SuppressWarnings("deprecation") // The non-fragment Preferences API is deprecated, but there is no alternative in the support library for API<11 support.
public class SettingsActivity extends PreferenceActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
  }

}
