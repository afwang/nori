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
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import pe.moe.nori.api.Image.ObscenityRating;

import android.net.Uri;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

/**
 * Client for the Danbooru 2.x API
 */
public class Danbooru implements BooruClient {
  private class SearchResultRequest extends Request<SearchResult> {
    // Response listener.
    private Listener<SearchResult> mListener;
    
    /**
     * Creates a new GET request
     * 
     * @param url URL to fetch the Search XML at
     * @param listener Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public SearchResultRequest(String url, Listener<SearchResult> listener, ErrorListener errorListener) {
      super(Method.GET, url, errorListener);
      // Set response listener.
      mListener = listener;
      // Set request queue.
      setRequestQueue(mRequestQueue);
    }
    
    @Override
    protected void deliverResponse(SearchResult response) {
      // Pass response to listener.
      mListener.onResponse(response); 
    }
    
    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
      // Set authentication headers.
      if (mUsername != null && mApiKey != null) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", Base64.encodeToString((mUsername+":"+mApiKey).getBytes(), Base64.DEFAULT));
        return headers;
      } else {
        return Collections.emptyMap();
      }
    }

    @Override
    protected Response<SearchResult> parseNetworkResponse(NetworkResponse response) {
      try {
        return Response.success(parseSearchResultXML(new String(response.data, HttpHeaderParser.parseCharset(response.headers))),
            HttpHeaderParser.parseCacheHeaders(response));
      } catch (Exception e) {
        return Response.error(new VolleyError("Error processing data."));
      }
    }
    
  }
  private static final String DEFAULT_API_ENDPOINT = "http://danbooru.donmai.us";
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
  private static final int DEFAULT_LIMIT = 100;
  private final String mApiEndpoint;
  private final String mUsername;
  private final String mApiKey;
  
  private final RequestQueue mRequestQueue;
  
  public Danbooru(RequestQueue requestQueue, String username, String apiKey) {
    // Use default API endpoint (Danbooru).
    mApiEndpoint = DEFAULT_API_ENDPOINT;
    // Set request queue.
    mRequestQueue = requestQueue;
    // Username and API key
    mUsername = username;
    mApiKey = apiKey;
  }
  
  @Override
  public Request<CommentList> commentListRequest(long postId, Listener<CommentList> listener, ErrorListener errorListener) {
    // TODO implement me
    return null;    
  }

  @Override
  public String getDefaultQuery() {
    return "rating:safe";
  }
  
  /**
   * Parses an HTTP response body into a {@link SearchResult}
   * 
   * @param data HTTP response body containing a valid XML document
   * @return Returns a {@link SearchResult} containing data found in the XML document, null if HTTP response was empty.
   * @throws XmlPullParserException
   * @throws IOException
   * @throws ParseException
   */
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
    
    Image image = null;
    
    // Loop over XML elements.
    while (eventType != XmlPullParser.END_DOCUMENT) {
      
      
      if (eventType == XmlPullParser.START_TAG) {
        // Get tag name.
        final String name = xpp.getName();
        
        
        if (xpp.getName().equals("post")) { // Create new image
          image = new Image();
        }
        
        else if (name.equals("large-file-url")) // Image URL
          image.fileUrl = mApiEndpoint + xpp.nextText();
        else if (name.equals("image-width")) // Image width
          image.width = Integer.parseInt(xpp.nextText());
        else if (name.equals("image-height")) // Image height
          image.height = Integer.parseInt(xpp.nextText());
        
        else if (name.equals("preview-file-url")) { // Thumbnail URL
          image.previewUrl = mApiEndpoint + xpp.nextText();
          // FIXME: API doesn't return thumbnail dimensions.
          image.previewWidth = 150;
          image.previewHeight = 150;
        }
        
        else if (name.equals("file-url")) { // Sample URL
          image.sampleUrl = mApiEndpoint + xpp.nextText();
          // FIXME: API doesn't return sample dimensions.
          image.sampleWidth = 850;
          image.sampleHeight = 850;
        }
        
        else if (name.equals("tag-string-general")) // General tags
          image.generalTags = xpp.nextText().trim().split(" ");
        else if (name.equals("tag-string-artist")) // Artist tags
          image.artistTags = xpp.nextText().trim().split(" ");
        else if (name.equals("tag-string-character")) // Character tags
          image.characterTags = xpp.nextText().trim().split(" ");
        else if (name.equals("tag-string-copyright")) // Copyright tags
          image.copyrightTags = xpp.nextText().trim().split(" ");
        
        else if (name.equals("id")) // Image ID
          image.id = Long.parseLong(xpp.nextText());
        else if (name.equals("parent-id")) // Parent ID
          image.parentId = xpp.getAttributeValue(null, "nil") != null ? -1 : Long.parseLong(xpp.nextText());
        else if (name.equals("pixiv-id")) // Pixiv ID
          image.pixivId = xpp.getAttributeValue(null, "nil") != null ? -1 : Long.parseLong(xpp.nextText());
        
        else if (name.equals("rating")) { // Obscenity rating
          final String rating = xpp.nextText();
          
          if (rating.equals("s")) // Safe for work
            image.obscenityRating = ObscenityRating.SAFE;
          else if (rating.equals("q")) // Ambiguous
            image.obscenityRating = ObscenityRating.QUESTIONABLE;
          else if (rating.equals("e")) // Explicit
            image.obscenityRating = ObscenityRating.EXPLICIT;
          else // Unknown / Undefined
            image.obscenityRating = ObscenityRating.UNDEFINED;
        }
        
        else if (name.equals("score")) // Popularity score
          image.score = Integer.parseInt(xpp.nextText());
        else if (name.equals("source")) // Source URL
          image.source = xpp.nextText();
        else if (name.equals("md5")) // MD5 checksum
          image.md5 = xpp.nextText();
        else if (name.equals("last-commented-at")) // Has comment
          image.hasComments = !xpp.nextText().equals("");
        else if (name.equals("created-at")) // Creation date
          image.createdAt = DATE_FORMAT.parse(xpp.nextText());
        
      } else if (eventType == XmlPullParser.END_TAG) {
        if (xpp.getName().equals("post")) // Add image to results
          searchResult.images.add(image);
          
      }
      // Get next XML element.
      eventType = xpp.next();
    }
    
    return searchResult;
  }
  
  @Override
  public boolean requiresAuthentication() {
    return true;
  }
  
  @Override
  public Request<SearchResult> searchRequest(String tags, int pid, Listener<SearchResult> listener, ErrorListener errorListener) {
    final String url = String.format(mApiEndpoint + "/posts.xml?tags=%s&page=%d&limit=%d", Uri.encode(tags), pid+1, DEFAULT_LIMIT);
    return new SearchResultRequest(url, listener, errorListener);
  }

  @Override
  public Request<SearchResult> searchRequest(String tags, Listener<SearchResult> listener, ErrorListener errorListener) {
    final String url = String.format(mApiEndpoint + "/posts.xml?tags=%s&limit=%d", Uri.encode(tags), DEFAULT_LIMIT);
    return new SearchResultRequest(url, listener, errorListener);
  }

}
