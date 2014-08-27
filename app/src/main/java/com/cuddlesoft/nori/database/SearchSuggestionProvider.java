package com.cuddlesoft.nori.database;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import static com.cuddlesoft.nori.database.SearchSuggestionDatabase.COLUMN_ICON;
import static com.cuddlesoft.nori.database.SearchSuggestionDatabase.COLUMN_ID;
import static com.cuddlesoft.nori.database.SearchSuggestionDatabase.COLUMN_NAME;
import static com.cuddlesoft.nori.database.SearchSuggestionDatabase.TABLE_NAME;

public class SearchSuggestionProvider extends ContentProvider {
  /** Content provider authority. (Unique ID) */
  public static String AUTHORITY = "com.cuddlesoft.nori.SearchSuggestionProvider";
  /** SQLite database access helper. */
  private SearchSuggestionDatabase dbHelper;

  /**
   * Uri path ID for queried suggestions data.
   * This is the path that the search manager will use when querying your content provider for suggestions data based on user input (e.g. looking for partial matches).
   */
  private static final int SEARCH_SUGGEST = 0;
  /**
   * Uri path ID for shortcut validation.
   * This is the path that the search manager will use when querying your content provider to refresh a shortcutted suggestion result and to check if it is still valid.
   * When asked, a source may return an up to date result, or no result.
   * No result indicates the shortcut refers to a no longer valid suggestion.
   */
  private static final int SHORTCUT_REFRESH = 1;
  /** URI parser used to match content provider paths. */
  private static final UriMatcher sURIMatcher;

  /** Columns to include in queries to the underlying SQLite database. */
  private static final String[] COLUMNS = {
      COLUMN_ID,
      COLUMN_NAME,
      COLUMN_ICON
  };

  @Override
  public boolean onCreate() {
    // Create an instance of the SQLite access helper to the underlying database.
    dbHelper = new SearchSuggestionDatabase(getContext());

    return true;
  }

  /**
   * Get tag suggestions from the underlying SQLite database.
   *
   * @param query Query the database for tags starting with this substring.
   * @return Database cursor with returned suggestion.
   */
  private Cursor getSuggestions(String query) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();

    if (query == null) {
      return db.query(TABLE_NAME, COLUMNS, null, null, null, null, null);
    } else {
      return db.query(TABLE_NAME, COLUMNS, COLUMN_NAME + " LIKE ?", new String[]{query + "%"}, null, null, null);
    }
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    // Match the content URI to decide which type of query to run.
    switch (sURIMatcher.match(uri)) {
      case SEARCH_SUGGEST:
        String query = null;
        if (uri.getPathSegments().size() > 1) {
          query = uri.getLastPathSegment().toLowerCase();
        }
        return getSuggestions(query);
      case SHORTCUT_REFRESH:
        // This is not implemented since the SUGGEST_COLUMN_SHORTCUT_ID column is not defined.
        // It's only useful when providing suggestions for the Quick Search Box (search from the launch screen).
        return null;
      default:
        throw new IllegalArgumentException("Unknown URI " + uri);
    }
  }

  @Override
  public String getType(Uri uri) {
    // Match URI and return the appropriate mime type.
    switch (sURIMatcher.match(uri)) {
      case SEARCH_SUGGEST:
        return SearchManager.SUGGEST_MIME_TYPE;
      case SHORTCUT_REFRESH:
        return SearchManager.SHORTCUT_MIME_TYPE;
      default:
        throw new IllegalArgumentException("Unknown URI " + uri);
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues contentValues) {
    // Use SearchSuggestionDatabase methods instead.
    throw new UnsupportedOperationException();
  }

  @Override
  public int delete(Uri uri, String s, String[] strings) {
    // Use SearchSuggestionDatabase methods instead.
    throw new UnsupportedOperationException();
  }

  @Override
  public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
    // Use SearchSuggestionDatabase methods instead.
    throw new UnsupportedOperationException();
  }

  static {
    // Set up the parser used to match ContentProvider URIs.
    sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    sURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
    sURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
    sURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT, SHORTCUT_REFRESH);
    sURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", SHORTCUT_REFRESH);
  }
}
