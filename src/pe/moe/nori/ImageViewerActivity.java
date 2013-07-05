package pe.moe.nori;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import com.actionbarsherlock.app.SherlockActivity;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import pe.moe.nori.api.BooruClient;
import pe.moe.nori.api.SearchResult;
import pe.moe.nori.providers.ServiceSettingsProvider;

public class ImageViewerActivity extends SherlockActivity {
  /** Current SearchResult */
  private SearchResult mSearchResult;
  /** HTTP request queue */
  private RequestQueue mRequestQueue;
  /** API client */
  private BooruClient mBooruClient;
  /** Bitmap LRU cache */
  private LruCache<String, Bitmap> mBitmapLruCache = new LruCache<String, Bitmap>(4096) {
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
  /** Volley image lazyloader */
  private ImageLoader mImageLoader;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Get SearchResult and API settings from Intent.
    mSearchResult = getIntent().getParcelableExtra("pe.moe.nori.api.SearchResult");
    mRequestQueue = Volley.newRequestQueue(this);
    mBooruClient = ServiceSettingsProvider.ServiceSettings.createClient(mRequestQueue,
        getIntent().<ServiceSettingsProvider.ServiceSettings>getParcelableExtra("pe.moe.nori.api.Settings"));
    mImageLoader = new ImageLoader(mRequestQueue, mImageCache);
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    // Trim bitmap cache to reduce memory usage.
    mBitmapLruCache.trimToSize(32);
  }
}
