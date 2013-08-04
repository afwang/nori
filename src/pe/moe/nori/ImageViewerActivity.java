package pe.moe.nori;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.analytics.tracking.android.EasyTracker;
import pe.moe.nori.api.BooruClient;
import pe.moe.nori.api.Image;
import pe.moe.nori.api.SearchResult;
import pe.moe.nori.fragments.TagListDialogFragment;
import pe.moe.nori.providers.ServiceSettingsProvider;
import pe.moe.nori.widgets.BasePagerAdapter;
import pe.moe.nori.widgets.TouchImageViewPager;
import pe.moe.nori.widgets.UrlTouchImageView;

public class ImageViewerActivity extends SherlockFragmentActivity implements ViewPager.OnPageChangeListener {
  /** Application settings */
  private SharedPreferences mSharedPreferences;
  /** Current SearchResult */
  private SearchResult mSearchResult;
  /** HTTP request queue */
  private RequestQueue mRequestQueue;
  /** API client */
  private BooruClient mBooruClient;
  /** Pending API request */
  private Request<SearchResult> mPendingRequest;
  /** Share {@link Intent} provider for the Share button */
  private ShareActionProvider mShareActionProvider = new ShareActionProvider(this);
  /** Response listener used for infinite scrolling. */
  private Response.Listener<SearchResult> mSearchResultListener = new Response.Listener<SearchResult>() {
    @Override
    public void onResponse(SearchResult response) {
      // Clear pending request and hide progress bar.
      mPendingRequest = null;
      setSupportProgressBarIndeterminateVisibility(false);
      // Extend SearchResult and notify ViewPager adapter.
      response.filter(mSharedPreferences.getString("search_safety_rating", getString(R.string.preference_safetyRating_default)));
      mSearchResult.extend(response);
      mViewPager.getAdapter().notifyDataSetChanged();
    }
  };
  /** API connection error listener */
  private Response.ErrorListener mResponseErrorListener = new Response.ErrorListener() {
    @Override
    public void onErrorResponse(VolleyError error) {
      Toast.makeText(ImageViewerActivity.this, R.string.error_connection, Toast.LENGTH_SHORT).show();
    }
  };
  private ViewPager mViewPager;
  /** This must be hidden when Pixiv ID isn't available for given item. */
  private MenuItem mViewOnPixivMenuItem;

