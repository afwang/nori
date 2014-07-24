/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;

import com.cuddlesoft.nori.fragment.ImageFragment;
import com.cuddlesoft.norilib.Image;
import com.cuddlesoft.norilib.SearchResult;
import com.cuddlesoft.norilib.Tag;

/** Activity used to display full-screen images. */
public class ImageViewerActivity extends ActionBarActivity implements ViewPager.OnPageChangeListener {
  // TODO: Endless scrolling.
  // TODO: Pinch to zoom.
  // TODO: Download image.
  // TODO: View image on web.
  // TODO: View image on pixiv.
  // TODO: Set image as wallpaper.
  /** Identifier used to keep the displayed {@link com.cuddlesoft.norilib.SearchResult} in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_RESULT = "com.cuddlesoft.nori.SearchResult";
  /** Identifier used to keep the position of the selected {@link com.cuddlesoft.norilib.Image} in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_IMAGE_INDEX = "com.cuddlesoft.nori.ImageIndex";
  /** View pager used to display the images. */
  private ViewPager viewPager;
  /** Search result shown by the {@link android.support.v4.app.FragmentStatePagerAdapter}. */
  private SearchResult searchResult;
  /** Adapter used to populate the {@link android.support.v4.view.ViewPager} used to display and flip through the images. */
  private ImagePagerAdapter imagePagerAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Get data out of Intent sent by SearchActivity or restore them from the saved instance
    // state.
    final int imageIndex;
    if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_ID_IMAGE_INDEX) &&
        savedInstanceState.containsKey(BUNDLE_ID_SEARCH_RESULT)) {
      imageIndex = savedInstanceState.getInt(BUNDLE_ID_IMAGE_INDEX);
      searchResult = savedInstanceState.getParcelable(BUNDLE_ID_SEARCH_RESULT);
    } else {
      final Intent intent = getIntent();
      imageIndex = intent.getIntExtra(SearchActivity.BUNDLE_ID_IMAGE_INDEX, 0);
      searchResult = intent.getParcelableExtra(SearchActivity.BUNDLE_ID_SEARCH_RESULT);
    }

    // Populate content view.
    setContentView(R.layout.activity_image_viewer);

    // Create and set the image viewer Fragment pager adapter.
    imagePagerAdapter = new ImagePagerAdapter(getSupportFragmentManager());
    viewPager = (ViewPager) findViewById(R.id.image_pager);
    viewPager.setAdapter(imagePagerAdapter);
    viewPager.setOnPageChangeListener(this);
    viewPager.setCurrentItem(imageIndex);
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

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    // Keep search result and the index of currently displayed image.
    outState.putParcelable(BUNDLE_ID_SEARCH_RESULT, searchResult);
    outState.putInt(BUNDLE_ID_IMAGE_INDEX, viewPager.getCurrentItem());
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
  }

  @Override
  public void onPageSelected(int position) {
    // Set activity title to image metadata.
    setTitle(searchResult.getImages()[position]);
  }

  @Override
  public void onPageScrollStateChanged(int state) {
  }

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
}
