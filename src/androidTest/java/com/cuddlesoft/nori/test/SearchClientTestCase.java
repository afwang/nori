/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.test;


import android.test.AndroidTestCase;

import com.cuddlesoft.nori.api.Image;
import com.cuddlesoft.nori.api.SearchResult;
import com.cuddlesoft.nori.api.clients.SearchClient;

import static org.fest.assertions.api.Assertions.assertThat;

/** Extend this class to test a class implementing the {@link com.cuddlesoft.nori.api.clients.SearchClient} API. */
public abstract class SearchClientTestCase extends AndroidTestCase {

  public void testSearchUsingTags() throws Throwable {
    // TODO: Ideally this should be mocked, so testing doesn't rely on external APIs.
    // Create a new client connected to the Danbooru API.
    final SearchClient client = createSearchClient();
    // Retrieve a search result.
    final SearchResult result = client.search("tagme");

    // Make sure we got results back.
    assertThat(result.getImages()).isNotEmpty();
    // Verify metadata for each returned image.
    for (Image image : result.getImages()) {
      ImageTests.verifyImage(image);
    }

    // Check rests of the values.
    assertThat(result.getCurrentOffset()).isEqualTo(0L);
    assertThat(result.getQuery()).hasSize(1);
    assertThat(result.getQuery()[0].getName()).isEqualTo("tagme");
    assertThat(result.hasNextPage()).isTrue();
  }

  public void testSearchUsingTagsAndOffset() throws Throwable {
    // TODO: Ideally this should be mocked, so testing doesn't rely on external APIs.
    // Create a new client connected to the Danbooru API.
    final SearchClient client = createSearchClient();
    // Retrieve search results.
    final SearchResult page1 = client.search("tagme", 0);
    final SearchResult page2 = client.search("tagme", 1);

    // Make sure that the results differ.
    assertThat(page1.getImages()).isNotEmpty();
    assertThat(page2.getImages()).isNotEmpty();
    assertThat(page1.getImages()[0].id).isNotEqualTo(page2.getImages()[0].id);
  }

  public void testGetDefaultQuery() throws Throwable {
    final SearchClient client = createSearchClient();
    assertThat(client.getDefaultQuery()).isNotNull();
  }

  public void testRequiredAuthentication() throws Throwable {
    final SearchClient client = createSearchClient();
    assertThat(client.getDefaultQuery()).isNotNull();
  }

  protected abstract SearchClient createSearchClient();

}
