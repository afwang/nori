/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

import java.util.Arrays;
import java.util.List;

/**
 * Search result received from the API.
 */
public class SearchResult implements Parcelable {
  // Parcelables are the standard Android serialization API used to retain data between sessions.
  /** Class loader used when deserializing from a {@link Parcel}. */
  public static final Parcelable.Creator<SearchResult> CREATOR = new Parcelable.Creator<SearchResult>() {
    @Override
    public SearchResult createFromParcel(Parcel source) {
      return new SearchResult(source);
    }

    @Override
    public SearchResult[] newArray(int size) {
      return new SearchResult[size];
    }
  };

  /** List of {@link Image}s included in this SearchResult. */
  private final List<Image> images;

  /** Total number of images in this SearchResult, including pages that have not been fetched yet. */
  private int count;
  /** Current offset. Used for paging. */
  private int offset = 0;

  /** List of tags originally used to retrieve this SearchResult. */
  private final Tag[] query;
  /**
   * True if more results may be available on the next page.
   * Set to false when the last page of results has been retrieved and included in {@link #images}.
   */
  private boolean hasNextPage = true;

  /**
   * Create a new SearchResult from a list of {@link com.cuddlesoft.nori.api.Image}s.
   *
   * @param images List of images included in this SearchResult.
   * @param query  Tags used to retrieve this query.
   * @param count  Total number of results in this SearchResult, including any pages that have not been fetched yet.
   * @param offset Current paging offset. (ie. page number)
   */
  public SearchResult(Image[] images, Tag[] query, int count, int offset) {
    this.images = Arrays.asList(images);
    this.query = query.clone();
    this.count = count;
    this.offset = offset;
  }

  /**
   * Re-create a SearchResult by deserializing data from a {@link android.os.Parcel}.
   *
   * @param parcel {@link android.os.Parcel} used to deserialize the SearchResult.
   */
  protected SearchResult(Parcel parcel) {
    this.images = parcel.createTypedArrayList(Image.CREATOR);
    this.count = parcel.readInt();
    this.offset = parcel.readInt();
    this.query = parcel.createTypedArray(Tag.CREATOR);
    this.hasNextPage = (parcel.readByte() == 0x01);
  }

  /**
   * Remove images with the given set of {@link com.cuddlesoft.nori.api.Tag}s from this SearchResult.
   *
   * @param tags Tags to remove.
   */
  public void filter(final Tag[] tags) {
    // Don't waste time filtering against an empty array.
    if (tags == null || tags.length == 0) {
      return;
    }

    // Convert filtered tag array to List.
    final List<Tag> tagList = Arrays.asList(tags);
    // Don't filter tags searched for by the user.
    CollectionUtils.removeAll(tagList, Arrays.asList(query));

    // Remove images containing filtered tags.
    CollectionUtils.filter(images, new Predicate<Image>() {
      @Override
      public boolean evaluate(Image image) {
        return !CollectionUtils.containsAny(Arrays.asList(image.tags), tagList);
      }
    });
  }

  /**
   * Remove images with the given set of {@link com.cuddlesoft.nori.api.Image.ObscenityRating} from this SearchResult.
   *
   * @param obscenityRatings Obscenity ratings to remove.
   */
  public void filter(final Image.ObscenityRating[] obscenityRatings) {
    // Don't waste time filtering against an empty array.
    if (obscenityRatings == null || obscenityRatings.length == 0) {
      return;
    }

    // Concert filtered rating array to List
    final List<Image.ObscenityRating> ratingList = Arrays.asList(obscenityRatings);
    // Remove images containing filtered ratings.
    CollectionUtils.filter(images, new Predicate<Image>() {
      @Override
      public boolean evaluate(Image image) {
        return !ratingList.contains(image.obscenityRating);
      }
    });
  }

  /**
   * Add more images to this SearchResult.
   * Usually called when new page of results has been fetched from the API.
   * Don't forget to call {@link #filter(Tag[])} and {@link #filter(com.cuddlesoft.nori.api.Image.ObscenityRating[])}
   * after adding more images.
   *
   * @param images Images to add.
   * @param offset Current paging offset. (ie. page number)
   */
  public void addImages(Image[] images, int offset) {
    // Add images to list.
    this.images.addAll(Arrays.asList(images));
    // Set new offset.
    this.offset = offset;
  }

  /**
   * Get {@link com.cuddlesoft.nori.api.Image}s contained in this SearchResult.
   *
   * @return {@link com.cuddlesoft.nori.api.Image}s returned by this SearchResult.
   */
  public Image[] getImages() {
    return images.toArray(new Image[images.size()]);
  }

  /**
   * Return total number of images in this SearchResult, including result pages that have not been fetched yet.
   * Note that some APIs don't return total result counts with their search results.
   * Don't expect this value to be accurate, unless you know that the API you're using returns accurate result counts.
   *
   * @return Total number of images in this SearchResult.
   */
  public long getResultCount() {
    return count;
  }

  /**
   * Get the current paging offset.
   * The way this value works varies greatly between APIs.
   * Some APIs use page numbers, some APIs use mySQL-style index offsets.
   * Don't rely on this value and don't show it to the user, unless you know what you're doing and
   * account for differences in the APIs.
   *
   * @return Current paging offset.
   */
  public long getCurrentOffset() {
    return offset;
  }

  /**
   * Get array of {@link com.cuddlesoft.nori.api.Tag}s used to retrieve this SearchResult.
   *
   * @return {@link com.cuddlesoft.nori.api.Tag}s used to retrieve this SearchResult.
   */
  public Tag[] getQuery() {
    return query;
  }

  /**
   * True if this SearchResult may contain another page that has not been retrieved yet.
   * Useful when implementing endless scrolling.
   *
   * @return True if another page of images could be fetched for this SearchResult.
   * @see #onLastPage()
   */
  public boolean hasNextPage() {
    return hasNextPage;
  }

  /**
   * Marks this SearchResult as having reached its final page.
   * This should be called by the API clients:
   * - Immediately for empty results.
   * - When fetching the next page returns an empty result.
   * Don't rely on length of array returned by {@link #getImages()}, as its value can be affected
   * by {@link #filter(Tag[])} and {@link #filter(com.cuddlesoft.nori.api.Image.ObscenityRating[])}.
   *
   * @see #hasNextPage()
   */
  public void onLastPage() {
    hasNextPage = false;
  }


  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeTypedList(images);
    dest.writeInt(count);
    dest.writeInt(offset);
    dest.writeTypedArray(query, 0);
    dest.writeByte((byte) (hasNextPage ? 0x01 : 0x00));
  }
}
