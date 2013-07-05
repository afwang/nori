package pe.moe.nori;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockActivity;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import pe.moe.nori.api.BooruClient;
import pe.moe.nori.api.SearchResult;
import pe.moe.nori.providers.ServiceSettingsProvider;

public class ImageViewerActivity extends SherlockActivity {
  private SearchResult mSearchResult;
  private RequestQueue mRequestQueue;
  private BooruClient mBooruClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Get SearchResult and API settings from Intent.
    mSearchResult = getIntent().getParcelableExtra("pe.moe.nori.api.SearchResult");
    mRequestQueue = Volley.newRequestQueue(this);
    mBooruClient = ServiceSettingsProvider.ServiceSettings.createClient(mRequestQueue,
        getIntent().<ServiceSettingsProvider.ServiceSettings>getParcelableExtra("pe.moe.nori.api.Settings"));
  }

}
