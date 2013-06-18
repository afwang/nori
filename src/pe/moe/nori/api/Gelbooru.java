/**
 * This file is part of Nori.
 * Copyright (c) 2013 Obscure Reference
 * License: GPLv3
 */
package pe.moe.nori.api;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

/**
 * Gelbooru (gelbooru.com) client.
 */
public class Gelbooru implements BooruClient {
  private final RequestQueue mRequestQueue;
  private static final String API_ENDPOINT = "http://gelbooru.com/index.php?page=dapi";
  private static final int DEFAULT_LIMIT = 100;
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy", Locale.US);


  public Gelbooru(RequestQueue requestQueue) {
    mRequestQueue = requestQueue;
  }

  /*
   * {@inheritDoc}
   * @see pe.moe.nori.api.BooruClient#commentListRequest(long, com.android.volley.Response.Listener, com.android.volley.Response.ErrorListener)
   */
  @Override
  public Request<CommentList> commentListRequest(long postId, Listener<CommentList> listener, ErrorListener errorListener) {
    // TODO: implement me
    return null;
  }

  /*
   * {@inheritDoc}
   * @see pe.moe.nori.api.BooruClient#getDefaultQuery()
   */
  @Override
  public String getDefaultQuery() {
    return "rating:safe";
  }

  /*
   * {@inheritDoc}
   * @see pe.moe.nori.api.BooruClient#searchRequest(java.lang.String, int, com.android.volley.Response.Listener, com.android.volley.Response.ErrorListener)
   */
  @Override
  public Request<SearchResult> searchRequest(String tags, int pid, Listener<SearchResult> listener, ErrorListener errorListener) {
    final String url = String.format(Locale.US, API_ENDPOINT + "&s=post&q=index&tags=%s&pid=%d&limit=%d", Uri.encode(tags), pid, DEFAULT_LIMIT);
    return new SearchResultRequest(url, listener, errorListener);
  }

  /*
   * {@inheritDoc}
   * @see pe.moe.nori.api.BooruClient#searchRequest(java.lang.String, com.android.volley.Response.Listener, com.android.volley.Response.ErrorListener)
   */
  @Override
  public Request<SearchResult> searchRequest(String tags, Listener<SearchResult> listener, ErrorListener errorListener) {
    final String url = String.format(Locale.US, API_ENDPOINT + "&s=post&q=index&tags=%s&limit=%d", Uri.encode(tags), DEFAULT_LIMIT);
    return new SearchResultRequest(url, listener, errorListener);
  }

  private SearchResult parseSearchResultXML(String data) throws XmlPullParserException, IOException, ParseException {
    // Make sure data isn't null.
    if (data == null)
      return null;

    // Create new searchResult.
    SearchResult searchResult = new SearchResult();

    // Create XML parser.
    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    factory.setNamespaceAware(true);
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
            }	

            else if (name.equals("score")) // Popularity score
              image.score = Integer.parseInt(value);
            else if (name.equals("source")) // Source URL
              image.source = value;
            else if (name.equals("md5")) // MD5 checksum
              image.md5 = value;

            else if (name.equals("created_at")) // Creation date
              image.createdAt = DATE_FORMAT.parse(value);
            else if (name.equals("has_comments")) // Has comments
              image.hasComments = value.equals("true");
          }
          // Add image to results.
          searchResult.images.add(image);
        }	
      }			
      // Get next XML element.
      eventType = xpp.next();
    }	
    return searchResult;
  }

  private class SearchResultRequest extends Request<SearchResult> {
    private Listener<SearchResult> mListener;

    public SearchResultRequest(String url, Listener<SearchResult> listener, ErrorListener errorListener) {
      super(Method.GET, url, errorListener);
      mListener = listener;
      setRequestQueue(mRequestQueue);
    }

    @Override
    protected Response<SearchResult> parseNetworkResponse(NetworkResponse response) {
      // Try parsing the XML or return error.
      try {
        return Response.success(parseSearchResultXML(new String(response.data, "UTF-8")), HttpHeaderParser.parseCacheHeaders(response));
      } catch (Exception e) {
        return Response.error(new VolleyError("Error processing data."));
      }
    }

    @Override
    protected void deliverResponse(SearchResult response) {
      mListener.onResponse(response);
    }

  }

}
