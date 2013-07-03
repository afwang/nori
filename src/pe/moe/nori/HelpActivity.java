package pe.moe.nori;

import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class HelpActivity extends SherlockActivity {
  /** Online help URL */
  private static final String HELP_URL = "http://vomitcuddle.github.io/nori";
  /** Updates the progress bar */
  private final WebChromeClient mWebChromeClient = new WebChromeClient() {
    @Override
    public void onProgressChanged(WebView view, int newProgress) {
      super.onProgressChanged(view, newProgress);

      // Set the value for the ActionBar progress bar.
      if (newProgress < 100)
        setSupportProgressBarIndeterminateVisibility(true);
      if (newProgress == 100)
        setSupportProgressBarIndeterminateVisibility(false);
    }

  };
  /** Closes the activity on network error. */
  private final WebViewClient mWebViewClient = new WebViewClient() {
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
      // Show error message and close activity.
      Toast.makeText(HelpActivity.this, R.string.error_connection, Toast.LENGTH_SHORT).show();
      finish();
    }
  };

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // ActionBar back button.
        if ((getSupportActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0)
          onBackPressed();
        return true;
      default:
        return false;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Request window features.
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // Create WebView, set event listeners and enable JavaScript.
    final WebView mWebView = new WebView(this);
    mWebView.setWebChromeClient(mWebChromeClient);
    mWebView.setWebViewClient(mWebViewClient);
    final WebSettings webSettings = mWebView.getSettings();
    webSettings.setJavaScriptEnabled(true);

    // Show the WebView and load online help url.
    setContentView(mWebView);
    mWebView.loadUrl(HELP_URL);
  }
}
