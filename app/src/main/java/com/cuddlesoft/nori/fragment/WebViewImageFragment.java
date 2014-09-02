/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.cuddlesoft.nori.R;
import com.cuddlesoft.norilib.Image;

import java.util.Locale;

/**
 * Fragment used to display images not supported by the standard {@link android.widget.ImageView} widget
 * (such as animated GIFs).
 */
public class WebViewImageFragment extends ImageFragment {
  /** String format used to create HTML used to display the images. */
  private static final String IMAGE_HTML = "<!DOCTYPE html>" +
      "<html><head>" +
      "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">" +
      "<title></title>" +
      "<style>img { max-width:100%%; max-height:100%%; bottom:0; left:0; margin: auto; overflow: auto; position: absolute; right: 0; top: 0; }</style>" +
      "</head><body>" +
      "<img src=\"%s\" alt=\"\" />" +
      "</body></html>";

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
    return false;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the WebView widget.
    View view = inflater.inflate(R.layout.fragment_image_viewer_webview, container, false);
    WebView webView = (WebView) view.findViewById(R.id.webView);

    // Set background color to black.
    webView.setBackgroundColor(0);
    webView.setBackgroundResource(android.R.color.transparent);
    // Disable over-scroll effect.
    webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

    // Zoom out the view port to display the entire image by default.
    WebSettings webSettings = webView.getSettings();
    webSettings.setBuiltInZoomControls(false);

    // Escape HTML entities from the image URL.
    String imageUrl = TextUtils.htmlEncode(shouldLoadImageSamples() ? image.sampleUrl : image.fileUrl);
    // Load the HTML to display the image in the center of the view.
    webView.loadDataWithBaseURL(null, String.format(Locale.US, IMAGE_HTML, imageUrl),
        "text/html", "utf-8", null);

    return view;
  }
}
