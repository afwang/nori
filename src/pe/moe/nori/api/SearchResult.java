/**
 * This file is part of Nori.
 * Copyright (c) 2013 Obscure Reference
 * License: GPLv3
 */
package pe.moe.nori.api;

import java.util.ArrayList;

public class SearchResult {
  /** List of images */
  public ArrayList<Image> images = new ArrayList<Image>();
  /** Image count, across all pages */
  public long count;
  /** Offset, used for paging */
  public long offset;
}
