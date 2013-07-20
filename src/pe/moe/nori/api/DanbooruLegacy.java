package pe.moe.nori.api;

import android.net.Uri;
import com.android.volley.*;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Legacy Danbooru API client (Gelbooru/Moebooru/Shimmie2) */
public class DanbooruLegacy implements BooruClient {
  protected static final int DEFAULT_LIMIT = 100;
  protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
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

  /**
   * Returns Web URL from image ID.
   *
   * @param id Image ID.
   * @return URL to the image on the web.
   */
  protected String getWebUrlFromId(long id) {
    return mApiEndpoint + "/post/show/" + id;
  }

  /**
   * Parse date from string.
   *
   * @param date Date as it appears in the API.
   * @return Parsed date.
   * @throws ParseException
   */
  protected Date parseDate(String date) throws ParseException {
    return DATE_FORMAT.parse(date);
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
  protected SearchResult parseResultResponse(String data) throws XmlPullParserException, IOException, ParseException {
    // Make sure data isn't null or empty.
    if (data == null || data.equals(""))
      return null;

    // Create new SearchResult.
    SearchResult searchResult = new SearchResult();

    // Create XML parser.
    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    XmlPullParser xpp = factory.newPullParser();
    xpp.setInput(new StringReader(data));
    int eventType = xpp.getEventType();

    // Loop over XML elements.
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        if (xpp.getName().equals("posts")) { // Root tag.
          // Parse attributes.
          for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (xpp.getAttributeValue(i).equals("count")) // Total image count across all pages.
              searchResult.count = Long.parseLong(xpp.getAttributeValue(i));
            else if (xpp.getAttributeValue(i).equals("offset")) // Offset, used for paging.
              searchResult.offset = Long.parseLong(xpp.getAttributeValue(i));
          }
        } else if (xpp.getName().equals("post")) { // Image tag.
          // Create new image.
          Image image = new Image();

          // Parse attributes.
          for (int i = 0; i < xpp.getAttributeCount(); i++) {
            // Get attribute name and value.
            String name = xpp.getAttributeName(i);
            String value = xpp.getAttributeValue(i);

            if (name.equals("file_url")) // Image URL
              image.fileUrl = value;
            else if (name.equals("width")) // Image width
              image.width = Integer.parseInt(value);
            else if (name.equals("height")) // Image height
              image.height = Integer.parseInt(value);

            else if (name.equals("preview_url")) // Thumbnail URL
              image.previewUrl = value;
            else if (name.equals("preview_width")) // Thumbnail width
              image.previewWidth = Integer.parseInt(value);
            else if (name.equals("preview_height")) // Thumbnail height
              image.previewHeight = Integer.parseInt(value);

            else if (name.equals("sample_url")) // Sample URL
              image.sampleUrl = value;
            else if (name.equals("sample_width")) // Sample width
              image.sampleWidth = Integer.parseInt(value);
            else if (name.equals("sample_height")) // Sample height
              image.sampleHeight = Integer.parseInt(value);

            else if (name.equals("id")) // Image ID
              image.id = Long.parseLong(value);
            else if (name.equals("parent_id")) // Image parent ID
              image.parentId = value.equals("") ? -1 : Long.parseLong(value);

            else if (name.equals("tags")) // Tags
              image.generalTags = value.trim().split(" ");

            else if (name.equals("rating")) { // Obscenity rating
              if (value.equals("s")) // Safe for work
                image.obscenityRating = Image.ObscenityRating.SAFE;
              else if (value.equals("q")) // Ambiguous
                image.obscenityRating = Image.ObscenityRating.QUESTIONABLE;
              else if (value.equals("e")) // Not safe for work
                image.obscenityRating = Image.ObscenityRating.EXPLICIT;
              else // Unknown / undefined
                image.obscenityRating = Image.ObscenityRating.UNDEFINED;
            } else if (name.equals("score")) // Popularity score
              image.score = Integer.parseInt(value);
            else if (name.equals("source")) // Source URL
              image.source = value;
            else if (name.equals("md5")) // MD5 checksum
              image.md5 = value;

            else if (name.equals("created_at")) // Creation date
              image.createdAt = parseDate(value);
            else if (name.equals("has_comments")) // Has comments
              image.hasComments = value.equals("true");
          }

          // Append web URL.
          image.webUrl = getWebUrlFromId(image.id);
          // Append Pixiv ID.
          image.pixivId = getPixivIdFromSourceUrl(image.source);

          // Add image to results.
          searchResult.images.add(image);
        }
      }
      // Get next XML element.
      eventType = xpp.next();
    }
    return searchResult;
  }

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
  public Request<SearchResult> searchRequest(String query, int pid, Listener<SearchResult> listener, ErrorListener errorListener) {
    final String url = String.format(Locale.US, mApiEndpoint + "/post/index.xml?tags=%s&limit=%d&page=&d", Uri.encode(query), DEFAULT_LIMIT, pid + 1);
    return new SearchResultRequest(url, query, listener, errorListener);
  }

  /*
   * {@inheritDoc}
   */
  @Override
  public Request<SearchResult> searchRequest(String query, Listener<SearchResult> listener, ErrorListener errorListener) {
    final String url = String.format(Locale.US, mApiEndpoint + "/post/index.xml?tags=%s&limit=%d", Uri.encode(query), DEFAULT_LIMIT);
    return new SearchResultRequest(url, query, listener, errorListener);
  }

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
