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
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import pe.moe.nori.adapters.ServiceDropdownAdapter;
import pe.moe.nori.providers.ServiceSettingsProvider;

import java.util.List;

public class SearchActivity extends SherlockFragmentActivity implements LoaderManager.LoaderCallbacks<List<ServiceSettingsProvider.ServiceSettings>> {
  private static final int SERVICE_DROPDOWN_LOADER = 0x00;
  public ActionBar.OnNavigationListener mNavigationCallback = new ActionBar.OnNavigationListener() {

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
      // Save state.
      mPreferences.edit().putLong("last_service_dropdown_index", itemId).apply();
      // Select item.
      mActionBar.setSelectedNavigationItem(itemPosition);
      return true;
    }

  };
  private ActionBar mActionBar;
  private SharedPreferences mPreferences;
  private SharedPreferences mSharedPreferences;
  private LoaderManager mLoaderManager;
  private ServiceSettingsProvider mServiceSettingsProvider;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Get action bar.
    mActionBar = getSupportActionBar();

    // Get service settings provider.
    mServiceSettingsProvider = new ServiceSettingsProvider(this);

    // Get shared preferences.
    mPreferences = getPreferences(MODE_PRIVATE);
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    // Get loader manager.
    mLoaderManager = getSupportLoaderManager();
    mLoaderManager.initLoader(SERVICE_DROPDOWN_LOADER, null, this).forceLoad();

    // Inflate view
    setContentView(R.layout.activity_search);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);

    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.search, menu);
    MenuItem searchItem = menu.findItem(R.id.action_search);

    return true;
  }

  @Override
  public Loader<List<ServiceSettingsProvider.ServiceSettings>> onCreateLoader(int i, Bundle bundle) {
    if (i == SERVICE_DROPDOWN_LOADER) {
      return mServiceSettingsProvider.getServiceSettingsLoader();
    }
    return null;
  }

  @Override
  public void onLoadFinished(Loader<List<ServiceSettingsProvider.ServiceSettings>> listLoader, List<ServiceSettingsProvider.ServiceSettings> serviceSettings) {
    if (listLoader.getId() == SERVICE_DROPDOWN_LOADER) {
      // Hide title and change navigation mode.
      mActionBar.setDisplayShowTitleEnabled(false);
      mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

      // Create the SpinnerAdapter.
      final ServiceDropdownAdapter serviceDropdownAdapter = new ServiceDropdownAdapter(this, serviceSettings);
      mActionBar.setListNavigationCallbacks(serviceDropdownAdapter, mNavigationCallback);

      // Restore last known state.
      final int lastSelectedIndex = serviceDropdownAdapter.getPositionByItemId(mPreferences.getLong("last_service_dropdown_index", 0L));
      if (lastSelectedIndex != -1) {
        mActionBar.setSelectedNavigationItem(lastSelectedIndex);
      }
    }
  }

  @Override
  public void onLoaderReset(Loader<List<ServiceSettingsProvider.ServiceSettings>> listLoader) {
    mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    mActionBar.setDisplayShowTitleEnabled(true);
  }
}
