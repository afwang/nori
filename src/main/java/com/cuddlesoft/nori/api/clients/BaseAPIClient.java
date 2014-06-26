/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.api.clients;

import com.cuddlesoft.nori.api.SearchResult;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract class for a client consuming a Danbooru style API.
 */
public abstract class BaseAPIClient {
  /**
   * By default, clients will fetch metadata for this many images per page.
   * Best to fetch a large number to reduce the number of unique HTTP requests.
   */
  protected static final int DEFAULT_LIMIT = 100;
  /** Regular expression used for extracting Pixiv IDs from source URLs. */
  private static final Pattern PIXIV_ID_FROM_URL_PATTERN = Pattern.compile("http://(?:www|i\\d)\\.pixiv\\.net/.+?(?:illust_id=|img/.+?/)(\\d+)");

  /**
   * Extract Pixiv ID from a source URL pointing to the image's Pixiv page.
   *
   * @param url Pixiv URL.
   * @return ID extracted from the URL or null if no Pixiv ID was found.
   */
  protected String getPixivIDFromUrl(String url) {
    // Make sure the URL isn't empty or null.
    if (url == null || url.isEmpty()) {
      return null;
    }

    // Match regular expression against URL.
    Matcher matcher = PIXIV_ID_FROM_URL_PATTERN.matcher(url);
    if (matcher.find()) {
      return matcher.group(1);
    }

    // No ID matched.
    return null;
  }

  /**
   * Get default query for this API.
   * Use "rating:safe" to only show SFW when the app is launched.
   * Ignored if user sets a custom default query in the settings.
   *
   * @return Default query string for this API.
   */
  public String getDefaultQuery() {
    return "rating:safe";
  }

  /**
   * Fetch first page of results containing images with the given set of tags.
   *
   * @param tags Search query. A space-separated list of tags.
   * @return A {@link com.cuddlesoft.nori.api.SearchResult} containing a set of Images.
   * @throws IOException Network error.
   */
  public abstract SearchResult search(String tags) throws IOException;

  /**
   * Search for images with the given set of tags.
   *
   * @param tags Search query. A space-separated list of tags.
   * @param pid  Page number. (zero-indexed)
   * @return A {@link com.cuddlesoft.nori.api.SearchResult} containing a set of Images.
   * @throws java.io.IOException Network error.
   */
  public abstract SearchResult search(String tags, int pid) throws IOException;

  /**
   * Check if the API server requires or supports optional authentication.
   * <p/>
   * This is used in the API server settings activity as follows:
   * If REQUIRED, the user will need to supply valid credentials.
   * If OPTIONAL, the credential form is shown, but can be left empty.
   * If NONE, the credential form will not be shown.
   *
   * @return {@link com.cuddlesoft.nori.api.clients.BaseAPIClient.AuthenticationType} value for this API backend.
   */
  public abstract AuthenticationType requiresAuthentication();

  /** API authentication types */
  public enum AuthenticationType {
    REQUIRED,
    OPTIONAL,
    NONE
  }

}
