/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.cuddlesoft.nori.R;
import com.cuddlesoft.nori.SearchActivity;
import com.cuddlesoft.norilib.Image;
import com.cuddlesoft.norilib.Tag;
import com.cuddlesoft.norilib.clients.SearchClient;

/** Dialog showing a list of tags for given image in {@link com.cuddlesoft.nori.ImageViewerActivity}. */
public class TagListDialogFragment extends DialogFragment implements ListView.OnItemClickListener {
  /** Identifier used for the parceled {@link com.cuddlesoft.norilib.clients.SearchClient.Settings} object in this fragment's argument bundle. */
  private static final String BUNDLE_ID_SEARCH_CLIENT_SETTINGS = "com.cuddlesoft.nori.SearchClient.Settings";
  /** Identifier used for the parceled {@link com.cuddlesoft.norilib.Image} object in this fragment's argument bundle. */
  private static final String BUNDLE_ID_IMAGE = "com.cuddlesoft.nori.Image";
  /** The image object containing the tags to show in this fragment. */
  private Image image;
  /** Search client settings object included in {@link android.content.Intent}s to launch {@link com.cuddlesoft.nori.SearchActivity}. */
  private SearchClient.Settings settings;

  /** Required empty constructor. */
  public TagListDialogFragment() {
  }

  /**
   * Factory method to create a new TagListDialogFragment for given {@link com.cuddlesoft.norilib.Image}.
   *
   * @param image Image to display tags from.
   * @param settings Settings object included in SearchActivity launch intents sent from this dialog.
   * @return A new instance of TagListDialogFragment.
   */
  @SuppressWarnings("TypeMayBeWeakened")
  public static TagListDialogFragment newInstance(Image image, SearchClient.Settings settings) {
    // Package image object into the fragment's argument bundle.
    TagListDialogFragment fragment = new TagListDialogFragment();
    Bundle arguments = new Bundle();
    arguments.putParcelable(BUNDLE_ID_IMAGE, image);
    arguments.putParcelable(BUNDLE_ID_SEARCH_CLIENT_SETTINGS, settings);
    fragment.setArguments(arguments);

    return fragment;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // Extract data from the arguments bundle.
    image = getArguments().getParcelable(BUNDLE_ID_IMAGE);
    settings = getArguments().getParcelable(BUNDLE_ID_SEARCH_CLIENT_SETTINGS);

    // Create and initialize the ListView.
    final ListView listView = new ListView(getActivity());
    listView.setOnItemClickListener(this);
    listView.setAdapter(new TagListAdapter());

    return new AlertDialog.Builder(getActivity())
        .setView(listView)
        .setPositiveButton(R.string.dialog_tags_closeButton, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            dismiss();
          }

        }).create();
  }

  @Override
  public void onItemClick(AdapterView<?> adapterView, View clickedView, int position, long itemId) {
    // Start SearchActivity with search for the given tag.
    final Intent intent = new Intent(getActivity(), SearchActivity.class);
    intent.setAction(Intent.ACTION_SEARCH);
    intent.putExtra(SearchActivity.INTENT_EXTRA_SEARCH_CLIENT_SETTINGS, settings);
    intent.putExtra(SearchActivity.INTENT_EXTRA_SEARCH_QUERY, image.tags[position].getName());
    startActivity(intent);

    // Dismiss the dialog after the activity is started.
    dismiss();
  }

  /**
   * Adapter used to show the tag list inside a {@link android.widget.ListView}.
   */
  private class TagListAdapter extends BaseAdapter {
    @Override
    public int getCount() {
      if (image == null || image.tags == null) {
        return 0;
      }
      return image.tags.length;
    }

    @Override
    public Tag getItem(int position) {
      return image.tags[position];
    }

    @Override
    public long getItemId(int position) {
      // Item ID == position.
      return position;
    }

    @Override
    public View getView(int position, View recycleView, ViewGroup container) {
      // Get tag object at given position.
      final Tag tag = getItem(position);

      // Recycle old view if possible.
      TextView textView = (TextView) recycleView;
      if (textView == null) {
        // Inflate new view from XML.
        final LayoutInflater inflater = getLayoutInflater(null);
        textView = (TextView) inflater.inflate(R.layout.listitem_tag, container, false);
      }

      // Set tag name.
      textView.setText(tag.getName());
      // Set appropriate colour for the tag type.
      switch (tag.getType()) {
        case GENERAL:
          textView.setTextColor(getResources().getColor(R.color.taglist_general));
          break;
        case ARTIST:
          textView.setTextColor(getResources().getColor(R.color.taglist_artist));
          break;
        case CHARACTER:
          textView.setTextColor(getResources().getColor(R.color.taglist_character));
          break;
        case COPYRIGHT:
          textView.setTextColor(getResources().getColor(R.color.taglist_copyright));
          break;
      }

      return textView;
    }
  }

}