  private void downloadCurrentItem() {
    final DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
    final Image image = mSearchResult.images.get(mViewPager.getCurrentItem());
    final String fileName = image.fileUrl.substring(image.fileUrl.lastIndexOf("/") + 1);

    // Create download directory if it doesn't exist.
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs();

    // Create and enqueue request.
    final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(image.fileUrl))
        .setTitle(fileName)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        .setVisibleInDownloadsUi(false);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      request.allowScanningByMediaScanner();
      request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
    }
    downloadManager.enqueue(request);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Request Window features.
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

    final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    if (sharedPreferences.getBoolean("imageViewer_keepScreenOn", false))
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    // Hide the ActionBar until user interacts with the activity.
    getSupportActionBar().hide();

    // Get shared preferences.
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    // Get SearchResult and API settings from Intent.
    mSearchResult = getIntent().getParcelableExtra("pe.moe.nori.api.SearchResult");
    mRequestQueue = Volley.newRequestQueue(this);
    mBooruClient = ServiceSettingsProvider.ServiceSettings.createClient(mRequestQueue,
        getIntent().<ServiceSettingsProvider.ServiceSettings>getParcelableExtra("pe.moe.nori.api.Settings"));

    // Inflate content view.
    setContentView(R.layout.activity_imageviewer);
    setSupportProgressBarIndeterminateVisibility(false);
    mViewPager = (ViewPager) findViewById(R.id.pager);
    mViewPager.setAdapter(new SearchResultPagerAdapter());
    mViewPager.setOnPageChangeListener(this);

    // Load current position from Intent if not restored from instance state.
    if (savedInstanceState == null) {
      final int position = getIntent().getIntExtra("pe.moe.nori.api.SearchResult.position", 0);
      mViewPager.setCurrentItem(position);
      // Make sure OnPageChangeListener is called even when position == 0.
      if (position == 0)
        onPageSelected(0);
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    mSearchResult = intent.getParcelableExtra("pe.moe.nori.api.SearchResult");
    mBooruClient = ServiceSettingsProvider.ServiceSettings.createClient(mRequestQueue,
        intent.<ServiceSettingsProvider.ServiceSettings>getParcelableExtra("pe.moe.nori.api.Settings"));
    mViewPager.getAdapter().notifyDataSetChanged();
    final int position = intent.getIntExtra("pe.moe.nori.api.SearchResult.position", 0);
    mViewPager.setCurrentItem(position);
    // Make sure OnPageChangeListener is called even when position == 0
    if (position == 0)
      onPageSelected(0);
  }

  @Override
  protected void onStart() {
    super.onStart();
    EasyTracker.getInstance().activityStart(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    EasyTracker.getInstance().activityStop(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getSupportMenuInflater().inflate(R.menu.imageviewer, menu);
    mViewOnPixivMenuItem = menu.findItem(R.id.action_viewOnPixiv);
    // Show "View on Pixiv" menu item if Pixiv ID is available.
    if (mSearchResult.images.get(mViewPager.getCurrentItem()).pixivId != -1)
      mViewOnPixivMenuItem.setVisible(true);

    // Set ActionProvider for the Share button.
    menu.findItem(R.id.action_share).setActionProvider(mShareActionProvider);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      case R.id.action_download:
        downloadCurrentItem();
        return true;
      case R.id.action_showTags:
        new TagListDialogFragment(mSearchResult.images.get(mViewPager.getCurrentItem())).show(getSupportFragmentManager(),
            "TagListDialog");
        return true;
      case R.id.action_viewOnWeb:
        startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse(mSearchResult.images.get(mViewPager.getCurrentItem()).webUrl)));
        return true;
      case R.id.action_viewOnPixiv:
        startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse("http://www.pixiv.net/member_illust.php?mode=medium&illust_id="
                + Long.toString(mSearchResult.images.get(mViewPager.getCurrentItem()).pixivId))));
        return true;
      default:
        return false;
    }
  }

  @Override
  public void onPageScrolled(int i, float v, int i2) {
  }

  @Override
  public void onPageSelected(int pos) {
    final Image image = mSearchResult.images.get(pos);

    // Infinite scrolling
    if (mPendingRequest == null && (mSearchResult.images.size() - pos) <= 3 && mSearchResult.hasMore()) {
      // Create API request and add it to the queue.
      setSupportProgressBarIndeterminateVisibility(true);
      mPendingRequest = mBooruClient.searchRequest(mSearchResult.query, mSearchResult.pageNumber + 1,
          mSearchResultListener, mResponseErrorListener);
      mRequestQueue.add(mPendingRequest);
    }

    // Show "View on Pixiv" menu item if Pixiv id available.
    if (mViewOnPixivMenuItem != null)
      if (image.pixivId == -1L)
        mViewOnPixivMenuItem.setVisible(false);
      else
        mViewOnPixivMenuItem.setVisible(true);

    // Set share intent for the share button.
    Intent shareIntent = new Intent(Intent.ACTION_SEND)
        .putExtra(Intent.EXTRA_TEXT, image.webUrl)
        .setType("text/plain");
    mShareActionProvider.setShareIntent(shareIntent);

    // Set activity title.
    final StringBuilder stringBuilder = new StringBuilder();

    for (int i = 0; i < image.generalTags.length; i++) {
      stringBuilder.append(image.generalTags[i] + " ");
    }
    if (stringBuilder.length() > 45) {
      stringBuilder.setLength(44);
      stringBuilder.append("…");
    }

    setTitle(stringBuilder.toString());
  }

  @Override
  public void onPageScrollStateChanged(int i) {

  }

  /** Adapter for the {@link ViewPager} used for flipping through the images. */
  private class SearchResultPagerAdapter extends BasePagerAdapter {

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
      super.setPrimaryItem(container, position, object);
      ((TouchImageViewPager) container).mCurrentView = ((UrlTouchImageView) object).getImageView();
    }

    @Override
    public int getCount() {
      if (mSearchResult != null)
        return mSearchResult.images.size();
      else
        return 0;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
      final UrlTouchImageView iv = new UrlTouchImageView(ImageViewerActivity.this);
      iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      container.addView(iv, 0);
      iv.setUrl(mSearchResult.images.get(position));
      return iv;
    }
  }
}
