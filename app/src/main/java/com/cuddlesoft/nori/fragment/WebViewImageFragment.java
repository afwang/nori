package com.cuddlesoft.nori.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cuddlesoft.nori.R;
import com.cuddlesoft.norilib.Image;

import java.util.Locale;

/**
 * Fragment used to display images not supported by the standard {@link android.widget.ImageView} widget
 * (such as animated GIFs).
 */
public class WebViewImageFragment extends ImageFragment {

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

    // Zoom out the view port to display the entire image by default.
    WebSettings webSettings = webView.getSettings();
    webSettings.setBuiltInZoomControls(false);
    webSettings.setUseWideViewPort(true);
    webSettings.setLoadWithOverviewMode(true);

    // Escape HTML entities from the image URL.
    String imageUrl = TextUtils.htmlEncode(shouldLoadImageSamples() ? image.sampleUrl : image.fileUrl);
    // Load the HTML to display the image in the center of the view.
    webView.loadData(String.format(Locale.US, "<center><img src=\"%s\" alt=\"\" /></center>", imageUrl),
        "text/html", "utf-8");

    return view;
  }
}
