/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import com.cuddlesoft.nori.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NSFWFilterSettingsActivity extends ActionBarActivity implements ListView.OnItemClickListener, CompoundButton.OnCheckedChangeListener {
  /** Default {@link android.content.SharedPreferences} object. */
  private SharedPreferences sharedPreferences;
  /** Human-readable labels for each obscenity rating. */
  private String[] obscenityRatingEntries;
  /** Human-readable summaries for each obscenity rating. */
  private String[] obscenityRatingSummaries;
  /** Values for each obscenity rating stored in {@link android.content.SharedPreferences} */
  private String[] obscenityRatingValues;
  /** Current values of the preference_nsfwFilter preference. */
  private List<String> obscenityRatingsFiltered = new ArrayList<>(4);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_nsfwfilter_settings);

    // Set up the action bar.
    final ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);

    // Get shared preference object.
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    // Get string array resources.
    obscenityRatingEntries = getResources().getStringArray(R.array.preference_nsfwFilter_entries);
    obscenityRatingSummaries = getResources().getStringArray(R.array.preference_nsfwFilter_summaries);
    obscenityRatingValues = getResources().getStringArray(R.array.preference_nsfwFilter_values);

    // Get current value of the preference_nsfwFilter preference, or fallback to the default value.
    if (sharedPreferences.contains(getString(R.string.preference_nsfwFilter_key))) {
      final String nsfwFilter = sharedPreferences.getString(getString(R.string.preference_nsfwFilter_key), null).trim();
      if (!TextUtils.isEmpty(nsfwFilter)) {
        obscenityRatingsFiltered.addAll((Arrays.asList(nsfwFilter.split(" "))));
      }
    } else {
      final String[] obscenityRatingDefaultValues = getResources().getStringArray(R.array.preference_nsfwFilter_defaultValues);
      obscenityRatingsFiltered.addAll(Arrays.asList(obscenityRatingDefaultValues));
    }

    // Set up ListView.
    final ListView listView = (ListView) findViewById(android.R.id.list);
    listView.setAdapter(new ObscenityRatingListAdapter());
    listView.setOnItemClickListener(this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home: // Handle back button in the action bar.
        onBackPressed();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
    // Toggle checkbox when List item is clicked.
    final Checkable checkBox = (Checkable) view.findViewById(R.id.checkbox);
    checkBox.toggle();
  }

  @SuppressWarnings("RedundantCast")
  @Override
  public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
    if (checked && !obscenityRatingsFiltered.contains((String) compoundButton.getTag())) {
      obscenityRatingsFiltered.add((String) compoundButton.getTag());
    } else if (!checked && obscenityRatingsFiltered.contains((String) compoundButton.getTag())) {
      obscenityRatingsFiltered.remove((String) compoundButton.getTag());
    }

    // Update SharedPreferences.
    sharedPreferences.edit()
        .putString(getString(R.string.preference_nsfwFilter_key),
            StringUtils.mergeStringArray(obscenityRatingsFiltered.toArray(new String[obscenityRatingsFiltered.size()]), " ").trim())
        .apply();
  }

  private class ObscenityRatingListAdapter extends BaseAdapter {

    public int getCount() {
      return obscenityRatingEntries.length;
    }

    @Override
    public Object getItem(int position) {
      return obscenityRatingEntries[position];
    }

    @Override
    public long getItemId(int position) {
      // Position == item id.
      return position;
    }

    @Override
    public View getView(int position, View recycledView, ViewGroup container) {
      // Recycle view if possible.
      View view = recycledView;
      if (view == null) {
        final LayoutInflater inflater = getLayoutInflater();
        view = inflater.inflate(R.layout.listitem_obscenity_rating, container, false);
      }

      // Populate views.
      final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
      checkBox.setChecked(obscenityRatingsFiltered.contains(obscenityRatingValues[position]));
      checkBox.setOnCheckedChangeListener(NSFWFilterSettingsActivity.this);
      checkBox.setTag(obscenityRatingValues[position]);
      final TextView title = (TextView) view.findViewById(R.id.title);
      title.setText(obscenityRatingEntries[position]);
      final TextView summary = (TextView) view.findViewById(R.id.summary);
      summary.setText(obscenityRatingSummaries[position]);

      return view;
    }
  }
}
