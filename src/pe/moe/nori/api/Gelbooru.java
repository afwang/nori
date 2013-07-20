package pe.moe.nori.api;

import android.net.Uri;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** API client for the Gelbooru API */
public class Gelbooru extends DanbooruLegacy {
  /** Date format used by Gelbooru */
  protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy", Locale.US);

  /**
   * Create a Gelbooru API client.
   *
   * @param endpoint     API endpoint URI (e.g. http://gelbooru.com)
   * @param requestQueue Volley HTTP {@link RequestQueue}
   */
  public Gelbooru(String endpoint, RequestQueue requestQueue) {
    super(endpoint, requestQueue);
  }

  @Override
  protected String getWebUrlFromId(long id) {
    return mApiEndpoint + "/index.php?page=post&s=view&id=" + id;
  }

  @Override
  protected Date parseDate(String date) throws ParseException {
    return DATE_FORMAT.parse(date);
  }

  @Override
  public Request<SearchResult> searchRequest(String query, Response.Listener<SearchResult> listener, Response.ErrorListener errorListener) {
    final String url = String.format(Locale.US, mApiEndpoint + "/index.php?page=dapi&s=post&q=index&tags=%s&limit=%d", Uri.encode(query), DEFAULT_LIMIT);
    return new SearchResultRequest(url, query, listener, errorListener);
  }

  @Override
  public Request<SearchResult> searchRequest(String query, int pid, Response.Listener<SearchResult> listener, Response.ErrorListener errorListener) {
    final String url = String.format(Locale.US, mApiEndpoint + "/index.php?page=dapi&s=post&q=index&tags=%s&pid=%d&limit=%d", Uri.encode(query), pid, DEFAULT_LIMIT);
    return new SearchResultRequest(url, query, listener, errorListener);
  }
}