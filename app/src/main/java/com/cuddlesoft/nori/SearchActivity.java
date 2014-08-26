/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.cuddlesoft.nori.database.APISettingsDatabase;
import com.cuddlesoft.nori.fragment.SearchResultGridFragment;
import com.cuddlesoft.norilib.Image;
import com.cuddlesoft.norilib.SearchResult;
import com.cuddlesoft.norilib.Tag;
import com.cuddlesoft.norilib.clients.SearchClient;

import java.io.IOException;
import java.util.List;

import io.github.vomitcuddle.SearchViewAllowEmpty.SearchView;

/** Searches for images and displays the results in a scrollable grid of thumbnails. */
public class SearchActivity extends ActionBarActivity implements SearchResultGridFragment.OnSearchResultGridFragmentInteractionListener {
  /** Identifier used to send the active {@link com.cuddlesoft.norilib.SearchResult} to {@link com.cuddlesoft.nori.ImageViewerActivity}. */
  public static final String BUNDLE_ID_SEARCH_RESULT = "com.cuddlesoft.nori.SearchResult";
  /** Identifier used to send the position of the selected {@link com.cuddlesoft.norilib.Image} to {@link com.cuddlesoft.nori.ImageViewerActivity}. */
  public static final String BUNDLE_ID_IMAGE_INDEX = "com.cuddlesoft.nori.ImageIndex";
  /** Identifier used to send {@link com.cuddlesoft.norilib.clients.SearchClient} settings to {@link com.cuddlesoft.nori.ImageViewerActivity}. */
  public static final String BUNDLE_ID_SEARCH_CLIENT_SETTINGS = "com.cuddlesoft.nori.SearchClient.Settings";
  /** Identifier used for the query string to search when starting this activity with an {@link android.content.Intent} */
  public static final String INTENT_EXTRA_SEARCH_QUERY = "com.cuddlesoft.nori.SearchQuery";
  /** Identifier used to include {@link SearchClient.Settings} objects in search intents. */
  public static final String INTENT_EXTRA_SEARCH_CLIENT_SETTINGS = "com.cuddlesoft.nori.SearchClient.Settings";
  /** Identifier used to preserve current search query in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_QUERY = "com.cuddlesoft.nori.SearchQuery";
  /** Identifier used to preserve iconified/expanded state of the SearchView in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_VIEW_IS_EXPANDED = "com.cuddlesoft.nori.SearchView.isExpanded";
  /** Identifier used to preserve search view focused state. */
  private static final String BUNDLE_ID_SEARCH_VIEW_IS_FOCUSED = "com.cuddlesoft.nori.SearchView.isFocused";
  /** Default {@link android.content.SharedPreferences} object. */
  private SharedPreferences sharedPreferences;
  /* {@link SearchClient.Settings} object selected from the service dropdown menu. */
  private SearchClient.Settings searchClientSettings;
  /** Search API client. */
  private SearchClient searchClient;
  /** Search view menu item. */
  private MenuItem searchMenuItem;
  /** Action bar search view. */
  private SearchView searchView;
  /** Search callback currently awaiting a response from the Search API. */
  private SearchResultCallback searchCallback;
  /** Search result grid fragment shown in this activity. */
  private SearchResultGridFragment searchResultGridFragment;
  /** Bundle used when restoring saved instance state (after screen rotation, app restored from background, etc.) */
  private Bundle savedInstanceState;

  /**
   * Set up the action bar SearchView and its event handlers.
   *
   * @param menu Menu after being inflated in {@link #onCreateOptionsMenu(android.view.Menu)}.
   */
  private void setUpSearchView(Menu menu) {
    // Extract SearchView from the MenuItem object.
    searchMenuItem = menu.findItem(R.id.action_search);
    searchMenuItem.setVisible(searchClientSettings != null);
    searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);

    // Set Searchable XML configuration.
    SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

    // Set SearchView attributes and event listeners.
    searchView.setFocusable(false);
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        if (searchClientSettings != null) {
          // Prepare a intent to send to a new instance of this activity.
          Intent intent = new Intent(SearchActivity.this, SearchActivity.class);
          intent.setAction(Intent.ACTION_SEARCH);
          intent.putExtra(BUNDLE_ID_SEARCH_CLIENT_SETTINGS, searchClientSettings);
          intent.putExtra(BUNDLE_ID_SEARCH_QUERY, query);

          // Collapse the ActionView. This makes navigating through previous results using the back key less painful.
          MenuItemCompat.collapseActionView(searchMenuItem);

          // Start a new activity with the created Intent.
          startActivity(intent);
        }

