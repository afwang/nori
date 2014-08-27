package com.cuddlesoft.nori.test.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import com.cuddlesoft.nori.database.SearchSuggestionDatabase;

import static org.fest.assertions.api.Assertions.assertThat;
import static com.cuddlesoft.nori.database.SearchSuggestionDatabase.*;

/** Tests the {@link SearchSuggestionDatabase} class. */
public class SearchSuggestionDatabaseTest extends InstrumentationTestCase {
  /** App context used for testing. */
  private Context context;

  @Override
  protected void setUp() throws Exception {
    // Set up a new app context before each test.
    context = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "_test");
  }

  /** Tests if the database is created and pre-populated with the Safebooru Top 1000 data set. */
  public void testDatabaseCreation() throws Throwable {
    // Get a read-only instance of the database.
    SearchSuggestionDatabase searchSuggestionDatabase = new SearchSuggestionDatabase(context);
    SQLiteDatabase db = searchSuggestionDatabase.getReadableDatabase();

    // Get all suggestions added from the Safebooru Top 1000 data set from the database.
    Cursor c = db.query(TABLE_NAME, null, COLUMN_ICON + " IS NULL", null, null, null, COLUMN_ID);

    // Number of returned rows should match the number of lines in the "tags.txt" asset file.
    assertThat(c.getCount()).isEqualTo(962);

    // Clean-up native resources.
    c.close();
    db.close();
  }
}
