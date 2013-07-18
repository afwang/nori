package pe.moe.nori.api;

import android.net.Uri;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** API client for the Shimmie2 Danbooru API */
public class Shimmie extends DanbooruLegacy {
  /** Date format used in Shimmie2's API */
  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

  public Shimmie(String endpoint, RequestQueue requestQueue) {
    super(endpoint, requestQueue);
  }

  @Override
  public Request<SearchResult> searchRequest(String query, Response.Listener<SearchResult> listener, Response.ErrorListener errorListener) {
    final String url = String.format(Locale.US, mApiEndpoint + "/api/danbooru/find_posts/index.xml?tags=%s&limit=%d", Uri.encode(query), DEFAULT_LIMIT);
    return new SearchResultRequest(url, query, listener, errorListener);
  }

  @Override
  public Request<SearchResult> searchRequest(String query, int pid, Response.Listener<SearchResult> listener, Response.ErrorListener errorListener) {
    final String url = String.format(Locale.US, mApiEndpoint + "/api/danbooru/find_posts/index.xml?tags=%s&page=%d&limit=%d", Uri.encode(query), pid + 1, DEFAULT_LIMIT);
    return new SearchResultRequest(url, query, listener, errorListener);
  }

  @Override
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

          // Workaround for older versions of the API.
          if (xpp.getAttributeCount() == 0) {
            // HACK: Force querying next page
            searchResult.count = DEFAULT_LIMIT * 2;
            searchResult.offset = 0;
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
              image.fileUrl = mApiEndpoint + value;
            else if (name.equals("width")) // Image width
              image.width = Integer.parseInt(value);
            else if (name.equals("height")) // Image height
              image.height = Integer.parseInt(value);

            else if (name.equals("preview_url")) // Thumbnail URL
              image.previewUrl = mApiEndpoint + value;
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

            else if (name.equals("created_at"))
              image.createdAt = DATE_FORMAT.parse(value);
            else if (name.equals("has_comments")) // Has comments
              image.hasComments = value.equals("true");
          }

          // Shimmie2 doens't use sample files or parent IDs.
          image.sampleUrl = image.fileUrl;
          image.sampleHeight = image.height;
          image.sampleWidth = image.width;

          // No parent IDs.
          image.parentId = -1L;

          // Older versions of the api don't return thumbnail dimensions
          if (xpp.getAttributeValue(null, "preview_height") == null || xpp.getAttributeValue(null, "preview_width") == null) {
            image.previewHeight = 150;
            image.previewWidth = 150;
          }

          // No comment API.
          image.hasComments = false;

          // Append web URL.
          image.webUrl = String.format(Locale.US, "%s/post/view/%d", mApiEndpoint, image.id);
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

}
