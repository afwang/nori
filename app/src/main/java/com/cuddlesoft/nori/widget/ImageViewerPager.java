/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.widget;

import android.content.Context;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

import com.cuddlesoft.nori.fragment.ImageFragment;

/**
 * View pager used in {@link com.cuddlesoft.nori.ImageViewerActivity}. Gives touch event precedence to
 * multi-touch events sent to the {@link com.ortiz.touch.TouchImageView} in the contained fragment.
 */
public class ImageViewerPager extends ViewPager {
  public ImageViewerPager(Context context) {
    super(context);
  }

  public ImageViewerPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean canScrollHorizontally(int direction) {
    // Check if TouchImageView isn't panning a zoomed image and will let us scroll the ViewPager.
    final ImageFragment imageFragment =
        (ImageFragment) ((FragmentPagerAdapter) getAdapter()).getItem(getCurrentItem());
    if (imageFragment != null) {
      return imageFragment.canScroll(direction);
    }
    return super.canScrollHorizontally(direction);
  }
}
