/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.cuddlesoft.nori.widget.SquareImageView;
import com.cuddlesoft.norilib.Image;
import com.cuddlesoft.norilib.SearchResult;
import com.squareup.picasso.Picasso;

/** Shows images from a {@link SearchResult} as a scrollable grid of thumbnails. */
public class SearchResultGridFragment extends Fragment implements AdapterView.OnItemClickListener, AbsListView.OnScrollListener {
  /** Identifier used for saving currently displayed search result in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_RESULT = "com.cuddlesoft.nori.SearchResult";
  /** Interface used for communication with parent class. */
  private OnSearchResultGridFragmentInteractionListener mListener;
  /** GridView used to display the thumbnails. */
  private GridView gridView;
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

      // Create a new image, if not recycled.
      if (imageView == null) {
        imageView = new SquareImageView(getActivity());
        imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      }

      int previewSize;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        // Resize thumbnails to actual GridView column width on Jelly Bean and above.
        previewSize = gridView.getColumnWidth();
      } else {
        // Fallback to requested column width on older versions.
        previewSize = getGridViewColumnWidth();
      }

      // Load image into view.
      Picasso.with(getActivity())
          .load(image.previewUrl)
          .resize(previewSize, previewSize)
          .centerCrop()
          .placeholder(R.color.network_thumbnail_placeholder)
          .into(imageView);

      return imageView;
    }
  };

  /** Required public empty constructor. */
  public SearchResultGridFragment() {
  }

  /**
   * Get search result displayed by this fragment.
   *
   * @return Search result shown in this fragment.
   */
  public SearchResult getSearchResult() {
    return this.searchResult;
  }

  /**
   * Update the SearchResult displayed by this fragment.
   *
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
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_search_result_grid, container, false);
    // Restore SearchResult from saved instance state to preserve search results across screen rotations.
    if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_ID_SEARCH_RESULT)) {
      searchResult = savedInstanceState.getParcelable(BUNDLE_ID_SEARCH_RESULT);
    }
    // Set adapter for GridView.
    gridView = (GridView) view.findViewById(R.id.image_grid);
    gridView.setColumnWidth(getGridViewColumnWidth());
    gridView.setAdapter(gridAdapter);
    gridView.setOnScrollListener(this);
    gridView.setOnItemClickListener(this);

    // Return inflated view.
    return view;
  }

  /**
   * Get the grid view column size from the thumbnail size shared preference.
   *
   * @return Minimum column size, in pixels.
   */
  private int getGridViewColumnWidth() {
    // Get preference value from SharedPreference.
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    String previewSize = sharedPreferences.getString(getString(R.string.preference_previewSize_key),
        getString(R.string.preference_previewSize_default));

    // Return dimension for given preference value, in pixels.
    switch (previewSize) {
      case "small":
        return getResources().getDimensionPixelSize(R.dimen.previewSize_small);
      case "medium":
        return getResources().getDimensionPixelSize(R.dimen.previewSize_medium);
      case "large":
        return getResources().getDimensionPixelSize(R.dimen.previewSize_large);
      default:
        return getResources().getDimensionPixelSize(R.dimen.previewSize_medium);
    }

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
      mListener.onImageSelected((Image) gridAdapter.getItem(position), position);
    }
  }

  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {
  }

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
     * @param image    Image selected.
     * @param position Index of the image in the {@link SearchResult}.
     */
    public void onImageSelected(Image image, int position);

    /**
     * Called when the user scrolls the thumbnail {@link android.widget.GridView} near the end and more images should be fetched
     * to implement "endless scrolling".
     *
     * @param searchResult Search result for which more images should be fetched.
     */
    public void fetchMoreImages(SearchResult searchResult);
  }

}
