/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

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
  protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
    // Make sure the ImageViewerFragment isn't currently panning a zoomed in image.
    // #instantiateItem() seems to be the "least awful" way to get the current item.
    ImageFragment imageFragment = (ImageFragment) getAdapter().instantiateItem(null, getCurrentItem());
    if (imageFragment != null) {
      return imageFragment.canScroll(-dx);
    }
    return super.canScroll(v, checkV, dx, x, y);
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
