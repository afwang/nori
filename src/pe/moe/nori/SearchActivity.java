/**
 * This file is part of Nori.
 * Copyright (c) 2013 Obscure Reference
 * License: GPLv3
 */
package pe.moe.nori;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.Window;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import pe.moe.nori.adapters.ServiceDropdownAdapter;
import pe.moe.nori.api.BooruClient;
import pe.moe.nori.api.SearchResult;
import pe.moe.nori.fragments.SearchResultFragment;
import pe.moe.nori.providers.ServiceSettingsProvider;

import java.util.List;

public class SearchActivity extends SherlockFragmentActivity implements LoaderManager.LoaderCallbacks<List<ServiceSettingsProvider.ServiceSettings>> {
  /** Unique ID for the navigation dropdown {@link Loader} */
  private static final int SERVICE_DROPDOWN_LOADER = 0x00;

  /** ActionBar navigation dropdown {@link ActionBar.OnNavigationListener} */
  public ActionBar.OnNavigationListener mNavigationCallback = new ActionBar.OnNavigationListener() {

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
      // Creates a new API client from the saved settings in the ServiceDropdownAdapter.
      mBooruClient = ServiceSettingsProvider.ServiceSettings.createClient(mRequestQueue,
          mServiceDropdownAdapter.getItem(itemPosition));

      // Searches for default query when:
      // * App is first created (mLastResultSize == 0).
      // * Activity is restored from savedInstanceState and last search had no results (mLastResultSize == 0).
      // * A new service was picked from the dropdown menu (itemId != mPref...).
      if (mBooruClient != null && (mLastResultSize == 0 || itemId != mPreferences.getLong("last_service_dropdown_index", 0)))
        doSearch(mSharedPreferences.getString("default_query", mBooruClient.getDefaultQuery()));

      // Remember dropdown state to be restored when app is relaunched.
      mPreferences.edit().putLong("last_service_dropdown_index", itemId).apply();

      return true;
    }

  };

  /** Android Volley HTTP Request queue used for queuing API requests and image downloads. */
  public RequestQueue mRequestQueue;
  /** Last result size. Used when deciding to retain {@link SearchResult}s from previous instance */
  public long mLastResultSize = 0; // Used for saving instance state.
  /** Android ActionBar */
  private ActionBar mActionBar;
  /** Persistent data for this activity */
  private SharedPreferences mPreferences;
  /** Persistent data for the entire app */
  private SharedPreferences mSharedPreferences;
  /** LoaderManager used to query settings database asynchronously */
  private LoaderManager mLoaderManager;
  /** Loads API settings from the database */
  private ServiceSettingsProvider mServiceSettingsProvider;
  /** Imageboard API client */
  private BooruClient mBooruClient;
  /** Adapter for the ActionBar navigation dropdown */
  private ServiceDropdownAdapter mServiceDropdownAdapter;
  /** Listener receiving parsed {@link SearchResult} responses from the API client */
  private Response.Listener<SearchResult> mSearchResultListener = new Response.Listener<SearchResult>() {
    @Override
    public void onResponse(SearchResult response) {
      // Hide progress bar.
      setProgressBarIndeterminateVisibility(false);

      SearchResultFragment searchResultFragment = (SearchResultFragment) getSupportFragmentManager().findFragmentById(R.id.result_fragment);
      if (searchResultFragment != null) {
        if (response != null) mLastResultSize = response.images.size();
        else mLastResultSize = 0;
        searchResultFragment.onSearchResult(response);
      }
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

  private void doSearch(final String query) {
    // Give up if no API client available.
    if (mBooruClient == null)
      return;
    // Restarts and clears the request queue.
    mRequestQueue.start();
    // Show progress bar.
    setProgressBarIndeterminateVisibility(true);
    // Add request to queue.
    mRequestQueue.add(mBooruClient.searchRequest(query, mSearchResultListener, mErrorListener));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Request window manager features.
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    // Get action bar.
    mActionBar = getSupportActionBar();
    // Get service settings provider.
    mServiceSettingsProvider = new ServiceSettingsProvider(this);
    // Get shared preferences.
    mPreferences = getPreferences(MODE_PRIVATE);
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    // Prepare Volley request queue.
    mRequestQueue = Volley.newRequestQueue(this);
    // Get loader manager and setup navigation dropdown.
    mLoaderManager = getSupportLoaderManager();
    mLoaderManager.initLoader(SERVICE_DROPDOWN_LOADER, null, this).forceLoad();
    // Inflate views.
    setContentView(R.layout.activity_search);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    // Save last result size.
    // This is used to decide whether to retain results from a previous instance.
    outState.putLong("last_result_size", mLastResultSize);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    // Restore last result size.
    // This is used to decide whether to retain results from a previous instance.
    mLastResultSize = savedInstanceState.getLong("last_result_size");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);

    // Inflate menu.
    final MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.search, menu);
    // TODO: SearchView

    return true;
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
}
