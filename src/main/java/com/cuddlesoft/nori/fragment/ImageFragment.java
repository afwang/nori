/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.cuddlesoft.nori.R;
import com.cuddlesoft.nori.util.NetworkUtils;
import com.cuddlesoft.norilib.Image;
import com.ortiz.touch.TouchImageView;
import com.squareup.picasso.Picasso;

/** Fragment used to display images in {@link com.cuddlesoft.nori.ImageViewerActivity}. */
public class ImageFragment extends Fragment {
  /** Bundle identifier used to save the displayed image object in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_IMAGE = "com.cuddlesoft.nori.Image";
  /** Image view used to show the image in this fragment. */
  private TouchImageView imageView;
  /** Image object displayed in this fragment. */
  private Image image;

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

  /**
   * Check if the {@link android.support.v4.view.ViewPager} containing this fragment can scroll horizontally.
   *
   * @param direction Direction being scrolled in.
   * @return true if the {@link android.support.v4.view.ViewPager} containing this fragment can scroll horizontally.
   */
  public boolean canScroll(int direction) {
    return imageView == null || imageView.canScrollHorizontallyFroyo(direction);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Extract image from arguments bundle and initialize ImageView.
    imageView = new TouchImageView(getActivity());
    image = getArguments().getParcelable(BUNDLE_ID_IMAGE);

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
        .into(imageView);

    // Enable options menu.
    setHasOptionsMenu(true);

    return imageView;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.image, menu);

    // Set up ShareActionProvider
    MenuItem shareItem = menu.findItem(R.id.action_share);
    ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
    shareActionProvider.setShareIntent(getShareIntent());
  }

  /**
   * Get {@link android.content.Intent} to be sent by the {@link android.support.v7.widget.ShareActionProvider}.
   * @return Share intent.
   */
  protected Intent getShareIntent() {
    // Send web URL to image.
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_TEXT, image.webUrl);
    intent.setType("text/plain");
    return intent;
  }

}
