/*
 Copyright (c) 2012 Roman Truba

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial
 portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package pe.moe.nori.widgets;

import android.content.Context;
import android.graphics.PointF;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.actionbarsherlock.app.ActionBar;
import pe.moe.nori.ImageViewerActivity;

/** This class implements method to help <b>TouchImageView</b> fling, draggin and scaling. */
public class TouchImageViewPager extends ViewPager {
  public TouchNetworkImageView mCurrentView;
  PointF last;

  public TouchImageViewPager(Context context) {
    super(context);
  }

  public TouchImageViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  private void toggleActionBar() {
    if (getContext() instanceof ImageViewerActivity) {
      ActionBar actionBar = ((ImageViewerActivity) getContext()).getSupportActionBar();
      if (actionBar.isShowing())
        actionBar.hide();
      else
        actionBar.show();
    }
  }

  private float[] handleMotionEvent(MotionEvent event) {
    switch (event.getAction() & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN:
        last = new PointF(event.getX(0), event.getY(0));
        break;
      case MotionEvent.ACTION_MOVE:
      case MotionEvent.ACTION_UP:
        PointF curr = new PointF(event.getX(0), event.getY(0));
        return new float[]{curr.x - last.x, curr.y - last.y};

    }
    return null;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
      try {
        super.onTouchEvent(event);
      } catch (IllegalArgumentException e) {

      }
    }

    float[] difference = handleMotionEvent(event);

    try {
      if (mCurrentView.pagerCanScroll()) {
        return super.onTouchEvent(event);
      } else {
        if (difference != null && mCurrentView.onRightSide && difference[0] < 0) //move right
        {
          return super.onTouchEvent(event);
        }
        if (difference != null && mCurrentView.onLeftSide && difference[0] > 0) //move left
        {
          return super.onTouchEvent(event);
        }
        if (difference == null && (mCurrentView.onLeftSide || mCurrentView.onRightSide)) {
          return super.onTouchEvent(event);
        }
      }
    } catch (IllegalArgumentException e) {

    }

    return false;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
      try {
        super.onInterceptTouchEvent(event);
      } catch (IllegalArgumentException e) {

      }
    }

    float[] difference = handleMotionEvent(event);

    if (difference != null && difference[0] == 0 && difference[1] == 0)
      toggleActionBar();

    try {
      if (mCurrentView.pagerCanScroll() || !mCurrentView.hasBitmap) {
        return super.onInterceptTouchEvent(event);
      } else {
        if (difference != null && mCurrentView.onRightSide && difference[0] < 0) //move right
        {
          return super.onInterceptTouchEvent(event);
        }
        if (difference != null && mCurrentView.onLeftSide && difference[0] > 0) //move left
        {
          return super.onInterceptTouchEvent(event);
        }
        if (difference == null && (mCurrentView.onLeftSide || mCurrentView.onRightSide)) {
          return super.onInterceptTouchEvent(event);
        }
      }
    } catch (IllegalArgumentException e) {

    }

    return false;
  }
}