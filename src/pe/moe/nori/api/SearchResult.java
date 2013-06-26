/**
 * This file is part of Nori.
 * Copyright (c) 2013 Obscure Reference
 * License: GPLv3
 */
package pe.moe.nori.api;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class SearchResult implements Parcelable {
  /** Class loader used when deserializing from {@link Parcel}. */
  public static final Parcelable.Creator<SearchResult> CREATOR = new Parcelable.Creator<SearchResult>() {
    public SearchResult createFromParcel(Parcel in) {
      // Use the parcel constructor.
      return new SearchResult(in);
    }

    @Override
    public SearchResult[] newArray(int size) {
      // Return new array.
      return new SearchResult[size];
    }
  };
  /** List of images */
  public ArrayList<Image> images = new ArrayList<Image>();
  /** Image count, across all pages */
  public long count;
  /** Offset, used for paging */
  public long offset;
  /** Current page number */
  public int pageNumber = 0;
  /** Query used to get this SearchResult */
  public String query;
  /** True if more results are available on the next page */
  private boolean hasMore = true;

  /** Default constructor */
  public SearchResult() {
  }

  /**
   * Extends the result with images from another page.
   *
   * @param result Results from another page.
   */
  public void extend(SearchResult result) {
    if (result.images.size() > 0) {
      // Merge array lists.
      images.addAll(result.images);
      // Set offset and increment page number.
      offset = result.offset;
      pageNumber++;
    } else {
      // Don't bother fetching next page.
      hasMore = false;
    }
  }

  /**
   * Check if more results could be available on the next page.
   *
   * @return True if last call to {@link #extend(SearchResult)} added images to the result.
   */
  public boolean hasMore() {
    return hasMore;
  }

  /**
   * Constructor used for deserializing from {@link Parcel}
   * @param in {@link Parcel} to read values from
   */
  protected SearchResult(Parcel in) {
    // Read values from parcel.
    in.readList(images, Image.class.getClassLoader());
    count = in.readLong();
    pageNumber = in.readInt();
    offset = in.readLong();
    query = in.readString();
    hasMore = in.readByte() == 0x00;
  }

  @Override
  public int describeContents() {
    // Not used.
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    // Write values to parcel.
    dest.writeList(images);
    dest.writeLong(count);
    dest.writeInt(pageNumber);
    dest.writeLong(offset);
    dest.writeString(query);
    dest.writeByte((byte) (hasMore ? 0x01 : 0x00));
  }

}
