package com.cuddlesoft.nori.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cuddlesoft.norilib.Image;
import com.ortiz.touch.TouchImageView;
import com.squareup.picasso.Picasso;

/**
 * Fragment using the {@link com.ortiz.touch.TouchImageView} widget
 * and the Picasso HTTP image loading library to display images.
 */
public class PicassoImageFragment extends ImageFragment {
  /** Widget used to display the image. */
  private TouchImageView imageView;

  /**
   * Factory method used to construct new fragments
   *
   * @param image Image object to display in the created fragment.
   * @return New PicassoImageFragment with the image object appended to its arguments bundle.
   */
  public static PicassoImageFragment newInstance(Image image) {
    // Create a new instance of the fragment.
    PicassoImageFragment fragment = new PicassoImageFragment();

    // Add the image object to the fragment's arguments Bundle.
    Bundle arguments = new Bundle();
    arguments.putParcelable(BUNDLE_ID_IMAGE, image);
    fragment.setArguments(arguments);

    return fragment;
  }

  /** Required public empty constructor. */
  public PicassoImageFragment() {
  }

  @Override
  public boolean canScroll(int direction) {
    return imageView == null || !imageView.canScrollHorizontallyFroyo(direction);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Initialize the ImageView widget.
    imageView = new TouchImageView(getActivity());

    // Load image into the view.
    String imageUrl = shouldLoadImageSamples() ? image.sampleUrl : image.fileUrl;
    Picasso.with(getActivity())
        .load(imageUrl)
        .into(imageView);

    return imageView;
  }
}
