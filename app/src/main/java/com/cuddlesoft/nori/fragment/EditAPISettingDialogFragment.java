package com.cuddlesoft.nori.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.cuddlesoft.nori.R;
import com.cuddlesoft.norilib.clients.SearchClient;

/** Dialog fragment used to add new and edit existing {@link com.cuddlesoft.nori.database.APISettingsDatabase} entries in {@link com.cuddlesoft.nori.APISettingsActivity}. */
public class EditAPISettingDialogFragment extends DialogFragment implements AdapterView.OnItemClickListener, View.OnClickListener, TextWatcher, View.OnFocusChangeListener {
  /** Bundle ID of {@link SearchClient.Settings} object used to edit an existing {@link com.cuddlesoft.nori.database.APISettingsDatabase} entry. */
  private static final String BUNDLE_ID_SETTINGS = "com.cuddlesoft.nori.SearchClient.Settings";
  /** Bundle ID of the database row ID passed into the arguments bundle when editing an existing object. */
  private static final String BUNDLE_ID_ROW_ID = "com.cuddlesoft.nori.SearchClient.Settings.rowId";
  /** Danbooru API URL. Used to only show the optional authentication fields for the Danbooru API. */
  private static final String DANBOORU_API_URL = "http://danbooru.donmai.us";
  /** Interface in the parent activity waiting to receive data from the dialog. */
  private Listener listener;
  /** Database ID of the object being edited (if not creating a new settings object). */
  private long rowId = -1;
  /** Service name input field. */
  private AutoCompleteTextView name;
  /** Service uri input field. */
  private EditText uri;
  /** Service authentication username field. */
  private EditText username;
  /** Service authentication password/API key field. */
  private EditText passphrase;

  /**
   * Factory method used when editing an existing settings database entry.
   *
   * @param rowId    Database row ID.
   * @param settings Setting object to edit.
   * @return EditAPISettingDialogFragment with appended arguments bundle.
   */
  public static EditAPISettingDialogFragment newInstance(long rowId, SearchClient.Settings settings) {
    // Create a new EditAPISettingDialogFragment.
    EditAPISettingDialogFragment fragment = new EditAPISettingDialogFragment();

    // Append parameters to the fragment's arguments bundle.
    Bundle arguments = new Bundle();
    arguments.putLong(BUNDLE_ID_ROW_ID, rowId);
    arguments.putParcelable(BUNDLE_ID_SETTINGS, settings);
    fragment.setArguments(arguments);

    return fragment;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    // Ensure the parent activity implements the proper listener interface.
    try {
      listener = (Listener) getActivity();
    } catch (ClassCastException e) {
      throw new ClassCastException(getActivity().toString()
          + " must implement EditAPISettingDialogFragment.Listener");
    }

  }

  @Override
  public void onDetach() {
    super.onDetach();
    // Remove reference to the listener interface.
    listener = null;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // Get database row ID of the object being edited (if any) from the arguments bundle.
    if (getArguments() != null && getArguments().containsKey(BUNDLE_ID_ROW_ID)) {
      rowId = getArguments().getLong(BUNDLE_ID_ROW_ID);
    }

    // Inflate XML for the dialog's main view.
    LayoutInflater inflater = LayoutInflater.from(getActivity());
    @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_edit_api_setting, null, false);

    // Get references to the parent view's subviews.
    name = (AutoCompleteTextView) view.findViewById(R.id.name);
    uri = (EditText) view.findViewById(R.id.uri);
    username = (EditText) view.findViewById(R.id.username);
    passphrase = (EditText) view.findViewById(R.id.passphrase);

