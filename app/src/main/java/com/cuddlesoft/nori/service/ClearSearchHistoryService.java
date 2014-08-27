package com.cuddlesoft.nori.service;

import android.app.IntentService;
import android.content.Intent;

import com.cuddlesoft.nori.database.SearchSuggestionDatabase;

/**
 * Service used by {@link com.cuddlesoft.nori.SettingsActivity} to remove all recent search history entries stored in
 * {@link com.cuddlesoft.nori.database.SearchSuggestionDatabase}.
 */
public class ClearSearchHistoryService extends IntentService {

  public ClearSearchHistoryService() {
    // Set service name (useful for debugging).
    super("ClearSearchHistoryService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    // Open the search suggestion database
    SearchSuggestionDatabase db = new SearchSuggestionDatabase(this);

    // Remove recent search history entries.
    db.eraseSearchHistory();

    // Close the database resource.
    db.close();
  }
}
