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
import java.util.Date;
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
 * Legacy Danbooru API client (Gelbooru/Konachan/Shimmie2)
 */
public class DanbooruLegacy implements BooruClient {
  public static enum ApiSubtype {
    SHIMMIE2,
    GELBOORU,
    DANBOORU
  }
  private static final class DateFormat {
    public static final SimpleDateFormat GELBOORU = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy", Locale.US);
    public static final SimpleDateFormat SHIMMIE2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
  }
  private class SearchResultRequest extends Request<SearchResult> {
    private Listener<SearchResult> mListener;

    public SearchResultRequest(String url, Listener<SearchResult> listener, ErrorListener errorListener) {
      super(Method.GET, url, errorListener);
      mListener = listener;
      setRequestQueue(mRequestQueue);
      
    }

    @Override
    protected void deliverResponse(SearchResult response) {
      mListener.onResponse(response);
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

  }
  private final RequestQueue mRequestQueue;
  private final String mApiEndpoint;
  private final ApiSubtype mApiSubtype;
  private static final int DEFAULT_LIMIT = 100;
  private final String username;
  private final String password;
  
  public DanbooruLegacy(String endpoint, ApiSubtype subtype, RequestQueue requestQueue) {
    mApiEndpoint = endpoint;
    mApiSubtype = subtype;
    mRequestQueue = requestQueue;
    // No authentication needed.
    username = null;
    password = null;
  }

  public DanbooruLegacy(String endpoint, ApiSubtype subtype, RequestQueue requestQueue, String username, String password) {
    mApiEndpoint = endpoint;
    mApiSubtype = subtype;
    mRequestQueue = requestQueue;
    // Set credentials.
    this.username = username;
    this.password = password;
  }

  /*
   * {@inheritDoc}
   * @see pe.moe.nori.api.BooruClient#commentListRequest(long, com.android.volley.Response.Listener, com.android.volley.Response.ErrorListener)
   */
  @Override
  public Request<CommentList> commentListRequest(long postId, Listener<CommentList> listener, ErrorListener errorListener) {
    final String url;
    
    if (mApiSubtype == ApiSubtype.DANBOORU) {
      url = String.format(Locale.US, mApiEndpoint + "/comment.xml?post_id=%d", postId);
    } else if (mApiSubtype == ApiSubtype.GELBOORU) {
      url = String.format(Locale.US, mApiEndpoint + "/index.php?page=dapi&s=comment&q=index&post_id=%d", postId);
    } else {
      // Shimmie doesn't implement this.
      return null;
    }
    
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

  private SearchResult parseSearchResultXML(String data) throws XmlPullParserException, IOException, ParseException {
    // Make sure data isn't null or empty.
    if (data == null || data.equals(""))
      return null;

    // Create new SearchResult.
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
            
            // Shimmie is dumb
            if (mApiSubtype == ApiSubtype.SHIMMIE2) {
              // HACK: Force querying for next page
              searchResult.count = DEFAULT_LIMIT * 2;
              searchResult.offset = 0;
            }
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
            }	

            else if (name.equals("score")) // Popularity score
              image.score = Integer.parseInt(value);
            else if (name.equals("source")) // Source URL
              image.source = value;
            else if (name.equals("md5")) // MD5 checksum
              image.md5 = value;

            else if (name.equals("created_at")) { // Creation date
              if (mApiSubtype == ApiSubtype.DANBOORU)
                image.createdAt = new Date(Integer.parseInt(value));
              else if (mApiSubtype == ApiSubtype.GELBOORU)
                image.createdAt = DateFormat.GELBOORU.parse(value);
              else if (mApiSubtype == ApiSubtype.SHIMMIE2)
                image.createdAt = DateFormat.SHIMMIE2.parse(value);
            }
            else if (name.equals("has_comments")) // Has comments
              image.hasComments = value.equals("true");
          }
          
          // Shimmie is dumb.
          if (mApiSubtype == ApiSubtype.GELBOORU) {
            image.sampleUrl = image.fileUrl;
            image.sampleHeight = image.height;
            image.sampleWidth = image.width;
            
            // HACK: Could calculate correct aspect ratio, but this isn't used anyway.
            image.previewHeight = 150;
            image.previewWidth = 150;
            
            image.parentId = -1;
            // Shimmie doesn't have a comment API.
            image.hasComments = false;            
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
  
  /**
   * Checks if API requires authentication.
   */
  @Override
  public boolean requiresAuthentication() {
    return (username != null && password != null);
  }

  /*
   * {@inheritDoc}
   * @see pe.moe.nori.api.BooruClient#searchRequest(java.lang.String, int, com.android.volley.Response.Listener, com.android.volley.Response.ErrorListener)
   */
  @Override
  public Request<SearchResult> searchRequest(String tags, int pid, Listener<SearchResult> listener, ErrorListener errorListener) {
    final String url;
    
    if (mApiSubtype == ApiSubtype.DANBOORU) {
      url = String.format(Locale.US, mApiEndpoint + "/post.xml?tags=%s&pid=%d&limit=%d", Uri.encode(tags), pid+1, DEFAULT_LIMIT);
    } else if (mApiSubtype == ApiSubtype.GELBOORU) {
      url = String.format(Locale.US, mApiEndpoint + "/index.php?page=dapi&s=post&q=index&tags=%s&pid=%d&limit=%d", Uri.encode(tags), pid, DEFAULT_LIMIT);
    } else if (mApiSubtype == ApiSubtype.SHIMMIE2) {
      url = String.format(Locale.US, mApiEndpoint + "/api/danbooru/find_posts/index.xml?tags=%s&page=%d&limit=%d", Uri.encode(tags), pid+1, DEFAULT_LIMIT);
    } else {
      // Not implemented.
      return null;
    }
    
    return new SearchResultRequest(url, listener, errorListener);
  }

  /*
   * {@inheritDoc}
   * @see pe.moe.nori.api.BooruClient#searchRequest(java.lang.String, com.android.volley.Response.Listener, com.android.volley.Response.ErrorListener)
   */
  @Override
  public Request<SearchResult> searchRequest(String tags, Listener<SearchResult> listener, ErrorListener errorListener) {
    final String url;
    
    if (mApiSubtype == ApiSubtype.DANBOORU) {
      url = String.format(Locale.US, mApiEndpoint + "/post.xml?tags=%s&limit=%d", Uri.encode(tags), DEFAULT_LIMIT);
    } else if (mApiSubtype == ApiSubtype.GELBOORU) {
      url = String.format(Locale.US, mApiEndpoint + "/index.php?page=dapi&s=post&q=index&tags=%s&limit=%d", Uri.encode(tags), DEFAULT_LIMIT);
    } else if (mApiSubtype == ApiSubtype.SHIMMIE2) {
      url = String.format(Locale.US, mApiEndpoint + "/api/danbooru/find_posts/index.xml?tags=%s&limit=%d", Uri.encode(tags), DEFAULT_LIMIT);
    } else {
      // Not implemented.
      return null;
    }
    
    return new SearchResultRequest(url, listener, errorListener);
  }

}