    // Set service name autosuggestion adapter.
    name.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.simple_dropdown_item, getResources().getStringArray(R.array.service_suggestions_names)));
    name.setThreshold(1);
    name.setOnItemClickListener(this);

    // Set service URI TextChangedListener to show optional authentication fields when the Danbooru endpoint is used.
    uri.addTextChangedListener(this);
    // Set service URI OnFocusChangeListener to prepend "http://" to the text field when the user clicks on it.
    uri.setOnFocusChangeListener(this);

    // Populate views from an existing SearchClient.Settings object, if it was passed in the arguments bundle.
    if (savedInstanceState == null && getArguments() != null && getArguments().containsKey(BUNDLE_ID_SETTINGS)) {
      // Get the SearchClient.Settings object from this fragment's arguments bundle.
      SearchClient.Settings settings = getArguments().getParcelable(BUNDLE_ID_SETTINGS);

      // Populate subviews with content.
      name.setText(settings.getName());
      uri.setText(settings.getEndpoint());
      username.setText(settings.getUsername());
      passphrase.setText(settings.getPassword());
    }

    // Dismiss dropdown when the view is first shown.
    name.dismissDropDown();

    // Create the AlertDialog object.
    final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
        .setView(view)
        .setTitle(rowId == -1 ? R.string.dialog_title_addService : R.string.dialog_title_editService)
        .setPositiveButton(R.string.ok, null)
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            // Dismiss dialog without saving changes.
            dismiss();
          }
        }).create();

    // Use OnShowListener to override positive button behaviour.
    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialogInterface) {
        // OnShowListener here is used as a hack to override Android Dialog's default OnClickListener
        // that doesn't provide a way to prevent the dialog from getting dismissed when a button is clicked.
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(EditAPISettingDialogFragment.this);
      }
    });

    return alertDialog;
  }

  @Override
  public void onItemClick(AdapterView<?> listView, View view, int position, long itemId) {
    // This gets called when the user selects a service name autosuggestion.
    // If a known service name was selected, the uri can be auto-completed too.
    String name = (String) listView.getItemAtPosition(position);
    String[] serviceNames = getResources().getStringArray(R.array.service_suggestions_names);
    String[] serviceUris = getResources().getStringArray(R.array.service_suggestions_uris);

    for (int i = 0; i < serviceNames.length; i++) {
      if (serviceNames[i].equals(name)) {
        uri.setText(serviceUris[i]);
      }
    }
  }

  @Override
  // Called when the dialog's OK button is clicked.
  public void onClick(View view) {
    // Don't submit if any of the fields are empty. (Username and passphrase are always optional).
    // Additional validation must be done by the parent activity when #addService is called.
    if (name.getText().toString().isEmpty() || uri.getText().toString().isEmpty()
        || (username.getText().toString().isEmpty() != passphrase.getText().toString().isEmpty())) {
      return;
    }

    // Send input to the parent activity, so that it can be added or edited in the database.
    if (rowId < 0) {
      listener.addService(name.getText().toString(), uri.getText().toString(),
          username.getText().toString(), passphrase.getText().toString());
    } else {
      listener.editService(rowId, name.getText().toString(), uri.getText().toString(),
          username.getText().toString(), passphrase.getText().toString());
    }

    // Dismiss dialog.
    dismiss();
  }

  @Override
  public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

  }

  @Override
  public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    // Only show optional authentication inputs for the Danbooru API.
    if (uri.getText().toString().startsWith(DANBOORU_API_URL)) {
      username.setVisibility(View.VISIBLE);
      passphrase.setVisibility(View.VISIBLE);
    } else if (username.getVisibility() != View.GONE) {
      // Empty and hide fields.
      username.setText("");
      username.setVisibility(View.GONE);
      passphrase.setText("");
      passphrase.setVisibility(View.GONE);
    }
  }

  @Override
  public void afterTextChanged(Editable editable) {

  }

  @Override
  public void onFocusChange(View v, boolean hasFocus) {
    // Prepend "http://" to the text field when it's focused by the user to inform them that
    // the http schema prefix is required.
    if (hasFocus && uri.getText().toString().isEmpty()) {
      uri.setText("http://");
    }
  }

  /** Interface implemented by the parent activity to receive values from the dialog. */
  public static interface Listener {
    /**
     * Add a new service to the database.
     *
     * @param name       Service name.
     * @param url        Service endpoint uri.
     * @param username   Service authentication username (optional).
     * @param passphrase Service authentication passphrase (optional).
     */
    public void addService(String name, String url, String username, String passphrase);

    /**
     * Edit an existing service in the database.
     *
     * @param rowId      Database row ID.
     * @param name       Service name.
     * @param url        Service endpoint uri.
     * @param username   Service authentication username (optional).
     * @param passphrase Service authentication passphrase (optional).
     */
    public void editService(long rowId, String name, String url, String username, String passphrase);
  }
}
