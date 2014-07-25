/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.cuddlesoft.nori.util.NetworkUtils;
import com.cuddlesoft.norilib.Image;
import com.squareup.picasso.Picasso;

/** Fragment used to display images in {@link com.cuddlesoft.nori.ImageViewerActivity}. */
public class ImageFragment extends Fragment {
  /** Bundle identifier used to save the displayed image object in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_IMAGE = "com.cuddlesoft.nori.Image";

  /** Required empty public constructor. */
  public ImageFragment() {
  }

  /**
   * Factory method used to construct new fragments.
   *
   * @param image Image to display in the created fragment.
   * @return Fragment instance displaying the given image.
   */
  @SuppressWarnings("TypeMayBeWeakened")
  public static ImageFragment newInstance(Image image) {
    // Create new instance of the fragment and add the image to the fragment's arguments bundle.
    final ImageFragment fragment = new ImageFragment();
    final Bundle arguments = new Bundle();
    arguments.putParcelable(BUNDLE_ID_IMAGE, image);
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final ImageView view = new ImageView(getActivity());

    // Extract image from arguments bundle.
    Image image = getArguments().getParcelable(BUNDLE_ID_IMAGE);

    // Evaluate the current network conditions to decide whether to load the medium-resolution
    // (sample) version of the image or the original full-resolution one.
    final String imageUrl;
    if (NetworkUtils.shouldFetchImageSamples(getActivity())) {
      imageUrl = image.sampleUrl;
    } else {
      imageUrl = image.fileUrl;
    }

    // Load image into view.
    Picasso.with(getActivity())
        .load(imageUrl)
        .into(view);

    return view;
  }

}
