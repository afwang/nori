package pe.moe.nori.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import pe.moe.nori.R;
import pe.moe.nori.SearchActivity;
import pe.moe.nori.api.Image;

import java.util.ArrayList;
import java.util.Arrays;

public class TagListDialogFragment extends SherlockDialogFragment {
  /** Image */
  private Image mImage;

  /** Default constructor. (Needed by {@link android.support.v4.app.FragmentManager}) */
  public TagListDialogFragment() {
  }

  /**
   * Create a new fragment displaying tags for given image.
   *
   * @param image Image with tags to display in the list
   */
  public TagListDialogFragment(Image image) {
    mImage = image;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    // Save Image to bundle.
    if (mImage != null)
      outState.putParcelable("tagdialog_image", mImage);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // Restore image from instance state.
    if (savedInstanceState != null && savedInstanceState.containsKey("tagdialog_image")) {
      mImage = savedInstanceState.getParcelable("tagdialog_image");
    }

    // Create ListView.
    ListView listView = new ListView(getSherlockActivity());
    TagListAdapter tagListAdapter = new TagListAdapter(getSherlockActivity(), mImage);
    listView.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
    listView.setAdapter(tagListAdapter);
    listView.setOnItemClickListener(tagListAdapter.onTagClickListener);

    // Create dialog.
    AlertDialog.Builder builder = new AlertDialog.Builder(getSherlockActivity());
    builder.setView(listView);
    return builder.create();
  }

  /** Adapter used in the tag {@link ListView}. */
  private static class TagListAdapter extends BaseAdapter {
    private final Image mImage;
    private final ArrayList<String> mTagList;
    private final Context mContext;
    /** Starts search when tag is clicked */
    public AdapterView.OnItemClickListener onTagClickListener = new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Search for tag.
        final Intent intent = new Intent(mContext, SearchActivity.class);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra("pe.moe.nori.Search.query", getItem(position));
        mContext.startActivity(intent);
      }
    };

    /**
     * Create a new TagListAdapter
     *
     * @param context Context
     * @param image   Image with tags to display.
     */
    public TagListAdapter(Context context, Image image) {
      mContext = context;
      mImage = image;

      if (image == null) {
        mTagList = new ArrayList<String>(0);
      } else {
        // Populate tag list.
        mTagList = new ArrayList<String>();
        if (image.copyrightTags != null)
          mTagList.addAll(Arrays.asList(image.copyrightTags));
        if (image.characterTags != null)
          mTagList.addAll(Arrays.asList(image.characterTags));
        if (image.artistTags != null)
          mTagList.addAll(Arrays.asList(image.artistTags));
        if (image.generalTags != null)
          mTagList.addAll(Arrays.asList(image.generalTags));
      }
    }

    /**
     * Sets text color depending on tag type.
     *
     * @param position Item position
     * @param textView TextView to set color of
     */
    private void setItemTextColor(int position, TextView textView) {
      if (mImage.copyrightTags != null && position < mImage.copyrightTags.length)
        textView.setTextColor(mContext.getResources().getColor(R.color.tag_copyright));
      else if (mImage.characterTags != null &&
          position < (mImage.copyrightTags == null ? 0 : mImage.copyrightTags.length) + mImage.characterTags.length)
        textView.setTextColor(mContext.getResources().getColor(R.color.tag_character));
      else if (mImage.artistTags != null &&
          position < (mImage.copyrightTags == null ? 0 : mImage.copyrightTags.length) + (mImage.characterTags == null ? 0 : mImage.characterTags.length) + mImage.artistTags.length)
        textView.setTextColor(mContext.getResources().getColor(R.color.tag_artist));
    }

    @Override
    public int getCount() {
      return mTagList.size();
    }

    @Override
    public String getItem(int position) {
      return mTagList.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // Inflate TextView
      final TextView textView = (TextView) ((SherlockFragmentActivity) mContext).getLayoutInflater()
          .inflate(android.R.layout.simple_list_item_1, parent, false);

      // Set color and text.
      setItemTextColor(position, textView);
      textView.setText(getItem(position));

      return textView;
    }
  }
}
