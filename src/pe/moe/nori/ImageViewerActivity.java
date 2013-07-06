package pe.moe.nori;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import pe.moe.nori.api.BooruClient;
import pe.moe.nori.api.Image;
import pe.moe.nori.api.SearchResult;
import pe.moe.nori.providers.ServiceSettingsProvider;

public class ImageViewerActivity extends SherlockActivity implements ViewPager.OnPageChangeListener {
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
  /** Bitmap LRU cache */
  private LruCache<String, Bitmap> mBitmapLruCache = new LruCache<String, Bitmap>(4096) {
  private LruCache<String, Bitmap> mBitmapLruCache = new LruCache<String, Bitmap>(2048) {
    @Override
    protected int sizeOf(String key, Bitmap value) {
      // Convert size to kilobytes.
      return (int) ((long) value.getRowBytes() * (long) value.getHeight() / 1024);
    }
  };
  /** Volley image cache */
  private ImageLoader.ImageCache mImageCache = new ImageLoader.ImageCache() {
    @Override
    public Bitmap getBitmap(String url) {
      return mBitmapLruCache.get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
      mBitmapLruCache.put(url, bitmap);
    }
  };
  /** Response listener used for infinite scrolling. */
  private Response.Listener<SearchResult> mSearchResultListener = new Response.Listener<SearchResult>() {
    @Override
    public void onResponse(SearchResult response) {
      // Clear pending request and hide progress bar.
      mPendingRequest = null;
      setSupportProgressBarIndeterminateVisibility(false);
      // Extend SearchResult and notify ViewPager adapter.
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
  /** Volley image lazyloader */
  private ImageLoader mImageLoader;
  private ViewPager mViewPager;
  /** Used when fading out the ActionBar in #onPageScrollStateChanged */
  private boolean mShouldToggleActionBar = false;
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
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        .setVisibleInDownloadsUi(false);
    request.allowScanningByMediaScanner();
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

    // Dim system UI on Ice Cream Sandwich and above.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
      getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

    // Hide the ActionBar until user interacts with the activity.
    getSupportActionBar().hide();

    // Get SearchResult and API settings from Intent.
    mSearchResult = getIntent().getParcelableExtra("pe.moe.nori.api.SearchResult");
    mRequestQueue = Volley.newRequestQueue(this);
    mBooruClient = ServiceSettingsProvider.ServiceSettings.createClient(mRequestQueue,
        getIntent().<ServiceSettingsProvider.ServiceSettings>getParcelableExtra("pe.moe.nori.api.Settings"));
    mImageLoader = new ImageLoader(mRequestQueue, mImageCache);

    // Inflate content view.
    setContentView(R.layout.activity_imageviewer);
    mViewPager = (ViewPager) findViewById(R.id.pager);
    mViewPager.setAdapter(new SearchResultPagerAdapter(this, mSearchResult));
    mViewPager.setOffscreenPageLimit(5);
    mViewPager.setOffscreenPageLimit(2);
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
  public void onLowMemory() {
    super.onLowMemory();
    // Trim bitmap cache to reduce memory usage.
    mBitmapLruCache.trimToSize(32);
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
    stringBuilder.append(image.id);
    stringBuilder.append(": ");

    for (int i = 0; i < image.generalTags.length; i++) {
      stringBuilder.append(image.generalTags[i] + " ");
    }
    if (stringBuilder.length() > 25) {
      stringBuilder.setLength(24);
      stringBuilder.append("…");
    }

    setTitle(stringBuilder.toString());
  }

  @Override
  public void onPageScrollStateChanged(int i) {
    // Toggle the ActionBar when the ViewPager is touched, but not scrolled in either direction.
    if (i == 1) {
      // ViewPager touched.
      mShouldToggleActionBar = true;
    } else if (i == 2) {
      // ViewPager scrolled.
      mShouldToggleActionBar = false;
    } else if (i == 0 & mShouldToggleActionBar) {
      // ViewPager touched, but not scrolled.
      if (getSupportActionBar().isShowing())
        getSupportActionBar().hide();
      else
        getSupportActionBar().show();
    }
  }

  /** Adapter for the {@link ViewPager} used for flipping through the images. */
  private class SearchResultPagerAdapter extends PagerAdapter {
    private final Context mContext;
    private final SearchResult mSearchResult;

    public SearchResultPagerAdapter(Context context, SearchResult searchResult) {
      // Set search result and context.
      this.mSearchResult = searchResult;
      this.mContext = context;
    }

    @Override
    public NetworkImageView instantiateItem(ViewGroup container, int position) {
      // Create ImageView.
      final NetworkImageView networkImageView = new NetworkImageView(mContext);
      networkImageView.setLayoutParams(new AbsListView.LayoutParams(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.MATCH_PARENT));
      networkImageView.setImageUrl(mSearchResult.images.get(position).fileUrl, mImageLoader);

      // Add ImageView to the View container.
      container.addView(networkImageView);
      return networkImageView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
      // Remove View from the container.
      container.removeView((View) object);
    }

    @Override
    public int getCount() {
      return mSearchResult.images.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
      // Checks if View corresponds to the given object returned from #instantiateItem()
      // Just check if view == o, since #instantiateItem() returns the view anyways.
      return view == o;
    }
  }
}