        // Returns true to override the default behaviour and stop another search Intent from being sent.
        return true;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        // Returns false to perform the default action.
        return false;
      }
    });

    if (savedInstanceState != null) {
      // Restore state from saved instance state bundle (after screen rotation, app restored from background, etc.)
      if (savedInstanceState.containsKey(BUNDLE_ID_SEARCH_QUERY)) {
        // Restore search query from saved instance state.
        searchView.setQuery(savedInstanceState.getCharSequence(BUNDLE_ID_SEARCH_QUERY), false);
      }
      // Restore iconified/expanded search view state from saved instance state.
      if (savedInstanceState.getBoolean(BUNDLE_ID_SEARCH_VIEW_IS_EXPANDED, false)) {
        MenuItemCompat.expandActionView(searchMenuItem);
        // Restore focus state.
        if (!savedInstanceState.getBoolean(BUNDLE_ID_SEARCH_VIEW_IS_FOCUSED, false)) {
          searchView.clearFocus();
        }
      }
    } else if (getIntent() != null && getIntent().getAction().equals(Intent.ACTION_SEARCH)) {
      // Set SearchView query string to match the one sent in the SearchIntent.
      searchView.setQuery(getIntent().getStringExtra(BUNDLE_ID_SEARCH_QUERY), false);
    }
  }

  /** Set up the {@link android.support.v7.app.ActionBar}, including the API service picker dropdown. */
  private void setUpActionBar() {
    ActionBar actionBar = getSupportActionBar();
    ServiceDropdownAdapter serviceDropdownAdapter = new ServiceDropdownAdapter();
    actionBar.setIcon(R.drawable.ic_launcher);
    actionBar.setDisplayShowTitleEnabled(false);
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
    actionBar.setListNavigationCallbacks(serviceDropdownAdapter, serviceDropdownAdapter);
  }

  /**
   * Request a {@link SearchResult} object to be fetched from the background.
   *
   * @param query Query string (a space-separated list of tags).
   */
  private void doSearch(String query) {
    // Show progress bar in ActionBar.
    setSupportProgressBarIndeterminateVisibility(true);
    // Request a search result from the API client.
    searchCallback = new SearchResultCallback();
    searchClient.search(query, searchCallback);
  }

  /**
   * Called when a new Search API is selected by the user from the action bar dropdown.
   *
   * @param settings Selected {@link com.cuddlesoft.norilib.clients.SearchClient.Settings} object.
   */
  protected void onSearchAPISelected(SearchClient.Settings settings) {
    if (settings == null) {
      // The SearchClient setting database is empty.
      return;
    }

    // Show search action bar icon (if not already visible).
    if (searchMenuItem != null) {
      searchMenuItem.setVisible(true);
    }
    // Expand the SearchView when an API is selected manually by the user.
    // (and not automatically restored from previous state when the app is first launched)
    if (searchClientSettings != null && searchMenuItem != null) {
      MenuItemCompat.expandActionView(searchMenuItem);
    }

    searchClientSettings = settings;

    // If a SearchClient wasn't included in the Intent that started this activity, create one now and search for the default query.
    if (searchClient == null && searchResultGridFragment.getSearchResult() == null) {
      searchClient = settings.createSearchClient();
      doSearch(searchClient.getDefaultQuery());
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Request window manager features.
    supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

    // Get shared preferences.
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    // Inflate views.
    setContentView(R.layout.activity_search);

    // Get search result grid fragment from fragment manager.
    searchResultGridFragment = (SearchResultGridFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_searchResultGrid);

    // If the activity was started from a Search intent, create the SearchClient object and submit search.
    Intent intent = getIntent();
    if (intent != null && intent.getAction().equals(Intent.ACTION_SEARCH) && searchResultGridFragment.getSearchResult() == null) {
      SearchClient.Settings searchClientSettings = intent.getParcelableExtra(BUNDLE_ID_SEARCH_CLIENT_SETTINGS);
      searchClient = searchClientSettings.createSearchClient();
      doSearch(intent.getStringExtra(BUNDLE_ID_SEARCH_QUERY));
    }

    // Set up the dropdown API server picker.
    setUpActionBar();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // Cancel pending API callbacks.
    if (searchCallback != null) {
      searchCallback.cancel();
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    // Make saved instance state available to other methods.
    this.savedInstanceState = savedInstanceState;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    // Preserve SearchView state.
    if (searchView != null) {
      outState.putCharSequence(BUNDLE_ID_SEARCH_QUERY, searchView.getQuery());
      outState.putBoolean(BUNDLE_ID_SEARCH_VIEW_IS_EXPANDED, MenuItemCompat.isActionViewExpanded(searchMenuItem));
      outState.putBoolean(BUNDLE_ID_SEARCH_VIEW_IS_FOCUSED, searchView.isFocused());
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.search, menu);
    // Set up action bar search view.
    setUpSearchView(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    switch (item.getItemId()) {
      case R.id.action_settings:
        startActivity(new Intent(SearchActivity.this, SettingsActivity.class));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onImageSelected(Image image, int position) {
    // Open ImageViewerActivity.
    final Intent intent = new Intent(SearchActivity.this, ImageViewerActivity.class);
    intent.putExtra(BUNDLE_ID_IMAGE_INDEX, position);
    intent.putExtra(BUNDLE_ID_SEARCH_RESULT, searchResultGridFragment.getSearchResult());
    intent.putExtra(BUNDLE_ID_SEARCH_CLIENT_SETTINGS, searchClient.getSettings());
    startActivity(intent);
  }

  @Override
  public void fetchMoreImages(SearchResult searchResult) {
    // Ignore request if there is another API request pending.
    if (searchCallback != null) {
      return;
    }
    // Show progress bar in ActionBar.
    setSupportProgressBarIndeterminateVisibility(true);
    // Request search result from API client.
    searchCallback = new SearchResultCallback(searchResult);
    searchClient.search(Tag.stringFromArray(searchResult.getQuery()), searchResult.getCurrentOffset() + 1, searchCallback);
  }

  /** Callback waiting for a SearchResult received on a background thread from the Search API. */
  private class SearchResultCallback implements SearchClient.SearchCallback {
    /** Search result to extend when fetching more images for endless scrolling. */
    private final SearchResult searchResult;
    /** Callback cancelled and should no longer respond to received SearchResult. */
    private boolean isCancelled = false;

    /** Default constructor. */
    public SearchResultCallback() {
      this.searchResult = null;
    }

    /** Constructor used to add more images to an existing SearchResult to implement endless scrolling. */
    public SearchResultCallback(SearchResult searchResult) {
      this.searchResult = searchResult;
    }

    @Override
    public void onFailure(IOException e) {
      if (!isCancelled) {
        // Show error message to user.
        Toast.makeText(SearchActivity.this, String.format(getString(R.string.toast_networkError), e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        // Clear callback and hide progress indicator in Action Bar.
        setSupportProgressBarIndeterminateVisibility(false);
        searchCallback = null;
      }
    }

    @Override
    public void onSuccess(SearchResult searchResult) {
      if (!isCancelled) {
        // Clear callback and hide progress indicator in Action Bar.
        setSupportProgressBarIndeterminateVisibility(false);
        searchCallback = null;

        // Filter the received SearchResult.
        final int resultCount = searchResult.getImages().length;
        if (sharedPreferences.contains(getString(R.string.preference_nsfwFilter_key))) {
          // Get filter from shared preferences.
          searchResult.filter(Image.ObscenityRating.arrayFromStrings(
              sharedPreferences.getString(getString(R.string.preference_nsfwFilter_key), "").split(" ")));
        } else {
          // Get default filter from resources.
          searchResult.filter(Image.ObscenityRating.arrayFromStrings(getResources().getStringArray(R.array.preference_nsfwFilter_defaultValues)));
        }
        if (sharedPreferences.contains(getString(R.string.preference_tagFilter_key))) {
          // Get tag filters from shared preferences and filter the result.
          searchResult.filter(Tag.arrayFromString(sharedPreferences.getString(getString(R.string.preference_tagFilter_key), "")));
        }

        if (this.searchResult != null) {
          // Set onLastPage if no more images were fetched.
          if (resultCount == 0) {
            this.searchResult.onLastPage();
          } else {
            // Extend existing search result for endless scrolling.
            this.searchResult.addImages(searchResult.getImages(), searchResult.getCurrentOffset());
            searchResultGridFragment.setSearchResult(this.searchResult);
          }
        } else {
          // Show search result.
          if (resultCount == 0) {
            searchResult.onLastPage();
          }
          searchResultGridFragment.setSearchResult(searchResult);
        }
      }
    }

    /** Cancels this callback. */
    public void cancel() {
      this.isCancelled = true;
    }
  }

  /** Adapter populating the Search API picker in the ActionBar. */
  private class ServiceDropdownAdapter extends BaseAdapter implements LoaderManager.LoaderCallbacks<List<Pair<Integer, SearchClient.Settings>>>, ActionBar.OnNavigationListener {
    /** Search client settings loader ID. */
    private static final int LOADER_ID_API_SETTINGS = 0x00;
    /** Shared preference key used to store the last active {@link com.cuddlesoft.norilib.clients.SearchClient}. */
    private static final String SHARED_PREFERENCE_LAST_SELECTED_INDEX = "com.cuddlesoft.nori.SearchActivity.lastSelectedServiceIndex";
    /** List of service settings loaded from {@link com.cuddlesoft.nori.database.APISettingsDatabase}. */
    private List<Pair<Integer, SearchClient.Settings>> settingsList;
    /** ID of the last selected item. */
    private long lastSelectedItem;

    public ServiceDropdownAdapter() {
      // Restore last active item from SharedPreferences.
      lastSelectedItem = sharedPreferences.getLong(SHARED_PREFERENCE_LAST_SELECTED_INDEX, 1L);
      // Initialize the search client settings database loader.
      getSupportLoaderManager().initLoader(LOADER_ID_API_SETTINGS, null, this);
    }

    @Override
    public int getCount() {
      if (settingsList == null) {
        return 0;
      } else {
        return settingsList.size();
      }
    }

    @Override
    public SearchClient.Settings getItem(int position) {
      return settingsList.get(position).second;
    }

    @Override
    public long getItemId(int position) {
      // Return database row ID.
      return settingsList.get(position).first;
    }

    /**
     * Get position of the item with given database row ID.
     *
     * @param id Row ID.
     * @return Position of the item.
     */
    public int getPositionByItemId(long id) {
      for (int i = 0; i < getCount(); i++) {
        if (getItemId(i) == id) {
          return i;
        }
      }
      return 0;
    }

    @Override
    public View getView(int position, View recycledView, ViewGroup container) {
      // Reuse recycled view, if possible.
      View view = recycledView;
      if (view == null) {
        // View could not be recycled, inflate new view.
        view = LayoutInflater.from(SearchActivity.this).inflate(android.R.layout.simple_spinner_dropdown_item, container, false);
      }

      // Populate views with content.
      SearchClient.Settings settings = getItem(position);
      TextView text1 = (TextView) view.findViewById(android.R.id.text1);
      text1.setText(settings.getName());

      return view;
    }

    @Override
    public Loader<List<Pair<Integer, SearchClient.Settings>>> onCreateLoader(int id, Bundle args) {
      if (id == LOADER_ID_API_SETTINGS) {
        return new APISettingsDatabase.Loader(SearchActivity.this);
      }
      return null;
    }

    @Override
    public void onLoadFinished(Loader<List<Pair<Integer, SearchClient.Settings>>> loader, List<Pair<Integer, SearchClient.Settings>> data) {
      if (loader.getId() == LOADER_ID_API_SETTINGS) {
        // Update adapter data.
        settingsList = data;
        notifyDataSetChanged();
        // Reselect last active item.
        if (!data.isEmpty()) {
          getSupportActionBar().setSelectedNavigationItem(getPositionByItemId(lastSelectedItem));
        }
      }
    }

    @Override
    public void onLoaderReset(Loader<List<Pair<Integer, SearchClient.Settings>>> loader) {
      // Invalidate adapter's data.
      settingsList = null;
      notifyDataSetInvalidated();
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
      // Save last active item to SharedPreferences.
      lastSelectedItem = id;
      sharedPreferences.edit().putLong(SHARED_PREFERENCE_LAST_SELECTED_INDEX, id).apply();
      // Notify parent activity.
      onSearchAPISelected(getItem(position));

      return true;
    }
  }
}
