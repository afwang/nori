/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.fragment;

import android.app.DownloadManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cuddlesoft.nori.R;
import com.cuddlesoft.nori.util.NetworkUtils;
import com.cuddlesoft.norilib.Image;
import com.ortiz.touch.TouchImageView;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/** Fragment used to display images in {@link com.cuddlesoft.nori.ImageViewerActivity}. */
public class ImageFragment extends Fragment {
  /** String to prepend to Pixiv IDs to open them in the system web browser. */
  private static final String PIXIV_URL_PREFIX = "http://www.pixiv.net/member_illust.php?mode=medium&illust_id=";
  /** Bundle identifier used to save the displayed image object in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_IMAGE = "com.cuddlesoft.nori.Image";
  /** Image view used to show the image in this fragment. */
  private TouchImageView imageView;
  /** Image object displayed in this fragment. */
  private Image image;

  /** Required empty public constructor. */
  public ImageFragment() {
  }

  /**
   * Factory method used to construct new fragments.
   *
   * @param image Image to display in the created fragment.
   * @return Fragment instance displaying the given image.
   */
  @SuppressWarnings("TypeMayBeWeakened")
  public static ImageFragment newInstance(Image image) {
    // Create new instance of the fragment and add the image to the fragment's arguments bundle.
    final ImageFragment fragment = new ImageFragment();
    final Bundle arguments = new Bundle();
    arguments.putParcelable(BUNDLE_ID_IMAGE, image);
    fragment.setArguments(arguments);
    return fragment;
  }

  /**
   * Check if the {@link android.support.v4.view.ViewPager} containing this fragment can scroll horizontally.
   *
   * @param direction Direction being scrolled in.
   * @return true if the {@link android.support.v4.view.ViewPager} containing this fragment can scroll horizontally.
   */
  public boolean canScroll(int direction) {
    return imageView == null || imageView.canScrollHorizontallyFroyo(direction);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Extract image from arguments bundle and initialize ImageView.
    imageView = new TouchImageView(getActivity());
    image = getArguments().getParcelable(BUNDLE_ID_IMAGE);

    // Evaluate the current network conditions to decide whether to load the medium-resolution
    // (sample) version of the image or the original full-resolution one.
    final String imageUrl;
    if (NetworkUtils.shouldFetchImageSamples(getActivity())) {
      imageUrl = image.sampleUrl;
    } else {
      imageUrl = image.fileUrl;
    }

    // Load image into view.
    Picasso.with(getActivity())
        .load(imageUrl)
        .into(imageView);

    // Enable options menu.
    setHasOptionsMenu(true);

    return imageView;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.image, menu);

    // Set up ShareActionProvider
    MenuItem shareItem = menu.findItem(R.id.action_share);
    ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
    shareActionProvider.setShareIntent(getShareIntent());

    // Hide the view on Pixiv menu item, if the Image does not have a Pixiv source URL.
    MenuItem shareOnPixivItem = menu.findItem(R.id.action_viewOnPixiv);
    if (image.pixivId == null || TextUtils.isEmpty(image.pixivId)) {
      shareOnPixivItem.setVisible(false);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_download:
        downloadImage();
        return true;
      case R.id.action_viewOnWeb:
        viewOnWeb();
        return true;
      case R.id.action_viewOnPixiv:
        viewOnPixiv();
        return true;
      case R.id.action_setAsWallpaper:
        setAsWallpaper();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Get {@link android.content.Intent} to be sent by the {@link android.support.v7.widget.ShareActionProvider}.
   * @return Share intent.
   */
  protected Intent getShareIntent() {
    // Send web URL to image.
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_TEXT, image.webUrl);
    intent.setType("text/plain");
    return intent;
  }

  /**
   * Use the system {@link android.app.DownloadManager} service to download the image.
   */
  protected void downloadImage() {
    // Get download manager system service.
    DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);

    // Extract file name from URL.
    String fileName = image.fileUrl.substring(image.fileUrl.lastIndexOf("/")+1);
    // Create download directory, if it does not already exist.
    //noinspection ResultOfMethodCallIgnored
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs();

    // Create and queue download request.
    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(image.fileUrl))
        .setTitle(fileName)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        .setVisibleInDownloadsUi(true);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      // Trigger media scanner to add image to system gallery app on Honeycomb and above.
      request.allowScanningByMediaScanner();
      // Show download UI notification on Honeycomb and above.
      request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
    }
    downloadManager.enqueue(request);
  }

  /**
   * Opens the image Danbooru page in the system web browser.
   */
  protected void viewOnWeb() {
    // Create and send intent to display the image in the web browser.
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(image.webUrl));
    startActivity(intent);
  }

  /**
   * Opens the image Pixiv page in the system web browser.
   */
  protected void viewOnPixiv() {
    // Create and send to intent to display the image's pixiv page in the web browser.
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PIXIV_URL_PREFIX + image.pixivId));
    startActivity(intent);
  }

  /**
   * Downloads the full-resolution image in the background and sets it as the wallpaper.
   */
  protected void setAsWallpaper() {
    // Fetch and set full-screen image as wallpaper on background thread.
    final Context context = getActivity();
    final String imageUrl = image.fileUrl;
    final WallpaperManager wallpaperManager = WallpaperManager.getInstance(getActivity());

    new AsyncTask<Void, Void, Exception>() {
      @Override
      protected Exception doInBackground(Void... ignored) {
        try {
          InputStream inputStream = new URL(imageUrl).openStream();
          wallpaperManager.setStream(inputStream);
        } catch (IOException e) {
          return e;
        }
        return null;
      }

      @Override
      protected void onPostExecute(Exception error) {
        if (error != null) {
          // Show error message to the user.
          Toast.makeText(context, String.format(context.getString(R.string.toast_couldNotSetWallpaper),
              error.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
      }
    }.execute();
  }

}
