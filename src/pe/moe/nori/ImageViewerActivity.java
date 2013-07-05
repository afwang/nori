package pe.moe.nori;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockActivity;
import pe.moe.nori.api.SearchResult;

public class ImageViewerActivity extends SherlockActivity {
  private SearchResult mSearchResult;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Get SearchResult and API settings from Intent.
    mSearchResult = getIntent().getParcelableExtra("pe.moe.nori.api.SearchResult");
    setContentView(null);
  }
}
