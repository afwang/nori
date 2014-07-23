/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.cuddlesoft.nori.R;
import com.cuddlesoft.norilib.Image;
import com.cuddlesoft.norilib.SearchResult;
import com.squareup.picasso.Picasso;

/** Shows images from a {@link SearchResult} as a scrollable grid of thumbnails. */
public class SearchResultGridFragment extends Fragment implements AdapterView.OnItemClickListener, AbsListView.OnScrollListener {
  /** Identifier used for saving currently displayed search result in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_RESULT = "com.cuddlesoft.nori.SearchResult";
  /** Interface used for communication with parent class. */
  private OnSearchResultGridFragmentInteractionListener mListener;
  /** Search result displayed by the SearchResultGridFragment. */
  private SearchResult searchResult;
  /** Adapter used by the GridView in this fragment. */
  private BaseAdapter gridAdapter = new BaseAdapter() {
    @Override
    public int getCount() {
      // Return count of images.
      if (searchResult == null) {
        return 0;
      }
      return searchResult.getImages().length;
    }

    @Override
    public Image getItem(int position) {
      // Return image at given position.
      return searchResult.getImages()[position];
    }

    @Override
    public long getItemId(int position) {
      return Long.valueOf(getItem(position).id);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // Get image object at given position.
      Image image = getItem(position);
      // Create image view for given position.
      ImageView imageView = (ImageView) convertView;
      // Get thumbnail size from resources.
      final int thumbnailSize = getResources().getDimensionPixelSize(R.dimen.searchResultGrid_columnWidth);

      // Create a new image, if not recycled.
      if (imageView == null) {
        imageView = new ImageView(getActivity());
        imageView.setLayoutParams(new GridView.LayoutParams(thumbnailSize, thumbnailSize));
      }

      // Load image into view.
      Picasso.with(getActivity())
          .load(image.previewUrl)
          .resize(thumbnailSize, thumbnailSize)
          .centerCrop()
          .into(imageView);

      return imageView;
    }
  };

  /** Required public empty constructor. */
  public SearchResultGridFragment() {
  }

  /**
   * Update the SearchResult displayed by this fragment.
   * @param searchResult Search result. Set to null to hide the current search result.
   */
  public void setSearchResult(SearchResult searchResult) {
    if (searchResult == null) {
      this.searchResult = null;
      gridAdapter.notifyDataSetInvalidated();
    } else {
      this.searchResult = searchResult;
      gridAdapter.notifyDataSetChanged();
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_search_result_grid, container, false);
    // Restore SearchResult from saved instance state to preserve search results across screen rotations.
    if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_ID_SEARCH_RESULT)) {
      searchResult = savedInstanceState.getParcelable(BUNDLE_ID_SEARCH_RESULT);
    }
    // Set adapter for GridView.
    GridView gridView = (GridView) view.findViewById(R.id.image_grid);
    gridView.setAdapter(gridAdapter);
    gridView.setOnScrollListener(this);
    gridView.setOnItemClickListener(this);
    // Return inflated view.
    return view;
  }

  /**
   * Called when an image in the search result {@link android.widget.GridView} is selected.
   *
   * @param image Image selected.
   */
  public void onImageSelected(Image image) {
    if (mListener != null) {
      mListener.onImageSelected(image);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    // Get a reference to parent activity, making sure that it implements the proper callback interface.
    try {
      mListener = (OnSearchResultGridFragmentInteractionListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString()
          + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    // Preserve currently displayed SearchResult.
    if (searchResult != null) {
      outState.putParcelable(BUNDLE_ID_SEARCH_RESULT, searchResult);
    }
  }

  @Override
  public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
    if (mListener != null) {
      // Notify parent activity that image has been clicked.
      mListener.onImageSelected((Image) gridAdapter.getItem(position));
    }
  }

  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {}

  @Override
  public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    // Implement endless scrolling.
    // Fetch more images if near the end of the list and more images are available for the SearchResult.
    if ((totalItemCount - visibleItemCount) <= (firstVisibleItem + 10) && searchResult != null
        && searchResult.hasNextPage() && mListener != null) {
      mListener.fetchMoreImages(searchResult);
    }
  }

  public interface OnSearchResultGridFragmentInteractionListener {
    /**
     * Called when {@link com.cuddlesoft.norilib.Image} in the search result grid is selected by the user.
     *
     * @param image Image selected.
     */
    public void onImageSelected(Image image);

    /**
     * Called when the user scrolls the thumbnail {@link android.widget.GridView} near the end and more images should be fetched
     * to implement "endless scrolling".
     *
     * @param searchResult Search result for which more images should be fetched.
     */
    public void fetchMoreImages(SearchResult searchResult);
  }

}
