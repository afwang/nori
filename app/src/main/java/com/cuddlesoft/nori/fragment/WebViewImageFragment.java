package com.cuddlesoft.nori.fragment;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.cuddlesoft.norilib.Image;

/**
 * Fragment used to display images not supported by the standard {@link android.widget.ImageView} widget
 * (such as animated GIFs).
 */
public class WebViewImageFragment extends ImageFragment implements View.OnTouchListener {
  /** Detects single tap gestures to toggle visibility of the ActionBar in the parent activity. */
  private GestureDetector gestureDetector;
  /** Listens for single tap events from the GestureDetector. */
  private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
      // Notify the parent activity about the event.
      listener.onSingleTapConfirmed();
      return false;
    }
  };

  /**
   * Factory method used to construct new fragments.
   *
   * @param image Image object to display in the created fragment.
   * @return New WebViewImageFragment with the image object appended to its arguments bundle.
   */
  public static WebViewImageFragment newInstance(Image image) {
    // Create a new instance of the fragment.
    WebViewImageFragment fragment = new WebViewImageFragment();

    // Add the image object to its arguments Bundle.
    Bundle arguments = new Bundle();
    arguments.putParcelable(BUNDLE_ID_IMAGE, image);
    fragment.setArguments(arguments);

    return fragment;
  }

  /** Required empty public constructor. */
  public WebViewImageFragment() {
  }

  @Override
  public boolean canScroll(int direction) {
    return true;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Initialize the WebView widget.
    WebView webView = new WebView(getActivity());
    webView.setBackgroundColor(0);
    webView.setBackgroundResource(android.R.color.black);
    WebSettings webSettings = webView.getSettings();
    webSettings.setUseWideViewPort(true);
    webSettings.setLoadWithOverviewMode(true);

    // Initialize the GestureDetector.
    gestureDetector = new GestureDetector(getActivity(), gestureListener);
    webView.setOnTouchListener(this);

    // Load image into the WebView.
    String imageUrl = shouldLoadImageSamples() ? image.sampleUrl : image.fileUrl;
    webView.loadUrl(imageUrl);

    return webView;
  }

  @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {
    // Intercept all touch events.
    gestureDetector.onTouchEvent(motionEvent);
    return false;
  }
}
