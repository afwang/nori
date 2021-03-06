/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.cuddlesoft.nori.R;

/** {@link android.support.v4.app.DialogFragment} to let the users add new tags in {@link com.cuddlesoft.nori.TagFilterSettingsActivity}. */
public class AddTagFilterDialogFragment extends DialogFragment implements View.OnClickListener {
  /** Regular expression pattern used to validate tags before they are added to the database. */
  private static final String TAG_VALIDATION_PATTERN = "^\\S+$";
  /** Bundle ID used to preserve the entered text and send results to the parent activity. */
  protected static final String BUNDLE_ID_TAG_FILTER = "com.cuddlesoft.nori.TagFilter";
  /** Text box with tag to add to the tag filter list. */
  private EditText tagEditText;
  /** Activity or Fragment listening for the result from this dialog. */
  private AddTagListener listener;


  /** Required empty constructor. */
  public AddTagFilterDialogFragment() {
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      listener = (AddTagListener) getActivity();
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString()
          + "must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    listener = null;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    // Preserve the entered text.
    if (tagEditText != null && !TextUtils.isEmpty(tagEditText.getText().toString())) {
      outState.putString(BUNDLE_ID_TAG_FILTER, tagEditText.getText().toString());
    }
  }

  @SuppressLint("InflateParams")
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // Inflate the Dialog view XML.
    final LayoutInflater inflater = LayoutInflater.from(getActivity());
    final View view = inflater.inflate(R.layout.dialog_add_tag_filter, null);
    tagEditText = (EditText) view.findViewById(R.id.editText);

    // Restore preserved text from savedInstanceState, if possible.
    if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_ID_TAG_FILTER)) {
      tagEditText.setText(savedInstanceState.getString(BUNDLE_ID_TAG_FILTER));
    }

    // Create the AlertDialog object.
    final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
        .setView(view)
        .setPositiveButton(R.string.action_add, null)
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                // Dismiss dialog.
                dismiss();
              }
            }
        ).create();

    // onShowListener is used here as a hack to override Android DialogInterface's default onClickInterface
    // that doesn't provide a way to prevent the dialog from getting dismissed when a button is clicked.
    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialogInterface) {
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(AddTagFilterDialogFragment.this);
      }
    });

    return alertDialog;
  }

  @Override
  // Called when the dialog's OK button is clicked.
  public void onClick(View view) {
    if (listener != null && tagEditText != null) {
      // Verify that the entered text isn't empty and doesn't contain illegal whitespace characters.
      String tag = tagEditText.getText().toString().trim();
      if (!TextUtils.isEmpty(tag) && tag.matches(TAG_VALIDATION_PATTERN)) {
        listener.addTag(tagEditText.getText().toString());
        dismiss();
      }
    }
  }

  /** Listener interface to be implemented by the Activity or Fragment that receives the tag entered in this Dialog. */
  public interface AddTagListener {
    /**
     * Called when the user submits a new Tag to be added to the list.
     *
     * @param tag Tag string submitted.
     */
    public void addTag(String tag);
  }
}
