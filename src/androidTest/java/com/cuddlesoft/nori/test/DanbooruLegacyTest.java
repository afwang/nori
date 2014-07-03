package com.cuddlesoft.nori.test;

import android.test.AndroidTestCase;

import com.cuddlesoft.nori.api.Image;
import com.cuddlesoft.nori.api.SearchResult;
import com.cuddlesoft.nori.api.clients.DanbooruLegacy;
import com.cuddlesoft.nori.api.clients.SearchClient;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Tests for the Danbooru 1.x API client.
 */
public class DanbooruLegacyTest extends AndroidTestCase {
  // TODO: Test Basic Auth authentication.

  public void testSearchUsingTags() throws Throwable {
    // TODO: Ideally this should be mocked, so testing doesn't rely on external APIs.
    // Create a new client connected to the Danbooru API.
    final SearchClient client = new DanbooruLegacy("http://danbooru.donmai.us");
    // Retrieve a search result.
    final SearchResult searchResult = client.search("yuri shared_scarf");

    // Make sure we got results back.
    assertThat(searchResult.getImages().length).isNotZero();
    // Check images.
    for (Image image : searchResult.getImages()) {
      ImageTests.verifyImage(image);
    }

    // Check rest of the values.
    assertThat(searchResult.getResultCount()).isPositive();
    assertThat(searchResult.getCurrentOffset()).isEqualTo(0L);
    assertThat(searchResult.getQuery()).hasSize(2);
    assertThat(searchResult.getQuery()[0].getName()).isEqualTo("yuri");
    assertThat(searchResult.getQuery()[1].getName()).isEqualTo("shared_scarf");
    assertThat(searchResult.hasNextPage()).isTrue();
  }

  public void testSearchUsingTagsAndOffset() throws Throwable {
    // TODO: Ideally this should be mocked, so testing doesn't rely on external APIs.
    // Create a new client connected to the Danbooru API.
    final SearchClient client = new DanbooruLegacy("http://danbooru.donmai.us");
    // Retrieve search results.
    final SearchResult page1 = client.search("yuri shared_scarf", 0);
    final SearchResult page2 = client.search("yuri shared_scarf", 1);

    // Make sure that the results differ.
    assertThat(page1.getImages()).isNotEmpty();
    assertThat(page2.getImages()).isNotEmpty();
    assertThat(page1.getImages()[0].id).isNotEqualTo(page2.getImages()[0].id);
  }
}