/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.api.clients;

import com.cuddlesoft.nori.api.SearchResult;

import java.io.IOException;

/**
 * Interface for a client consuming a Danbooru style API.
 */
public interface SearchClient {

  /**
   * Fetch first page of results containing images with the given set of tags.
   *
   * @param tags Search query. A space-separated list of tags.
   * @return A {@link com.cuddlesoft.nori.api.SearchResult} containing a set of Images.
   * @throws IOException Network error.
   */
  public SearchResult search(String tags) throws IOException;

  /**
   * Search for images with the given set of tags.
   *
   * @param tags Search query. A space-separated list of tags.
   * @param pid  Page number. (zero-indexed)
   * @return A {@link com.cuddlesoft.nori.api.SearchResult} containing a set of Images.
   * @throws java.io.IOException Network error.
   */
  public SearchResult search(String tags, int pid) throws IOException;

  /**
   * Get a SFW default query to search for when an app is launched.
   *
   * @return Safe-for-work query to search for when an app is launched.
   */
  public String getDefaultQuery();

  /**
   * Check if the API server requires or supports optional authentication.
   * <p/>
   * This is used in the API server settings activity as follows:
   * If REQUIRED, the user will need to supply valid credentials.
   * If OPTIONAL, the credential form is shown, but can be left empty.
   * If NONE, the credential form will not be shown.
   *
   * @return {@link com.cuddlesoft.nori.api.clients.SearchClient.AuthenticationType} value for this API backend.
   */
  public abstract AuthenticationType requiresAuthentication();

  /** API authentication types */
  public enum AuthenticationType {
    REQUIRED,
    OPTIONAL,
    NONE
  }

}
