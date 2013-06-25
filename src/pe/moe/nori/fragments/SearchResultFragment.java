package pe.moe.nori.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import com.actionbarsherlock.app.SherlockFragment;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import pe.moe.nori.R;
import pe.moe.nori.SearchActivity;
import pe.moe.nori.api.Image;
import pe.moe.nori.api.SearchResult;

public class SearchResultFragment extends SherlockFragment {
  /** The {@link GridView} displaying search results */
  private GridView mGridView;
  /** LRU cache used for caching images */
  private LruCache<String, Bitmap> mLruCache = new LruCache<String, Bitmap>(2048) {
    @Override
    protected int sizeOf(String key, Bitmap value) {
      return (int) ((long) value.getRowBytes() * (long) value.getHeight()) / 1024;
    }
  };
  /** An {@link ImageLoader.ImageCache} implementation wrapping the {@link LruCache} for use with Android Volley. */
  private ImageLoader.ImageCache mImageCache = new ImageLoader.ImageCache() {
    @Override
    public Bitmap getBitmap(String url) {
      return mLruCache.get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
      mLruCache.put(url, bitmap);
    }
  };
  /** Android Volley HTTP Request queue. Used for API queries and downloading images. */
  private RequestQueue mRequestQueue;
  /** An image loader handling downloading, caching and putting images into views */
  private ImageLoader mImageLoader;
  /** The {@link SearchResult} currently being displayed in this fragment */
  private SearchResult mSearchResult;

  /** Default constructor */
  public SearchResultFragment() {
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    // Save search result.
    outState.putParcelable("search_result", mSearchResult);
    // Save scroll position
    outState.putInt("scroll_index", mGridView.getFirstVisiblePosition());
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    // Get request queue from parent activity.
    mRequestQueue = ((SearchActivity) getActivity()).mRequestQueue;
    // Create the image loader.
    mImageLoader = new ImageLoader(mRequestQueue, mImageCache);


    if (savedInstanceState != null) {
      // Restore the search results.
      final SearchResult searchResult = savedInstanceState.getParcelable("search_result");
      if (searchResult != null) {
        onSearchResult(searchResult);
        // Restore scroll position.
        mGridView.setSelection(savedInstanceState.getInt("scroll_index"));
      }
    }
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    // Trim image cache.
    mLruCache.trimToSize(64);
  }

  public void onSearchResult(SearchResult searchResult) {
    // Save search results.
    mSearchResult = searchResult;
    // Create and set the adapter for the GridView.
    mGridView.setAdapter(new SearchResultAdapter(mSearchResult));
  }

  /** Remove all items from the {@link GridView}. */
  public void removeAllItems() {
    mGridView.setAdapter(null);
  }

  @Override
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
    // Inflate view and return it.
    mGridView = (GridView) inflater.inflate(R.layout.fragment_search_result, container, false);
    return mGridView;
  }

  /**
   * Adapter used by the {@link GridView}.
   */
  private class SearchResultAdapter extends BaseAdapter {
    SearchResult mSearchResult;

    /**
     * Create the adapter used by the {@link GridView}.
     *
     * @param searchResult Search results
     */
    public SearchResultAdapter(SearchResult searchResult) {
      mSearchResult = searchResult;
    }

    @Override
    public int getCount() {
      return mSearchResult.images.size();
    }

    @Override
    public Image getItem(int position) {
      return mSearchResult.images.get(position);
    }

    @Override
    public long getItemId(int position) {
      // Return API image ID.
      return getItem(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final NetworkImageView networkImageView;

      // Recycle view if possible.
      if (convertView == null) {
        // Create a new view.
        networkImageView = new NetworkImageView(getActivity());
        // Set properties.
        networkImageView.setLayoutParams(new GridView.LayoutParams(mGridView.getColumnWidth(), mGridView.getColumnWidth()));
        networkImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        networkImageView.setDefaultImageResId(R.color.background_image_loading);
      } else {
        // Recycle old view.
        networkImageView = (NetworkImageView) convertView;
      }

      // Set image URL.
      networkImageView.setImageUrl(getItem(position).previewUrl, mImageLoader);

      return networkImageView;
    }
  }
}
