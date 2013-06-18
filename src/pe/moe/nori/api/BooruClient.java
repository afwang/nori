/**
 * This file is part of Nori.
 * Copyright (c) 2013 Obscure Reference
 * License: GPLv3
 */
package pe.moe.nori.api;

import com.android.volley.Request;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;

/**
 * Imageboard service client interface.
 */
public interface BooruClient {
  /**
   * Fetch a {@link CommentList} using Android Volley
   * @param postId Post ID
   * @param listener Listener to receive the {@link CommentList} response
   * @param errorListener Error listener, or null to ignore errors
   * @return Android Volley {@link Request}
   */
  public Request<CommentList> commentListRequest(long postId, Listener<CommentList> listener, ErrorListener errorListener);
  
  /**
   * Get default query to search for when application is first launched.
   * 
   * @return Default query. Should be something like "rating: safe"
   */
  public String getDefaultQuery();
  /**
   * Fetch a list of {@link Image}s using Android Volley.
   * 
   * @param tags Tags to search for. Any tag combination that works on the web should work here
   * @param pid Page number
   * @param listener Listener to receive the {@link SearchResult} response
   * @param errorListener Error listener, or null to ignore errors
   * @return Android Volley {@link Request}
   */
  public Request<SearchResult> searchRequest(String tags, int pid, Listener<SearchResult> listener, ErrorListener errorListener);

  /**
   * Fetch a list of {@link Image}s using Android Volley.
   * 
   * @param tags Tags to search for. Any tag combination that works on the web should work here
   * @param listener Listener to receive the {@link SearchResult} response
   * @param errorListener Error listener, or null to ignore errors
   * @return Android Volley {@link Request}
   */
  public Request<SearchResult> searchRequest(String tags, Listener<SearchResult> listener, ErrorListener errorListener);

}
