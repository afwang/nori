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

  /** Default constructor */
  public SearchResult() {
  }

  /**
   * Constructor used for deserializing from {@link Parcel}
   * @param in {@link Parcel} to read values from
   */
  protected SearchResult(Parcel in) {
    // Read values from parcel.
    in.readList(images, null);
    count = in.readLong();
    offset = in.readLong();
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
    dest.writeLong(offset);
  }

}
