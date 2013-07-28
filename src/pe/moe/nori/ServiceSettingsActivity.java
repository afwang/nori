package pe.moe.nori;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import pe.moe.nori.providers.ServiceSettingsProvider;
import pe.moe.nori.services.ResourceTypeDetectService;

public class ServiceSettingsActivity extends SherlockFragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {
  /** Service settings cursor loader ID used in {@link LoaderManager} */
  public static final int SERVICE_SETTINGS_LOADER_ID = 0x00;
  /** Service list {@link ListView} */
  private ListView mServiceListView;
  /** Service list {@link Cursor} */
  private Cursor mServiceSettingsCursor;
  /** Receiver listening for results from {@link ResourceTypeDetectService} */
  private BroadcastReceiver mResourceTypeBroadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final int status = intent.getIntExtra("status", 0);
      // Hide progress bar.
      setSupportProgressBarIndeterminateVisibility(false);
      // Status toast.
      if (status == ResourceTypeDetectService.STATUS_RESOURCE_NOT_FOUND)
        Toast.makeText(ServiceSettingsActivity.this, "No resource found at given URL", Toast.LENGTH_SHORT).show();
      else if (status == ResourceTypeDetectService.STATUS_INVALID_URI)
        Toast.makeText(ServiceSettingsActivity.this, "Invalid URL", Toast.LENGTH_SHORT).show();
      else if (status != ResourceTypeDetectService.STATUS_OK)
        Toast.makeText(ServiceSettingsActivity.this, "Unknown error", Toast.LENGTH_SHORT).show();
    }
  };
  /** Receiver listening for changes in service configuration */
  private BroadcastReceiver mSettingsChangedReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      // Reload service list.
      getSupportLoaderManager().getLoader(SERVICE_SETTINGS_LOADER_ID).forceLoad();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

    // Set layout.
    setContentView(R.layout.activity_servicesettings);
    setSupportProgressBarIndeterminateVisibility(false);

    // Set list onItemClickListener.
    mServiceListView = (ListView) findViewById(R.id.service_list);
    mServiceListView.setOnItemClickListener(this);

    // Register broadcast receivers.
    registerReceiver(mResourceTypeBroadcastReceiver, new IntentFilter("pe.moe.nori.services.ResourceTypeDetectService.result"));
    registerReceiver(mSettingsChangedReceiver, new IntentFilter("pe.moe.nori.providers.ServiceSettingsProvider.update"));

    // Start loaders.
    getSupportLoaderManager().initLoader(SERVICE_SETTINGS_LOADER_ID, null, this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // Unregister broadcast receivers.
    unregisterReceiver(mResourceTypeBroadcastReceiver);
    unregisterReceiver(mSettingsChangedReceiver);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getSupportMenuInflater().inflate(R.menu.servicesettings, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // Override default home button behavior.
        onBackPressed();
        return true;
      case R.id.action_add_service:
        // Show a dialog with service settings form.
        new ServiceSettingsDialog(null).show(getSupportFragmentManager(), "ServiceSettingsDialog");
        return true;
      default:
        return false;
    }
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

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    // Create an instance of ServiceSettings from Cursor.
    if (mServiceSettingsCursor != null && mServiceSettingsCursor.moveToPosition(position)) {
      ServiceSettingsProvider.ServiceSettings serviceSettings = new ServiceSettingsProvider.ServiceSettings();
      serviceSettings.id = mServiceSettingsCursor
          .getInt(mServiceSettingsCursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_ID));
      serviceSettings.name = mServiceSettingsCursor
          .getString(mServiceSettingsCursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_NAME));
      serviceSettings.apiUrl = mServiceSettingsCursor
          .getString(mServiceSettingsCursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_API_URL));
      serviceSettings.type = mServiceSettingsCursor
          .getInt(mServiceSettingsCursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_TYPE));
      serviceSettings.subtype = mServiceSettingsCursor
          .getInt(mServiceSettingsCursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_SUBTYPE));
      serviceSettings.requiresAuthentication = mServiceSettingsCursor
          .getInt(mServiceSettingsCursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_REQUIRES_AUTHENTICATION)) == 1;
      if (serviceSettings.requiresAuthentication) {
        serviceSettings.username = mServiceSettingsCursor
            .getString(mServiceSettingsCursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_USERNAME));
        serviceSettings.passphrase = mServiceSettingsCursor
            .getString(mServiceSettingsCursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_PASSPHRASE));
      }
      // Show dialog.
      new ServiceSettingsDialog(serviceSettings).show(getSupportFragmentManager(), "ServiceSettingsDialog");
    }
  }

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

  ;

  /** Shows a dialog with a service settings editor */
  public static class ServiceSettingsDialog extends SherlockDialogFragment implements View.OnClickListener,
      TextWatcher, View.OnFocusChangeListener {
    /** Danbooru's API requires authentication. Used for showing authentication related fields when this is the service URL. */
    private static final String DANBOORU_URL = "http://danbooru.donmai.us";
    /** Service ID or null when adding a new service */
    private ServiceSettingsProvider.ServiceSettings mServiceSettings;
    /** Service name {@link EditText} control */
    private AutoCompleteTextView mServiceName;
    /** Service URI {@link EditText} control */
    private EditText mServiceUri;
    /** Service username {@link EditText} control. Shown only when service requires authentication */
    private EditText mServiceUsername;
    /** Service passphrase (API key) {@link EditText} control. Shown only when service requires authentication */
    private EditText mServicePassphrase;
    /** Service names used for autocomplete. */
    private String[] mAutocompleteServiceNames;
    /** Service URIs used for autocomplete. */
    private String[] mAutocompleteServiceUris;
    /** Automatically fills in Service URL when user selects a service name suggetion */
    private AdapterView.OnItemClickListener mOnSuggestionClickListener = new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final String serviceName = (String) parent.getItemAtPosition(position);
        for (int i = 0; i < mAutocompleteServiceNames.length; i++) {
          if (mAutocompleteServiceNames[i].equals(serviceName)) {
            mServiceUri.setText(mAutocompleteServiceUris[i]);
          }
        }
      }
    };

    /** Default constructor */
    public ServiceSettingsDialog() {
    }

    /**
     * Constructor used when editing existing services.
     *
     * @param serviceSettings Service settings being edited.
     */
    public ServiceSettingsDialog(ServiceSettingsProvider.ServiceSettings serviceSettings) {
      mServiceSettings = serviceSettings;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      if (mServiceSettings != null)
        // Save service settings.
        outState.putParcelable("service_settings", mServiceSettings);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      // Restore ID of service being edited, if possible.
      if (savedInstanceState != null && savedInstanceState.containsKey("service_settings")) {
        mServiceSettings = savedInstanceState.getParcelable("service_settings");
      }

      // Load autocomplete arrays.
      mAutocompleteServiceNames = getResources().getStringArray(R.array.service_names);
      mAutocompleteServiceUris = getResources().getStringArray(R.array.service_urls);

      // Create a dialog builder.
      AlertDialog.Builder builder = new AlertDialog.Builder(getSherlockActivity());
      final AlertDialog dialog;

      // Inflate dialog view and get View references.
      View dialogView = getSherlockActivity().getLayoutInflater().inflate(R.layout.dialog_servicesettings, null);
      mServiceName = (AutoCompleteTextView) dialogView.findViewById(R.id.service_name);
      mServiceName.setAdapter(new ArrayAdapter<String>(getSherlockActivity(),
          android.R.layout.simple_dropdown_item_1line, mAutocompleteServiceNames));
      mServiceName.setThreshold(1);
      mServiceName.setOnItemClickListener(mOnSuggestionClickListener);
      mServiceUri = (EditText) dialogView.findViewById(R.id.service_uri);
      mServiceUri.addTextChangedListener(this);
      mServiceUri.setOnFocusChangeListener(this);
      mServiceUsername = (EditText) dialogView.findViewById(R.id.service_username);
      mServicePassphrase = (EditText) dialogView.findViewById(R.id.service_passphrase);

      // Fill dialog with existing data when editing a service.
      if (mServiceSettings != null) {
        mServiceName.setText(mServiceSettings.name);
        mServiceUri.setText(mServiceSettings.apiUrl);
        if (mServiceSettings.requiresAuthentication) {
          mServiceUsername.setText(mServiceSettings.username);
          mServicePassphrase.setText(mServiceSettings.passphrase);
        }
      }

      // Use a different title for adding new preferences and editing existing ones.
      if (mServiceSettings == null)
        builder.setTitle(R.string.dialog_title_add_service);
      else
        builder.setTitle(R.string.dialog_title_edit_service);

      // Set content view and button click listeners.
      builder.setView(dialogView);
      builder.setNegativeButton(android.R.string.cancel, null);
      builder.setPositiveButton(android.R.string.ok, null);

      dialog = builder.create();
      dialog.setOnShowListener(new DialogInterface.OnShowListener() {
        @Override
        public void onShow(DialogInterface d) {
          dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ServiceSettingsDialog.this);

        }
      });
      return dialog;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
      // Show authentication controls for danbooru's API.
      if (mServiceUri.getText().toString().startsWith(DANBOORU_URL)) {
        mServiceUsername.setVisibility(View.VISIBLE);
        mServicePassphrase.setVisibility(View.VISIBLE);
      } else {
        mServiceUsername.setVisibility(View.GONE);
        mServicePassphrase.setVisibility(View.GONE);
      }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void onClick(View v) {
      // Get form values.
      final String serviceName = mServiceName.getText().toString();
      final String serviceUri = mServiceUri.getText().toString();
      final String serviceUsername = mServiceUsername.getText().toString();
      final String servicePassphrase = mServicePassphrase.getText().toString();

      // Make sure fields aren't left empty.
      if (serviceName.isEmpty() || serviceUri.isEmpty() || (serviceUsername.isEmpty() && !servicePassphrase.isEmpty())
          || (servicePassphrase.isEmpty() && !serviceUsername.isEmpty()))
        return;

      // Create a new ServiceSettings object and fill it with data.
      final ServiceSettingsProvider.ServiceSettings serviceSettings = new ServiceSettingsProvider.ServiceSettings();
      if (mServiceSettings != null)
        serviceSettings.id = mServiceSettings.id;
      else
        serviceSettings.id = -1;
      serviceSettings.name = serviceName;
      serviceSettings.apiUrl = serviceUri;
      serviceSettings.requiresAuthentication = (mServiceUsername.getVisibility() == View.VISIBLE && mServicePassphrase.getVisibility() == View.VISIBLE);
      if (serviceSettings.requiresAuthentication) {
        serviceSettings.username = serviceUsername;
        serviceSettings.passphrase = servicePassphrase;
      }

      // Send a broadcast to ResourceTypeDetectService for resource auto-detection and additional validation.
      final Intent serviceIntent = new Intent(getSherlockActivity(), ResourceTypeDetectService.class);
      serviceIntent.putExtra("service_settings", serviceSettings);
      getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
      getSherlockActivity().startService(serviceIntent);
      getDialog().dismiss();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
      // Provide hint to the user that the URL requires a scheme prefix.
      if (hasFocus && mServiceUri.getText().toString().equals("")) {
        mServiceUri.setText("http://");
      }
    }
  }

  /** Adapter for the Service list {@link ListView} */
  private class ServiceSettingsCursorAdapter extends CursorAdapter {
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
      View v = inflater.inflate(R.layout.listitem_service, parent, false);
      return v;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
      TextView serviceName = (TextView) view.findViewById(android.R.id.title);
      TextView serviceSummary = (TextView) view.findViewById(android.R.id.summary);
      ImageButton deleteButton = (ImageButton) view.findViewById(R.id.remove_button);
      // This gets overriden in ImageButton's constructor.
      deleteButton.setFocusable(false);
      // Set OnClickListener for the remove button.
      deleteButton.setOnClickListener(new RemoveServiceOnClickListener(cursor.getInt(cursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_ID))));
      // Set TextView content.
      serviceName.setText(cursor.getString(
          cursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_NAME)));
      serviceSummary.setText(cursor.getString(
          cursor.getColumnIndex(ServiceSettingsProvider.DatabaseOpenHelper.COLUMN_API_URL)));
    }
  }

  /** OnClickListener for remove service buttons. */
  private class RemoveServiceOnClickListener implements View.OnClickListener {
    private final int mServiceId;

    /**
     * Creates a new {@link RemoveServiceOnClickListener}.
     *
     * @param serviceId ID of service that will get deleted when this listener gets triggered.
     */
    public RemoveServiceOnClickListener(int serviceId) {
      mServiceId = serviceId;
    }

    @Override
    public void onClick(View v) {
      // Send broadcast to remove the service.
      Intent broadcastIntent = new Intent("pe.moe.nori.providers.ServiceSettingsProvider.remove");
      broadcastIntent.putExtra("pe.moe.nori.Service.id", mServiceId);
      sendBroadcast(broadcastIntent);
    }
  }
}
