/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.api.clients;

import android.net.Uri;

import com.cuddlesoft.nori.api.Image;
import com.cuddlesoft.nori.api.SearchResult;
import com.cuddlesoft.nori.api.Tag;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.Proxy;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Client for the Danbooru 1.x API.
 */
public class DanbooruLegacy implements SearchClient {
  /**
   * Number of images per search results page.
   * Best to use a large value to minimize number of unique HTTP requests.
   */
  private static final int DEFAULT_LIMIT = 100;
  /** Parser used to read the date format used by this API. */
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
  /** OkHTTP Client. */
  private final OkHttpClient okHttpClient = new OkHttpClient();
  /** URL to the HTTP API Endpoint - the server implementing the API. */
  protected final String apiEndpoint;
  /** Username used for authentication. (optional) */
  private final String username;
  /** Password used for authentication. (optional) */
  private final String password;

  /**
   * Create a new Danbooru 1.x client without authentication.
   *
   * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
   */
  public DanbooruLegacy(String endpoint) {
    this.apiEndpoint = endpoint;
    this.username = null;
    this.password = null;
  }

  /**
   * Create a new Danbooru 1.x client with authentication.
   *
   * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
   * @param username Username used for authentication.
   * @param password Password used for authentication.
   */
  public DanbooruLegacy(String endpoint, String username, String password) {
    this.apiEndpoint = endpoint;
    this.username = username;
    this.password = password;

    // Enable HTTP basic authentication.
    okHttpClient.setAuthenticator(new Authenticator() {
      @Override
      public Request authenticate(Proxy proxy, Response response) throws IOException {
        final String credential = Credentials.basic(DanbooruLegacy.this.username, DanbooruLegacy.this.password);
        return response.request().newBuilder()
            .header("Authorization", credential)
            .build();
      }

      @Override
      public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
        return null;
      }
    });
  }

  @Override
  public SearchResult search(String tags) throws IOException {
    // Return results for page 0.
    return search(tags, 0);
  }

  @Override
  public SearchResult search(String tags, int pid) throws IOException {
    // Create HTTP request.
    final Request request = new Request.Builder()
        .url(createSearchURL(tags, pid, DEFAULT_LIMIT))
        .build();
    // Get HTTP response.
    final Response response = okHttpClient.newCall(request).execute();
    final String body = response.body().string();

    // Return parsed SearchResult.
    return parseXMLResponse(body, tags);
  }

  /**
   * Parse an XML response returned by the API.
   *
   * @param body HTTP Response body.
   * @param tags Tags used to retrieve the response.
   * @return A {@link com.cuddlesoft.nori.api.SearchResult} parsed from given XML.
   */
  protected SearchResult parseXMLResponse(String body, String tags) throws IOException {
    // Create variables to hold the values as XML is being parsed.
    final List<Image> imageList = new ArrayList<>(DEFAULT_LIMIT);
    int count = 0;
    int offset = 0;

    try {
      // Create an XML parser factory and disable namespace awareness for security reasons.
      // See: (http://lists.w3.org/Archives/Public/public-xmlsec/2009Dec/att-0000/sws5-jensen.pdf).
      final XmlPullParserFactory xmlParserFactory = XmlPullParserFactory.newInstance();
      xmlParserFactory.setNamespaceAware(false);

      // Create a new XML parser and feed HTTP response data into it.
      final XmlPullParser xpp = xmlParserFactory.newPullParser();
      xpp.setInput(new StringReader(body));

      // Iterate over each XML element and handle pull parser "events".
      while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
        if (xpp.getEventType() == XmlPullParser.START_TAG) {
          if (xpp.getName().equals("post")) {
            // <post> tags contain metadata for each image.
            final Image image = new Image();

            // Extract image metadata from XML attributes.
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
              // Get name and value of current XML attribute.
              final String name = xpp.getAttributeName(i);
              final String value = xpp.getAttributeValue(i);

              // Set the appropriate value for each tag name.
              if (name.equals("file_url")) {
                image.fileUrl = apiEndpoint + value;
              } else if (name.equals("width")) {
                image.width = Integer.parseInt(value);
              } else if (name.equals("height")) {
                image.height = Integer.parseInt(value);
              } else if (name.equals("preview_url")) {
                image.previewUrl = apiEndpoint + value;
              } else if (name.equals("preview_width")) {
                image.previewWidth = Integer.valueOf(value);
              } else if (name.equals("preview_height")) {
                image.previewHeight = Integer.valueOf(value);
              } else if (name.equals("sample_url")) {
                image.sampleUrl = apiEndpoint + value;
              } else if (name.equals("sample_width")) {
                image.sampleWidth = Integer.valueOf(value);
              } else if (name.equals("sample_height")) {
                image.sampleHeight = Integer.valueOf(value);
              } else if (name.equals("tags")) {
                image.tags = Tag.arrayFromString(value, Tag.Type.GENERAL);
              } else if (name.equals("id")) {
                image.id = value;
              } else if (name.equals("parent_id")) {
                image.parentId = value;
              } else if (name.equals("rating")) {
                image.obscenityRating = Image.ObscenityRating.fromString(value);
              } else if (name.equals("score")) {
                image.score = Integer.parseInt(value);
              } else if (name.equals("md5")) {
                image.md5 = value;
              } else if (name.equals("created_at") || name.equals("date")) {
                image.createdAt = dateFromString(value);
              }
            }

            // Append values not returned by the API.
            image.webUrl = webUrlFromId(image.id);
            image.pixivId = Image.getPixivIDFromUrl(image.source);
            // Use original file if low-resolution sample does not exist.
            if (image.sampleUrl == null) {
              image.sampleUrl = image.fileUrl;
              image.sampleWidth = image.width;
              image.sampleHeight = image.height;
            }
            // Add Image to search result.
            imageList.add(image);
          } else if (xpp.getName().equals("posts")) {
            // <posts> is the root XML tag.
            // Its attributes contain useful metadata, such as total result count and current offset.
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
              if (xpp.getAttributeName(i).equals("count")) {
                count = Integer.parseInt(xpp.getAttributeValue(i));
              } else if (xpp.getAttributeName(i).equals("offset")) {
                offset = Integer.parseInt(xpp.getAttributeValue(i));
              }
            }
          }
        }
        // Get next XMLPullParser event.
        xpp.next();
      }
    } catch (XmlPullParserException | ParseException e) {
      // Convert into IOException.
      // Needed for consistent method signatures in the SearchClient interface for different APIs.
      // (Throwing an XmlPullParserException would be fine, until dealing with an API using JSON, etc.)
      throw new IOException(e);
    }
    // Set count if not returned by the API.
    count = (count == 0) ? imageList.size() : count;
    // Create and return a SearchResult.
    return new SearchResult(imageList.toArray(new Image[imageList.size()]), Tag.arrayFromString(tags), count, offset);
  }

  @Override
  public String getDefaultQuery() {
    // Show all safe-for-work images by default.
    return "rating:safe";
  }

  /**
   * Get a URL viewable in the system web browser for given Image ID.
   *
   * @param id {@link com.cuddlesoft.nori.api.Image} ID.
   * @return URL for viewing the image in the browser.
   */
  protected String webUrlFromId(String id) {
    return apiEndpoint + "/post/show/" + id;
  }

  /**
   * Create a {@link java.util.Date} object from String date representation used by this API.
   *
   * @param date Date string.
   * @return Date converted from given String.
   */
  protected Date dateFromString(String date) throws ParseException {
    return DATE_FORMAT.parse(date);
  }

  /**
   * Generate request URL to the search API endpoint.
   *
   * @param tags  Space-separated tags.
   * @param pid   Page number (0-indexed).
   * @param limit Images to fetch per page.
   * @return URL to search results API.
   */
  protected String createSearchURL(String tags, int pid, int limit) {
    // Page numbers are 1-indexed for this API.
    final int page = pid + 1;

    return String.format(Locale.US, apiEndpoint + "/post/index.xml?tags=%s&limit=%d&page=%d", Uri.encode(tags), limit, page);
  }

  @Override
  public AuthenticationType requiresAuthentication() {
    return AuthenticationType.OPTIONAL;
  }
}
