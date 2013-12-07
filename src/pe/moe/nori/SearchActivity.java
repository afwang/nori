package pe.moe.nori;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.*;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.google.analytics.tracking.android.EasyTracker;
import pe.moe.nori.adapters.ServiceDropdownAdapter;
import pe.moe.nori.api.BooruClient;
import pe.moe.nori.api.Image;
import pe.moe.nori.api.SearchResult;
import pe.moe.nori.providers.ServiceSettingsProvider;
import pe.moe.nori.widgets.SquaredNetworkImageView;

import java.util.List;

public class SearchActivity extends Activity implements LoaderManager.LoaderCallbacks<List<ServiceSettingsProvider.ServiceSettings>>, AbsListView.OnScrollListener, SearchView.OnQueryTextListener, AdapterView.OnItemClickListener {
  /** Unique ID for the navigation dropdown {@link Loader} */
  private static final int SERVICE_DROPDOWN_LOADER = 0x00;
  /** Overrides the default behavior to clear the {@link SearchView} when collapsed. */
  private final SearchView.OnClickListener mSearchViewOnClickListener = new SearchView.OnClickListener() {
    @Override
    public void onClick(View v) {
      if (mBooruClient != null && mSearchResult != null
          && !mSearchResult.query.equals(mSharedPreferences.getString("search_default_query", mBooruClient.getDefaultQuery())))
        ((SearchView) v).setQuery(mSearchResult.query, false);
    }
  };
  /** ActionBar navigation dropdown {@link ActionBar.OnNavigationListener} */
  public ActionBar.OnNavigationListener mNavigationCallback = new ActionBar.OnNavigationListener() {

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
      // Creates a new API client from the saved settings in the ServiceDropdownAdapter.
      mServiceSettings = mServiceDropdownAdapter.getItem(itemPosition);
      mBooruClient = ServiceSettingsProvider.ServiceSettings.createClient(mRequestQueue, mServiceSettings);

      // Searches for default query when:
      // * App is first created (mSearchResult == null).
      // * A new service was picked from the dropdown menu (itemId != mPref...).
      if (mBooruClient != null && (mSearchResult == null || itemId != mPreferences.getLong("last_service_dropdown_index", 0)))
        if (itemId == mPreferences.getLong("last_service_dropdown_index", 0) &&
            getIntent() != null && getIntent().getAction() == Intent.ACTION_SEARCH && getIntent().hasExtra("pe.moe.nori.Search.query")) {
          final String query = getIntent().getStringExtra("pe.moe.nori.Search.query");

          doSearch(query);
          mSearchViewItem.expandActionView();
          ((SearchView) mSearchViewItem.getActionView()).setQuery(query, false);
          mSearchViewItem.getActionView().clearFocus();
        } else {
          doSearch(mSharedPreferences.getString("search_default_query", mBooruClient.getDefaultQuery()));
        }

      // Remember dropdown state to be restored when app is relaunched.
      mPreferences.edit().putLong("last_service_dropdown_index", itemId).apply();

      return true;
    }

  };
  /** Android Volley HTTP Request queue used for queuing API requests and image downloads. */
  public RequestQueue mRequestQueue;
  /** Used for passing {@link ServiceSettingsProvider.ServiceSettings} in {@link Intent}s */
  private ServiceSettingsProvider.ServiceSettings mServiceSettings;
  /** {@link GridView} displaying search results */
  private GridView mGridView;
  /** LRU cache used for caching images */
  private LruCache<String, Bitmap> mLruCache = new LruCache<String, Bitmap>(4096) {
    @Override
    protected int sizeOf(String key, Bitmap value) {
      return (int) ((long) value.getRowBytes() * (long) value.getHeight() / 1024);
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
  /** An image loader handling fetching, caching and putting images into views. */
  private ImageLoader mImageLoader;
  /** The {@link SearchResult} currently being displayed in this view. */
  private SearchResult mSearchResult = null;
  /** Android ActionBar */
  private ActionBar mActionBar;
  /** SearchView action item */
  private MenuItem mSearchViewItem;
  /** Persistent data for this activity */
  private SharedPreferences mPreferences;
  /** Persistent data for the entire app */
  private SharedPreferences mSharedPreferences;
  /** Loads API settings from the database */
  private ServiceSettingsProvider mServiceSettingsProvider;
  /** Imageboard API client */
  private BooruClient mBooruClient;
  /** Adapter for the ActionBar navigation dropdown */
  private ServiceDropdownAdapter mServiceDropdownAdapter;
  /** Receiver listening for service settings changes */
  private BroadcastReceiver mSettingsChangeReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      // Reload service dropdown list and force default search.
      mSearchResult = null;
      mSearchAdapter.notifyDataSetChanged();
      getLoaderManager().getLoader(SERVICE_DROPDOWN_LOADER).forceLoad();
    }
  };
  /** Pending API request */
  private Request<SearchResult> mPendingRequest = null;
  /** Listener receiving parsed {@link SearchResult} responses from the API client */
  private Response.Listener<SearchResult> mSearchResultListener = new Response.Listener<SearchResult>() {
    @Override
    public void onResponse(SearchResult response) {
      // Hide progress bar.
      setProgressBarIndeterminateVisibility(false);

      // Clear pending request.
      mPendingRequest = null;

      if (mSearchResult == null) { // New result.
        response.filter(mSharedPreferences.getString("search_safety_rating", getString(R.string.preference_safetyRating_default)));
        mSearchResult = response;
      } else { // Load next page.
        mSearchResult.extend(response, mSharedPreferences.getString("search_safety_rating", getString(R.string.preference_safetyRating_default)));
      }

      mSearchAdapter.notifyDataSetChanged();
    }
  };
  /** Listener receiving errors from the API client */
  private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
    @Override
    public void onErrorResponse(VolleyError error) {
      // Hide progress bar.
      setProgressBarIndeterminateVisibility(false);
      // Log error.
      Log.e("SearchResultFetch", error.toString());
      // Show error notification to the user.
      Toast.makeText(SearchActivity.this, R.string.error_connection, Toast.LENGTH_SHORT).show();
    }
  };
  /** Adapter used by the {@link GridView */
  private BaseAdapter mSearchAdapter = new BaseAdapter() {
    @Override
    public int getCount() {
      return mSearchResult == null ? 0 : mSearchResult.images.size();
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
        networkImageView = new SquaredNetworkImageView(SearchActivity.this);
        // Set properties.
        networkImageView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
        networkImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        networkImageView.setDefaultImageResId(R.color.background_image_loading);
        networkImageView.setErrorImageResId(android.R.color.white);
      } else {
        // Recycle old view.
        networkImageView = (NetworkImageView) convertView;
      }

      // Set image URL.
      networkImageView.setImageUrl(getItem(position).previewUrl, mImageLoader);

      return networkImageView;
    }
  };

  private void doSearch(final String query) {
    // Give up if no API client available.
    if (mBooruClient == null)
      return;
    // Cancel pending request and clear search result.
    if (mPendingRequest != null) {
      mPendingRequest.cancel();
    }
    mSearchResult = null;
    mSearchAdapter.notifyDataSetChanged();
    // Show progress bar.
    setProgressBarIndeterminateVisibility(true);
    // Create and add request to queue.
    mPendingRequest = mBooruClient.searchRequest(query, mSearchResultListener, mErrorListener);
    mRequestQueue.add(mBooruClient.searchRequest(query, mSearchResultListener, mErrorListener));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Request window manager features.
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    // Get action bar.
    mActionBar = getActionBar();
    mActionBar.setIcon(R.drawable.ic_activity_default);
    // Register broadcast receivers.
    registerReceiver(mSettingsChangeReceiver, new IntentFilter("pe.moe.nori.providers.ServiceSettingsProvider.update"));
    // Get service settings provider.
    mServiceSettingsProvider = new ServiceSettingsProvider(this);
    // Get shared preferences.
    mPreferences = getPreferences(MODE_PRIVATE);
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    // Prepare Volley request queue and ImageLoader.
    mRequestQueue = Volley.newRequestQueue(this);
    mImageLoader = new ImageLoader(mRequestQueue, mImageCache);
    // Inflate views.
    setContentView(R.layout.activity_search);
    setProgressBarIndeterminateVisibility(false);
    // Get GridView and set adapter.
    mGridView = (GridView) findViewById(R.id.result_grid);
    mGridView.setAdapter(mSearchAdapter);
    mGridView.setOnScrollListener(this);
    mGridView.setOnItemClickListener(this);
    // Get loader manager and setup navigation dropdown.
    final LoaderManager mLoaderManager = getLoaderManager();
    mLoaderManager.initLoader(SERVICE_DROPDOWN_LOADER, null, this).forceLoad();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (intent != null && intent.getAction().equals(Intent.ACTION_SEARCH) && intent.hasExtra("pe.moe.nori.Search.query")) {
      final String query = intent.getStringExtra("pe.moe.nori.Search.query");
      doSearch(query);
      mSearchViewItem.expandActionView();
      ((SearchView) mSearchViewItem.getActionView()).setQuery(query, false);
      mSearchViewItem.getActionView().clearFocus();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    EasyTracker.getInstance().activityStart(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    EasyTracker.getInstance().activityStop(this);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    // Save search result.
    outState.putParcelable("search_result", mSearchResult);
    // Save scroll position.
    outState.putInt("scroll_position", mGridView.getFirstVisiblePosition());
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    // Trim image cache.
    mLruCache.trimToSize(64);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // Unregister broadcast receivers.
    unregisterReceiver(mSettingsChangeReceiver);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    // Restore search result.
    mSearchResult = savedInstanceState.getParcelable("search_result");
    mSearchAdapter.notifyDataSetChanged();
    // Restore scroll position.
    mGridView.setSelection(savedInstanceState.getInt("scroll_position"));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);

    // Inflate menu.
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.search, menu);
    // Find SearchView.
    mSearchViewItem = menu.findItem(R.id.action_search);
    SearchView mSearchView = (SearchView) mSearchViewItem.getActionView();
    // Disable dictionary suggestions.
    mSearchView.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
    // Set listeners.
    mSearchView.setOnQueryTextListener(this);
    mSearchView.setOnSearchClickListener(mSearchViewOnClickListener);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_help:
        // Start help activity.
        startActivity(new Intent(this, HelpActivity.class));
        return true;
      case R.id.action_settings:
        // Start settings activity.
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
      default:
        return false;
    }
  }

  @Override
  public Loader<List<ServiceSettingsProvider.ServiceSettings>> onCreateLoader(final int i, final Bundle bundle) {
    if (i == SERVICE_DROPDOWN_LOADER) { // ActionBar navigation dropdown loader.
      return mServiceSettingsProvider.getServiceSettingsLoader();
    }
    return null;
  }

  @Override
  public void onLoadFinished(final Loader<List<ServiceSettingsProvider.ServiceSettings>> listLoader, List<ServiceSettingsProvider.ServiceSettings> serviceSettings) {
    if (listLoader.getId() == SERVICE_DROPDOWN_LOADER) {
      // Hide activity title and switch into list navigation mode.
      mActionBar.setDisplayShowTitleEnabled(false);
      mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

      // Create the adapter for the navigation dropdown.
      mServiceDropdownAdapter = new ServiceDropdownAdapter(this, serviceSettings);
      mActionBar.setListNavigationCallbacks(mServiceDropdownAdapter, mNavigationCallback);

      // Select last item.
      final int lastSelectedIndex = mServiceDropdownAdapter.getPositionByItemId(mPreferences.getLong("last_service_dropdown_index", 0L));
      if (lastSelectedIndex != -1) {
        mActionBar.setSelectedNavigationItem(lastSelectedIndex);
      }
    }
  }

  @Override
  public void onLoaderReset(final Loader<List<ServiceSettingsProvider.ServiceSettings>> listLoader) {
    // Restore navigation mode and show activity title.
    mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    mActionBar.setDisplayShowTitleEnabled(true);
  }

  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {

  }

  @Override
  public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    // Fetch more images if near end of list and there is no other pending API request.
    if (mPendingRequest == null && ((totalItemCount - visibleItemCount)) <= (firstVisibleItem + 10)) {
      // Give up if no API client, no search result or is at last page.
      if (mBooruClient == null || mSearchResult == null || !mSearchResult.hasMore())
        return;
      // Create API request and add it to queue.
      setProgressBarIndeterminateVisibility(true);
      mPendingRequest = mBooruClient.searchRequest(mSearchResult.query, mSearchResult.pageNumber + 1, mSearchResultListener, mErrorListener);
      mRequestQueue.add(mPendingRequest);
    }
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    // Clear focus from SearchView and start search.
    doSearch(query);
    mSearchViewItem.getActionView().clearFocus();
    return true;
  }

  @Override
  public boolean onQueryTextChange(String newText) {
    return false;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    // Pass the SearchResult, ServiceSettings and position to ImageViewerActivity.
    Intent intent = new Intent(this, ImageViewerActivity.class);

    intent.putExtra("pe.moe.nori.api.SearchResult", mSearchResult);
    intent.putExtra("pe.moe.nori.api.Settings", mServiceSettings);
    intent.putExtra("pe.moe.nori.api.SearchResult.position", position);
    startActivity(intent);
  }
}
