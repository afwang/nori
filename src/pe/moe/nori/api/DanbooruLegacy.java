package pe.moe.nori.api;

import com.android.volley.*;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Legacy Danbooru API client (Gelbooru/Moebooru/Shimmie2) */
public abstract class DanbooruLegacy implements BooruClient {
  protected static final int DEFAULT_LIMIT = 100;
  private static final Pattern PIXIV_ID_FROM_URL_PATTERN =
      Pattern.compile("http://(?:www|i\\d)\\.pixiv\\.net/.+?(?:illust_id=|img/.+?/)(\\d+)");
  protected final String mApiEndpoint;
  private final RequestQueue mRequestQueue;
  private final String username;
  private final String password;

  /**
   * Creates a new instance of the Danbooru Legacy API client.
   *
   * @param endpoint     URL endpoint of the API endpoint (example: http://yande.re), doesn't include paths or trailing slashes
   * @param requestQueue Android Volley {@link RequestQueue}
   */
  public DanbooruLegacy(String endpoint, RequestQueue requestQueue) {
    mApiEndpoint = endpoint;
    mRequestQueue = requestQueue;
    // No authentication needed.
    username = null;
    password = null;
  }

  /**
   * Creates a new instance of the Danbooru Legacy API client.
   *
   * @param endpoint     URL of the API endpoint (example: http://yande.re), doesn't include path or trailing slashes
   * @param requestQueue Android Volley {@link RequestQueue}
   * @param username     HTTP Basic Auth username
   * @param password     HTTP Basic Auth password
   */
  public DanbooruLegacy(String endpoint, RequestQueue requestQueue, String username, String password) {
    mApiEndpoint = endpoint;
    mRequestQueue = requestQueue;
    // Set credentials.
    this.username = username;
    this.password = password;
  }

  /**
   * Extract Pixiv ID from a source URL.
   *
   * @param sourceUrl {@link Image} source URL.
   * @return Returns Pixiv ID extracted from given URL, or -1 if not found.
   */
  public static long getPixivIdFromSourceUrl(String sourceUrl) {
    if (sourceUrl == null || sourceUrl.isEmpty())
      return -1L;

    final Matcher matcher = PIXIV_ID_FROM_URL_PATTERN.matcher(sourceUrl);
    if (matcher.find()) {
      return Long.parseLong(matcher.group(1));
    }

    return -1L;
  }

  /*
   * {@inheritDoc}
   */
  @Override
  public String getDefaultQuery() {
    return "";
  }

  /**
   * Parses an HTTP response body into a {@link SearchResult}
   *
   * @param data HTTP response body containing a valid XML document
   * @return Returns a {@link SearchResult} containing data found in the XML document
   */
  protected abstract SearchResult parseResultResponse(String data) throws XmlPullParserException, IOException, ParseException;

  /**
   * Checks if API requires authentication.
   *
   * @return Returns true if the API require HTTP basic authentication
   */
  @Override
  public boolean requiresAuthentication() {
    return (username != null && password != null);
  }

  /*
   * {@inheritDoc}
   */
  @Override
  public abstract Request<SearchResult> searchRequest(String query, int pid, Listener<SearchResult> listener, ErrorListener errorListener);

  /*
   * {@inheritDoc}
   */
  @Override
  public abstract Request<SearchResult> searchRequest(String query, Listener<SearchResult> listener, ErrorListener errorListener);

  protected class SearchResultRequest extends Request<SearchResult> {
    private final Listener<SearchResult> mListener;
    private final String mQuery;

    /**
     * Creates a new HTTP GET Request
     *
     * @param url           URL to fetch
     * @param listener      Listener to receive the {@link SearchResult} response
     * @param errorListener Error listener, or null to ignore errors
     */
    public SearchResultRequest(String url, String query, Listener<SearchResult> listener, ErrorListener errorListener) {
      super(Method.GET, url, errorListener);
      mListener = listener;
      mQuery = query;
      setRequestQueue(mRequestQueue);
    }

    @Override
    protected void deliverResponse(SearchResult response) {
      // Append search query to response.
      if (response != null)
        response.query = mQuery;

      if (mListener != null)
        mListener.onResponse(response);
    }

    @Override
    protected Response<SearchResult> parseNetworkResponse(NetworkResponse response) {
      // Try parsing the XML or return error.
      try {
        return Response.success(parseResultResponse(new String(response.data, HttpHeaderParser.parseCharset(response.headers))),
            HttpHeaderParser.parseCacheHeaders(response));
      } catch (Exception e) {
        return Response.error(new VolleyError("Error processing data."));
      }
    }

  }

}
