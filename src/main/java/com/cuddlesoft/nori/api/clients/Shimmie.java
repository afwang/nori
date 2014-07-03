/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.api.clients;

import java.util.Locale;

/**
 * Client for the Shimmie2 API.
 * Shimmie2 provides an extension that enables a Danbooru 1.x-based API.
 */
public class Shimmie extends DanbooruLegacy {
  public Shimmie(String endpoint) {
    super(endpoint);
  }

  public Shimmie(String endpoint, String username, String password) {
    super(endpoint, username, password);
  }

  @Override
  protected String webUrlFromId(String id) {
    return String.format(Locale.US, "%s/post/view/%s", apiEndpoint, id);
  }

  @Override
  protected String createSearchURL(String tags, int pid, int limit) {
    // Page numbers are 1-indexed for this api.
    final int page = pid + 1;

    return String.format(Locale.US, "%s/api/danbooru/find_posts/index.xml?tags=%s&page=%d&limit=%d", apiEndpoint, tags, page, limit);
  }
}
