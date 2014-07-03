/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.api.clients;

import android.net.Uri;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Client for the Gelbooru API.
 * The Gelbooru API is based on the Danbooru 1.x API with a few minor differences.
 */
public class Gelbooru extends DanbooruLegacy {
  /** Date format used by Gelbooru. */
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy", Locale.US);

  public Gelbooru(String endpoint) {
    super(endpoint);
  }

  public Gelbooru(String endpoint, String username, String password) {
    super(endpoint, username, password);
  }

  @Override
  protected String createSearchURL(String tags, int pid, int limit) {
    // Unlike DanbooruLegacy, page numbers are 0-indexed for Gelbooru APIs.
    return String.format(Locale.US, "%s/index.php?page=dapi&s=post&q=index&tags=%s&pid=%d&limit=%d", apiEndpoint, Uri.encode(tags), pid, limit);
  }

  @Override
  protected String webUrlFromId(String id) {
    return String.format(Locale.US, "%s/index.php?page=post&s=view&id=%s", apiEndpoint, id);
  }

  @Override
  protected Date dateFromString(String date) throws ParseException {
    // Override Danbooru 1.x date format.
    return DATE_FORMAT.parse(date);
  }
}
