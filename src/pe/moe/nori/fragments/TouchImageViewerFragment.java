/*
 * This file is part of Nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: MIT
 */

package pe.moe.nori.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import pe.moe.nori.api.Image;
import pe.moe.nori.widgets.TouchImageView;
import pe.moe.nori.widgets.UrlTouchImageView;

public class TouchImageViewerFragment extends Fragment {

  private final Image image;
  private TouchImageView imageView;

  public TouchImageViewerFragment(Image image) {
    this.image = image;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final UrlTouchImageView iv = new UrlTouchImageView(getActivity());
    iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    iv.setUrl(image);
    imageView = iv.getImageView();
    return iv;
  }

  public TouchImageView getImageView() {
    return imageView;
  }
}
