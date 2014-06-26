/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.api.clients;

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

}
