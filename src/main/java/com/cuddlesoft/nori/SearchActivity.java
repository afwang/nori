/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import com.cuddlesoft.nori.fragment.SearchResultGridFragment;
import com.cuddlesoft.norilib.Image;
import com.cuddlesoft.norilib.SearchResult;
import com.cuddlesoft.norilib.Tag;
import com.cuddlesoft.norilib.clients.Gelbooru;
import com.cuddlesoft.norilib.clients.SearchClient;

import java.io.IOException;

import io.github.vomitcuddle.SearchViewAllowEmpty.SearchView;

/** Searches for images and displays the results in a scrollable grid of thumbnails. */
public class SearchActivity extends ActionBarActivity implements SearchResultGridFragment.OnSearchResultGridFragmentInteractionListener {
  /** LogCat tag (used for filtering log output). */
  private static final String TAG = "com.cuddlesoft.nori.SearchActivity";
  /** Identifier used to preserve current search query in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_QUERY = "com.cuddlesoft.nori.SearchQuery";
  /** Identifier used to preserve iconified/expanded state of the SearchView in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_VIEW_IS_EXPANDED = "com.cuddlesoft.nori.SearchView.isExpanded";
  /** Identifier used to preserve search view focused state. */
  private static final String BUNDLE_ID_SEARCH_VIEW_IS_FOCUSED = "com.cuddlesoft.nori.SearchView.isFocused";
  /** Identifier used to send the active {@link com.cuddlesoft.norilib.SearchResult} to {@link com.cuddlesoft.nori.ImageViewerActivity}. */
  public static final String BUNDLE_ID_SEARCH_RESULT = "com.cuddlesoft.nori.SearchResult";
  /** Identifier used to send the position of the selected {@link com.cuddlesoft.norilib.Image} to {@link com.cuddlesoft.nori.ImageViewerActivity}. */
  public static final String BUNDLE_ID_IMAGE_INDEX = "com.cuddlesoft.nori.ImageIndex";
  /** Search API Client. */
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
    // Get SearchView from the Menu item.
    searchMenuItem = menu.findItem(R.id.action_search);
    searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
    // Set SearchView attributes.
    searchView.setFocusable(false);
    searchView.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      // Force ASCII keyboard on foreign input methods. (API 16+)
      searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_FORCE_ASCII);
    }
    // Restore state from saved instance state bundle (after screen rotation, app restored from background, etc.)
    if (savedInstanceState != null) {
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
    }
    // Set event listener responding to submitted queries.
    SearchView.OnQueryTextListener searchViewHandler = new SearchViewListener();
    searchView.setOnQueryTextListener(searchViewHandler);
  }

  /**
   * Submit a search query.
   *
   * @param query Query string (comma-separated list of tags).
   */
  private void doSearch(String query) {
    // Cancel any existing callbacks waiting for a SearchResult.
    if (searchCallback != null) {
      searchCallback.cancel();
      searchCallback = null;
    }
    // Remove results from the search result grid fragment.
    searchResultGridFragment.setSearchResult(null);
    // Show progress bar in ActionBar.
    setSupportProgressBarIndeterminateVisibility(true);
    // Request search result from API client.
    searchCallback = new SearchResultCallback();
    searchClient.search(query, searchCallback);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // TODO: Implement API server picker.
    searchClient = new Gelbooru("http://gelbooru.com");
    // Request window manager features.
    supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    // Inflate views.
    setContentView(R.layout.activity_search);
    // Get search result grid fragment from fragment manager.
    searchResultGridFragment = (SearchResultGridFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_searchResultGrid);
    // Search for default query on first launch.
    if (savedInstanceState == null) {
      doSearch(searchClient.getDefaultQuery());
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
    /**int id = item.getItemId();
     if (id == R.id.action_settings) {
     // TODO: Implement me.
     return true;
     }*/
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onImageSelected(Image image, int position) {
    // Open ImageViewerActivity.
    final Intent intent = new Intent(SearchActivity.this, ImageViewerActivity.class);
    intent.putExtra(BUNDLE_ID_IMAGE_INDEX, position);
    intent.putExtra(BUNDLE_ID_SEARCH_RESULT, searchResultGridFragment.getSearchResult());
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
    searchClient.search(Tag.stringFromArray(searchResult.getQuery()), searchResult.getCurrentOffset()+1, searchCallback);
  }

  /** Listens for queries submitted to the action bar {@link android.support.v7.widget.SearchView}. */
  private class SearchViewListener implements SearchView.OnQueryTextListener {

    @Override
    public boolean onQueryTextSubmit(String s) {
      // Remove focus from the SearchView and submit query.
      searchView.clearFocus();
      SearchActivity.this.doSearch(s);

      return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
      // Return false to perform the default action of showing any suggestions available.
      return false;
    }
  }

  /** Callback waiting for a SearchResult received on a background thread from the Search API. */
  private class SearchResultCallback implements SearchClient.SearchCallback {
    /** Callback cancelled and should no longer respond to received SearchResult. */
    private boolean isCancelled = false;
    /** Search result to extend when fetching more images for endless scrolling. */
    private final SearchResult searchResult;

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
      // Log failure.
      Log.e(TAG, String.format("Error occurred when fetching query: %s", e.toString()));
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
        if (this.searchResult != null) {
          // Set onLastPage if no more images were fetched.
          if (searchResult.getImages().length == 0) {
            this.searchResult.onLastPage();
          } else {
            // Extend existing search result for endless scrolling.
            this.searchResult.addImages(searchResult.getImages(), searchResult.getCurrentOffset());
            searchResultGridFragment.setSearchResult(this.searchResult);
          }
        } else {
          // Show search result.
          searchResultGridFragment.setSearchResult(searchResult);
        }
      }
    }

    /** Cancels this callback. */
    public void cancel() {
      this.isCancelled = true;
    }
  }
}
