/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.cuddlesoft.nori.fragment.ImageFragment;
import com.cuddlesoft.norilib.Image;
import com.cuddlesoft.norilib.SearchResult;
import com.cuddlesoft.norilib.Tag;
import com.cuddlesoft.norilib.clients.SearchClient;

import java.io.IOException;

/** Activity used to display full-screen images. */
public class ImageViewerActivity extends ActionBarActivity implements ViewPager.OnPageChangeListener, ImageFragment.ImageFragmentListener {
  /** Identifier used to keep the displayed {@link com.cuddlesoft.norilib.SearchResult} in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_RESULT = "com.cuddlesoft.nori.SearchResult";
  /** Identifier used to keep the position of the selected {@link com.cuddlesoft.norilib.Image} in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_IMAGE_INDEX = "com.cuddlesoft.nori.ImageIndex";
  /** Identifier used to keep {@link #searchClient} settings in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_CLIENT_SETTINGS = "com.cuddlesoft.nori.SearchClient.Settings";
  /** Fetch more images when the displayed image is this far from the last {@link com.cuddlesoft.norilib.Image} in the current {@link com.cuddlesoft.norilib.SearchResult}. */
  private static final int INFINITE_SCROLLING_THRESHOLD = 3;
  /** Default shared preferences. */
  private SharedPreferences sharedPreferences;
  /** View pager used to display the images. */
  private ViewPager viewPager;
  /** Search result shown by the {@link android.support.v4.app.FragmentStatePagerAdapter}. */
  private SearchResult searchResult;
  /** Adapter used to populate the {@link android.support.v4.view.ViewPager} used to display and flip through the images. */
  private ImagePagerAdapter imagePagerAdapter;
  /** Search API client used to retrieve more search results for infinite scrolling. */
  private SearchClient searchClient;
  /** Callback waiting to receive another page of {@link com.cuddlesoft.norilib.Image}s for the current {@link com.cuddlesoft.norilib.SearchResult}. */
  private SearchClient.SearchCallback searchCallback;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Get shared preferences.
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    // Get data out of Intent sent by SearchActivity or restore them from the saved instance
    // state.
    final int imageIndex;
    if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_ID_IMAGE_INDEX) &&
        savedInstanceState.containsKey(BUNDLE_ID_SEARCH_RESULT)) {
      imageIndex = savedInstanceState.getInt(BUNDLE_ID_IMAGE_INDEX);
      searchResult = savedInstanceState.getParcelable(BUNDLE_ID_SEARCH_RESULT);
      searchClient = ((SearchClient.Settings) savedInstanceState.getParcelable(BUNDLE_ID_SEARCH_CLIENT_SETTINGS))
          .createSearchClient();
    } else {
      final Intent intent = getIntent();
      imageIndex = intent.getIntExtra(SearchActivity.BUNDLE_ID_IMAGE_INDEX, 0);
      searchResult = intent.getParcelableExtra(SearchActivity.BUNDLE_ID_SEARCH_RESULT);
      searchClient = ((SearchClient.Settings) intent.getParcelableExtra(SearchActivity.BUNDLE_ID_SEARCH_CLIENT_SETTINGS))
          .createSearchClient();
    }

    // Request window features.
    supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    // Keep screen on, if enabled by the user.
    if (sharedPreferences.getBoolean(getString(R.string.preference_image_viewer_keepScreenOn_key), true)) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Populate content view.
    setContentView(R.layout.activity_image_viewer);

    // Set up the action bar.
    final ActionBar actionBar = getSupportActionBar();
    actionBar.hide();
    actionBar.setDisplayShowHomeEnabled(false);
    actionBar.setDisplayHomeAsUpEnabled(true);

    // Create and set the image viewer Fragment pager adapter.
    imagePagerAdapter = new ImagePagerAdapter(getSupportFragmentManager());
    viewPager = (ViewPager) findViewById(R.id.image_pager);
    viewPager.setAdapter(imagePagerAdapter);
    viewPager.setOnPageChangeListener(this);
    viewPager.setCurrentItem(imageIndex);

    // Dim system UI.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      viewPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle menu item interactions.
    switch (item.getItemId()) {
      case android.R.id.home: // Action bar "back button".
        onBackPressed();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Set the activity title to contain the currently displayed image's metadata.
   *
   * @param image Image to get the metadata from.
   */
  private void setTitle(Image image) {
    String title = String.format(getString(R.string.activity_image_viewer_titleFormat),
        image.id, Tag.stringFromArray(image.tags));

    // Truncate string with ellipsis at the end, if needed.
    if (title.length() > getResources().getInteger(R.integer.activity_image_viewer_titleMaxLength)) {
      title = title.substring(0, getResources().getInteger(R.integer.activity_image_viewer_titleMaxLength)) + "…";
    }
    setTitle(title);
  }

  /**
   * Fetch images from the next page of the {@link com.cuddlesoft.norilib.SearchResult}, if available.
   */
  private void fetchMoreImages() {
    // Ignore request if there is another API request pending.
    if (searchCallback != null) {
      return;
    }
    // Show the indeterminate progress bar in the action bar, and show the action bar if hidden.
    setSupportProgressBarIndeterminateVisibility(true);
    getSupportActionBar().show();
    // Request search result from API client.
    searchCallback = new InfiniteScrollingSearchCallback(searchResult);
    searchClient.search(Tag.stringFromArray(searchResult.getQuery()), searchResult.getCurrentOffset() + 1, searchCallback);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    // Keep search result and the index of currently displayed image.
    outState.putParcelable(BUNDLE_ID_SEARCH_RESULT, searchResult);
    outState.putInt(BUNDLE_ID_IMAGE_INDEX, viewPager.getCurrentItem());
    outState.putParcelable(BUNDLE_ID_SEARCH_CLIENT_SETTINGS, searchClient.getSettings());
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
  }

  @Override
  public void onPageSelected(int position) {
    // Set activity title to image metadata.
    setTitle(searchResult.getImages()[position]);

    // Fetch more images for infinite scrolling, if available and there isn't another search request being waited on.
    if (searchCallback == null && searchResult.hasNextPage()
        && (searchResult.getImages().length - position) <= INFINITE_SCROLLING_THRESHOLD) {
      fetchMoreImages();
    }
  }

  @Override
  public void onPageScrollStateChanged(int state) {
  }

  @Override
  public void onSingleTapConfirmed() {
    // Toggle the action bar and UI dim.
    ActionBar actionBar = getSupportActionBar();
    if (actionBar.isShowing()) {
      actionBar.hide();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        viewPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
      }
    } else {
      actionBar.show();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        viewPager.setSystemUiVisibility(0);
      }
    }
  }

  /** Adapter used to populate {@link android.support.v4.view.ViewPager} with {@link com.cuddlesoft.nori.fragment.ImageFragment}s. */
  private class ImagePagerAdapter extends FragmentStatePagerAdapter {
    public ImagePagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      // Create a new instance of ImageFragment for the given image.
      return ImageFragment.newInstance(searchResult.getImages()[position]);
    }

    @Override
    public int getCount() {
      // Return the search result count.
      if (searchResult == null) {
        return 0;
      }
      return searchResult.getImages().length;
    }
  }

  /** Callback waiting to receive more images for infinite scrolling. */
  private class InfiniteScrollingSearchCallback implements SearchClient.SearchCallback {
    private final SearchResult searchResult;

    /**
     * Create a new InfiniteScrollingSearchCallback.
     *
     * @param searchResult Search result to append new results to.
     */
    public InfiniteScrollingSearchCallback(SearchResult searchResult) {
      this.searchResult = searchResult;
    }

    @Override
    public void onFailure(IOException e) {
      // Clear the active search callback and hide the progress bar in the action bar.
      searchCallback = null;
      setSupportProgressBarIndeterminateVisibility(false);

      // Display error toast notification to the user.
      Toast.makeText(ImageViewerActivity.this,
          String.format(getString(R.string.toast_infiniteScrollingFetchError),
              e.getLocalizedMessage()), Toast.LENGTH_LONG
      ).show();
    }

    @Override
    public void onSuccess(SearchResult searchResult) {
      // Clear the active search callback and hide the progress bar in the action bar.
      searchCallback = null;
      setSupportProgressBarIndeterminateVisibility(false);

      if (searchResult.getImages().length == 0) {
        // Just mark the current SearchResult as having reached the last page.
        this.searchResult.onLastPage();
      } else {
        // Filter the received SearchResult.
        if (sharedPreferences.contains(getString(R.string.preference_nsfwFilter_key))) {
          // Get filter from shared preferences.
          searchResult.filter(Image.ObscenityRating.arrayFromStrings(
              sharedPreferences.getString(getString(R.string.preference_nsfwFilter_key), "").split(" ")));
        } else {
          // Get default filter from resources.
          searchResult.filter(Image.ObscenityRating.arrayFromStrings(getResources().getStringArray(R.array.preference_nsfwFilter_defaultValues)));
        }
        // Update the search result and notify the ViewPager adapter that the data set has changed.
        this.searchResult.addImages(searchResult.getImages(), searchResult.getCurrentOffset());
        imagePagerAdapter.notifyDataSetChanged();
      }
    }
  }


}
