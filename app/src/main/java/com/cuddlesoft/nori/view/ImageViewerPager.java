/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.view;

import android.content.Context;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.cuddlesoft.nori.fragment.ImageFragment;

/**
 * View pager used in {@link com.cuddlesoft.nori.ImageViewerActivity}. Gives touch event precedence to
 * multi-touch events sent to the {@link com.ortiz.touch.TouchImageView} in the contained fragment.
 */
@SuppressWarnings("UnusedDeclaration")
public class ImageViewerPager extends ViewPager {
  /** Gets notified when a {@link android.view.MotionEvent} is intercepted by this view. */
  // Can't use OnGenericMotionEventListener, because it requires API>=12
  private OnMotionEventListener onMotionEventListener;

  public ImageViewerPager(Context context) {
    super(context);
  }

  public ImageViewerPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setOnMotionEventListener(OnMotionEventListener listener) {
    onMotionEventListener = listener;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (onMotionEventListener != null) {
      onMotionEventListener.onMotionEvent(ev);
    }
    return super.onInterceptTouchEvent(ev);
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    // Needed to intercept touch events before the ImageView in the child view has loaded.
    if (onMotionEventListener != null) {
      onMotionEventListener.onMotionEvent(ev);
    }
    return super.onTouchEvent(ev);
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

  public static interface OnMotionEventListener {
    /**
     * Notifies the listener that a touch event has been intercepted.
     *
     * @param ev Motion event intercepted.
     */
    public void onMotionEvent(MotionEvent ev);
  }
}
