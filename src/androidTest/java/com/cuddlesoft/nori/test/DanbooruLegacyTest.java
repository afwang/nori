package com.cuddlesoft.nori.test;

import com.cuddlesoft.nori.api.clients.DanbooruLegacy;
import com.cuddlesoft.nori.api.clients.SearchClient;

/**
 * Tests for the Danbooru 1.x API client.
 */
public class DanbooruLegacyTest extends SearchClientTestCase {
  // TODO: Test Basic Auth authentication.

  @Override
  protected SearchClient createSearchClient() {
    return new DanbooruLegacy("http://danbooru.donmai.us");
  }
}