/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.api.clients;

import android.net.Uri;
import android.util.Base64;

import com.cuddlesoft.nori.api.Image;
import com.cuddlesoft.nori.api.SearchResult;
import com.cuddlesoft.nori.api.Tag;
import com.squareup.okhttp.Authenticator;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Client for the Danbooru 2.x API.
 */
public class Danbooru implements SearchClient {
  /**
   * Number of images per search results page.
   * Best to use a large value to minimize number of unique HTTP requests.
   */
  private static final int DEFAULT_LIMIT = 100;
  /** Thumbnail size set if not returned by the API. */
  private static final int THUMBNAIL_SIZE = 150;
  /** Sample size set if not returned by the API. */
  private static final int SAMPLE_SIZE = 850;
  /** Parser used to read the date format used by this API. */
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
  /** OkHTTP Client. */
  private final OkHttpClient okHttpClient = new OkHttpClient();
  /** URL to the HTTP API Endpoint - the server implementing the API. */
  private final String apiEndpoint;
  /** Username used for authentication. (optional) */
  private final String username;
  /** API key used for authentication. (optional) */
  private final String apiKey;

  /**
   * Create a new Danbooru 2.x client without authentication.
   *
   * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
   */
  public Danbooru(String endpoint) {
    this.apiEndpoint = endpoint;
    this.username = null;
    this.apiKey = null;
  }

  /**
   * Create a new Danbooru 1.x client with authentication.
   *
   * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
   * @param username Username used for authentication.
   * @param apiKey   API key used for authentication.
   */
  public Danbooru(String endpoint, String username, final String apiKey) {
    this.apiEndpoint = endpoint;
    this.username = username;
    this.apiKey = apiKey;

    // Enable HTTP Basic Authentication.
    okHttpClient.setAuthenticator(new Authenticator() {
      @Override
      public Request authenticate(Proxy proxy, Response response) throws IOException {
        final String credential = Base64.encodeToString(String.format("%s:%s", Danbooru.this.username, Danbooru.this.apiKey).getBytes(), Base64.DEFAULT);
        return response.request().newBuilder().header("Authorization", credential).build();
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
  @SuppressWarnings("FeatureEnvy")
  protected SearchResult parseXMLResponse(String body, String tags) throws IOException {
    // Create variables to hold the values as XML is being parsed.
    final List<Image> imageList = new ArrayList<>(DEFAULT_LIMIT);
    Image image = new Image();
    List<Tag> imageTags = new ArrayList<>();

    try {
      // Create an XML parser factory and disable namespace awareness for security reasons.
      // See: (http://lists.w3.org/Archives/Public/public-xmlsec/2009Dec/att-0000/sws5-jensen.pdf).
      final XmlPullParserFactory xmlParserFactory = XmlPullParserFactory.newInstance();
      xmlParserFactory.setNamespaceAware(false);

      // Create a new XML parser from factory and feed HTTP response data into it.
      final XmlPullParser xpp = xmlParserFactory.newPullParser();
      xpp.setInput(new StringReader(body));

      // Iterate over each XML element and handle pull parser "events".
      while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
        if (xpp.getEventType() == XmlPullParser.START_TAG) {
          // Get the tag's name.
          final String name = xpp.getName();

          if (name.equals("post")) {
            // Create a new image for each <post> tag.
            image = new Image();
            imageTags = new ArrayList<>();
          }
          // Extract image metadata from XML tags.
          else if (name.equals("large-file-url")) {
            image.fileUrl = apiEndpoint + xpp.nextText();
          } else if (name.equals("image-width")) {
            image.width = Integer.parseInt(xpp.nextText());
          } else if (name.equals("image-height")) {
            image.height = Integer.parseInt(xpp.nextText());
          } else if (name.equals("preview-file-url")) {
            image.previewUrl = apiEndpoint + xpp.nextText();
          } else if (name.equals("file-url")) {
            image.sampleUrl = apiEndpoint + xpp.nextText();
          } else if (name.equals("tag-string-general")) {
            imageTags.addAll(Arrays.asList(Tag.arrayFromString(xpp.nextText(), Tag.Type.GENERAL)));
          } else if (name.equals("tag-string-artist")) {
            imageTags.addAll(Arrays.asList(Tag.arrayFromString(xpp.nextText(), Tag.Type.ARTIST)));
          } else if (name.equals("tag-string-character")) {
            imageTags.addAll(Arrays.asList(Tag.arrayFromString(xpp.nextText(), Tag.Type.CHARACTER)));
          } else if (name.equals("tag-string-copyright")) {
            imageTags.addAll(Arrays.asList(Tag.arrayFromString(xpp.nextText(), Tag.Type.COPYRIGHT)));
          } else if (name.equals("id")) {
            image.id = xpp.nextText();
          } else if (name.equals("parent-id")) {
            image.parentId = xpp.getAttributeValue(null, "nil") != null ? null : xpp.nextText();
          } else if (name.equals("pixiv-id")) {
            image.pixivId = xpp.getAttributeValue(null, "nil") != null ? null : xpp.nextText();
          } else if (name.equals("rating")) {
            image.obscenityRating = Image.ObscenityRating.fromString(xpp.nextText());
          } else if (name.equals("score")) {
            image.score = Integer.parseInt(xpp.nextText());
          } else if (name.equals("source")) {
            image.source = xpp.nextText();
          } else if (name.equals("md5")) {
            image.md5 = xpp.nextText();
          } else if (name.equals("created-at")) {
            image.createdAt = DATE_FORMAT.parse(xpp.nextText());
          }
          // createdAt
        } else if (xpp.getEventType() == XmlPullParser.END_TAG) {
          if (xpp.getName().equals("post")) {
            // Convert tag list to array.
            image.tags = imageTags.toArray(new Tag[imageTags.size()]);
            // Append values not returned by API to image.
            image.webUrl = webUrlFromId(image.id);
            // FIXME: API does not return thumbnail sizes.
            image.previewWidth = THUMBNAIL_SIZE;
            image.previewHeight = THUMBNAIL_SIZE;
            // FIXME: API does not return sample sizes.
            image.sampleWidth = SAMPLE_SIZE;
            image.sampleHeight = SAMPLE_SIZE;
            // Add to result.
            imageList.add(image);
          }
        }
        xpp.next();
      }
    } catch (XmlPullParserException | ParseException e) {
      // Convert into IOException.
      // Needed for consistent method signatures in the SearchClient interface for different APIs.
      // (Throwing an XmlPullParserException would be fine, until dealing with an API using JSON, etc.)
      throw new IOException(e);
    }

    return new SearchResult(imageList.toArray(new Image[imageList.size()]), Tag.arrayFromString(tags));
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

    return String.format(Locale.US, apiEndpoint + "/posts.xml?tags=%s&page=%d&limit=%d", Uri.encode(tags), page, limit);
  }

  /**
   * Get a URL viewable in the system web browser for given Image ID.
   *
   * @param id {@link com.cuddlesoft.nori.api.Image} ID.
   * @return URL for viewing the image in the browser.
   */
  protected String webUrlFromId(String id) {
    return String.format(Locale.US, "%s/posts/%s", apiEndpoint, id);
  }

  @Override
  public String getDefaultQuery() {
    // Show work-safe images by default.
    return "rating:safe";
  }

  @Override
  public AuthenticationType requiresAuthentication() {
    return AuthenticationType.OPTIONAL;
  }
}
