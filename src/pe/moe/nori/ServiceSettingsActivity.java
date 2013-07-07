package pe.moe.nori;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import pe.moe.nori.providers.ServiceSettingsProvider;

public class ServiceSettingsActivity extends SherlockFragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
  /** Service settings cursor loader ID used in {@link LoaderManager} */
  public static final int SERVICE_SETTINGS_LOADER_ID = 0x00;
  /** Service list {@link ListView} */
  private ListView mServiceListView;
  /** Service list {@link Cursor} */
  private Cursor mServiceSettingsCursor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_servicesettings);
    mServiceListView = (ListView) findViewById(R.id.service_list);
    // Start loaders.
    getSupportLoaderManager().initLoader(SERVICE_SETTINGS_LOADER_ID, null, this);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    if (id == SERVICE_SETTINGS_LOADER_ID) {
      return new ServiceSettingsLoader(this);
    }
    return null;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    if (loader.getId() == SERVICE_SETTINGS_LOADER_ID) {
      mServiceSettingsCursor = data;
      if (mServiceListView.getAdapter() == null)
        // Create a new adapter if not set.
        mServiceListView.setAdapter(new ServiceSettingsCursorAdapter(this, data));
      else
        // Reuse existing adapter.
        ((CursorAdapter) mServiceListView.getAdapter()).swapCursor(data);
    }

  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    if (loader.getId() == SERVICE_SETTINGS_LOADER_ID && mServiceSettingsCursor != null && !mServiceSettingsCursor.isClosed()) {
      // Close cursor.
      mServiceSettingsCursor.close();
    }
  }

  /** Adapter for the Service list {@link ListView} */
  private static class ServiceSettingsCursorAdapter extends CursorAdapter {
    private final LayoutInflater inflater;

    /**
     * Create a new adapter for the Service list {@link ListView}.
     *
     * @param context The context
     * @param c       The cursor from which to get data
     */
    public ServiceSettingsCursorAdapter(Context context, Cursor c) {
      super(context, c, false);
      inflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
      View v = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
      return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
      TextView serviceName = (TextView) view.findViewById(android.R.id.text1);
      TextView serviceSummary = (TextView) view.findViewById(android.R.id.text2);
      serviceName.setText(cursor.getString(
          cursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_NAME)));
      serviceSummary.setText(cursor.getString(
          cursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_API_URL)));
    }
  }

  ;

  /** Asynchronously queries the database for a list of service settings. */
  private static class ServiceSettingsLoader extends AsyncTaskLoader<Cursor> {
    private final SQLiteOpenHelper dbHelper;
    private final SQLiteDatabase database;
    private Cursor mCursor;

    /**
     * Creates a new loader that will asynchronously load service settings from a {@link SQLiteDatabase}
     *
     * @param context The context
     */
    public ServiceSettingsLoader(Context context) {
      super(context);
      dbHelper = new ServiceSettingsProvider.DatabaseOpenHelper(context);
      database = dbHelper.getReadableDatabase();
    }

    @Override
    protected void onStartLoading() {
      if (mCursor != null)
        deliverResult(mCursor);
      if (takeContentChanged() || mCursor == null)
        forceLoad();
    }

    @Override
    public void deliverResult(Cursor data) {
      if (isReset()) {
        // An async query came in while the loader is stopped.
        if (data != null) {
          data.close();
        }
        return;
      }
      Cursor oldCursor = mCursor;
      mCursor = data;

      if (isStarted()) {
        super.deliverResult(data);
      }

      if (oldCursor != null && oldCursor != data && !oldCursor.isClosed()) {
        oldCursor.close();
      }
    }

    @Override
    public Cursor loadInBackground() {
      // Query database.
      Cursor c = database.query(ServiceSettingsProvider.DatabaseOpenHelper.SERVICE_SETTINGS_TABLE_NAME, null, null, null,
          null, null, null);

      return c;
    }

    @Override
    protected void onReset() {
      super.onReset();

      // Ensure the loader is stopped.
      onStopLoading();

      if (mCursor != null && !mCursor.isClosed())
        mCursor.close();
      mCursor = null;
      database.close();
    }

    @Override
    protected void onStopLoading() {
      // Attempt to cancel the current load task if possible.
      cancelLoad();
    }

    @Override
    public void onCanceled(Cursor data) {
      if (data != null && !mCursor.isClosed()) {
        mCursor.close();
      }
    }
  }
}
