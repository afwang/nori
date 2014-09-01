/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.test.database;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;
import android.util.Pair;

import com.cuddlesoft.nori.database.APISettingsDatabase;
import com.cuddlesoft.norilib.clients.SearchClient;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/** Tests the {@link com.cuddlesoft.nori.database.APISettingsDatabase} class. */
public class APISettingsDatabaseTest extends InstrumentationTestCase {
  private Context context;

  @Override
  protected void setUp() throws Exception {
    context = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "_test");
  }

  /** Test if the database is correctly initialized and pre-populated with data. */
  public void testDatabaseCreation() throws Throwable {
    APISettingsDatabase database = new APISettingsDatabase(context);
    List<Pair<Integer, SearchClient.Settings>> settingsList = database.getAll();
    database.close();

    // The list should contain exactly one object.
    assertThat(settingsList).hasSize(1);
    // The database should contain Safebooru API settings by default.
    SearchClient.Settings settings = settingsList.get(0).second;
    assertThat(settings).isNotNull();
    assertThat(settings.getApiType()).isEqualTo(SearchClient.Settings.APIType.GELBOORU);
    assertThat(settings.getName()).isEqualTo("Safebooru");
    assertThat(settings.getEndpoint()).isEqualTo("http://safebooru.org");
    assertThat(settings.getUsername()).isNull();
    assertThat(settings.getPassword()).isNull();
  }

  /** Test the {@link com.cuddlesoft.nori.database.APISettingsDatabase#getAll()} method. */
  public void testGetAll() throws Throwable {
    APISettingsDatabase database = new APISettingsDatabase(context);
    List<Pair<Integer, SearchClient.Settings>> settingsList = database.getAll();
    database.close();

    // The list should not be null or empty.
    assertThat(settingsList).isNotNull().isNotEmpty();
  }

  /** Test the {@link com.cuddlesoft.nori.database.APISettingsDatabase#get(long)} method. */
  public void testGet() throws Throwable {
    APISettingsDatabase database = new APISettingsDatabase(context);
    SearchClient.Settings settings = database.get(1);
    database.close();

    // The settings object should not be null.
    assertThat(settings).isNotNull();
  }

  /** Test the {@link com.cuddlesoft.nori.database.APISettingsDatabase#insert(com.cuddlesoft.norilib.clients.SearchClient.Settings)} method. */
  public void testInsert() throws Throwable {
    APISettingsDatabase database = new APISettingsDatabase(context);
    // Insert new row into the database.
    long rowID = database.insert(new SearchClient.Settings(SearchClient.Settings.APIType.DANBOORU,
        "Danbooru", "http://danbooru.donmai.us"));

    // Now get row from database and verify the data.
    SearchClient.Settings settings = database.get(rowID);
    database.close();
    assertThat(settings.getApiType()).isEqualTo(SearchClient.Settings.APIType.DANBOORU);
    assertThat(settings.getName()).isEqualTo("Danbooru");
    assertThat(settings.getEndpoint()).isEqualTo("http://danbooru.donmai.us");
    assertThat(settings.getUsername()).isNull();
    assertThat(settings.getPassword()).isNull();
  }

  /** Test the {@link com.cuddlesoft.nori.database.APISettingsDatabase#update(long, com.cuddlesoft.norilib.clients.SearchClient.Settings)} method. */
  public void testUpdate() throws Throwable {
    APISettingsDatabase database = new APISettingsDatabase(context);

    // Insert new row into the database.
    long rowID = database.insert(new SearchClient.Settings(SearchClient.Settings.APIType.DANBOORU,
        "Danbooru", "http://danbooru.donmai.us"));

    // Update the newly created row.
    int rowsAffected = database.update(rowID, new SearchClient.Settings(SearchClient.Settings.APIType.DANBOORU_LEGACY,
        "Danbooru", "http://danbooru.donmai.us"));
    assertThat(rowsAffected).isEqualTo(1);

    // Now get the row from the database and verify the data.
    SearchClient.Settings settings = database.get(rowID);
    database.close();
    assertThat(settings.getApiType()).isEqualTo(SearchClient.Settings.APIType.DANBOORU_LEGACY);
  }

  /** Test the {@link com.cuddlesoft.nori.database.APISettingsDatabase#delete(long)} method. */
  public void testDelete() throws Throwable {
    APISettingsDatabase database = new APISettingsDatabase(context);

    // Insert new row into the database.
    long rowID = database.insert(new SearchClient.Settings(SearchClient.Settings.APIType.DANBOORU,
        "Danbooru", "http://danbooru.donmai.us"));

    // Delete the newly created row.
    int rowsAffected = database.delete(rowID);
    assertThat(rowsAffected).isEqualTo(1);

    // Make sure it's really been deleted.
    assertThat(database.get(rowID)).isNull();

    // Clean up.
    database.close();
  }

  /** Test if the database sends a Broadcast to the {@link android.support.v4.content.LocalBroadcastManager} when the data is changed. */
  public void testUpdateBroadcast() throws Throwable {
    // Create a lock that waits for the broadcast to be received in the background.
    final CountDownLatch lock = new CountDownLatch(3);

    runTestOnUiThread(new Runnable() {
      @Override
      public void run() {
        // Register BroadcastReceiver.
        LocalBroadcastManager.getInstance(context).registerReceiver(new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            lock.countDown(); // Should receive 3 Broadcasts, one for each database operation.
            if (lock.getCount() == 0) {
              // Unregister broadcast receiver.
              LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
            }
          }
        }, new IntentFilter(APISettingsDatabase.BROADCAST_UPDATE));
        // Trigger database change broadcasts.
        APISettingsDatabase database = new APISettingsDatabase(context);
        long rowID = database.insert(new SearchClient.Settings(SearchClient.Settings.APIType.DANBOORU,
            "Danbooru", "http://danbooru.donmai.us"));
        database.update(rowID, new SearchClient.Settings(SearchClient.Settings.APIType.DANBOORU_LEGACY,
            "Danbooru", "http://danbooru.donmai.us"));
        database.delete(rowID);
        database.close();
      }
    });

    // Wait 10 seconds for the test to complete.
    lock.await(10, TimeUnit.SECONDS);
    assertThat(lock.getCount()).isEqualTo(0);
  }
}
