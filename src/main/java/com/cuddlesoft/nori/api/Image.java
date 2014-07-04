/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.api;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadata received from the API for each image.
 */
public class Image implements Parcelable {
  // Parcelables are the standard Android serialization API used to retain data between sessions.
  /** Class loader used when deserializing from a {@link Parcel}. */
  public static final Parcelable.Creator<Image> CREATOR = new Parcelable.Creator<Image>() {

    @Override
    public Image createFromParcel(Parcel source) {
      // Use the Parcel constructor.
      return new Image(source);
    }

    @Override
    public Image[] newArray(int size) {
      return new Image[size];
    }
  };

  /** Regular expression for matching Pixiv image ID from Pixiv URLs */
  private static final Pattern PIXIV_ID_FROM_URL_PATTERN = Pattern.compile("http://(?:www|i\\d)\\.pixiv\\.net/.+?(?:illust_id=|img/.+?/)(\\d+)");

  /** Default constructor */
  public Image() {
  }

  /**
   * Create a new Image by deserializing it from a {@link android.os.Parcel}.
   *
   * @param in {@link android.os.Parcel} used to deserialize the image.
   */
  protected Image(Parcel in) {
    // Deserialize values from Parcel.
    fileUrl = in.readString();
    width = in.readInt();
    height = in.readInt();
    previewUrl = in.readString();
    previewWidth = in.readInt();
    previewHeight = in.readInt();
    sampleUrl = in.readString();
    sampleWidth = in.readInt();
    sampleHeight = in.readInt();
    tags = in.createTypedArray(Tag.CREATOR);
    id = in.readString();
    parentId = in.readString();
    webUrl = in.readString();
    pixivId = in.readString();
    obscenityRating = ObscenityRating.values()[in.readInt()];
    score = in.readInt();
    source = in.readString();
    md5 = in.readString();
    final long tmpCreatedAt = in.readLong();
    createdAt = (tmpCreatedAt != -1) ? new Date(tmpCreatedAt) : null;
  }

  /** Full-resolution image URL. */
  public String fileUrl;
  /** Image width. */
  public int width;
  /** Image height. */
  public int height;

  /** Thumbnail URL. */
  public String previewUrl;
  /** Thumbnail width. */
  public int previewWidth = 0;
  /** Thumbnail height */
  public int previewHeight = 0;

  // Samples are medium-resolution images downsized for viewing on the web.
  // Usually no more than ~1000px width.
  // Suitable for slow networks and low resolution devices (mdpi or less).
  /** Sample URL. */
  public String sampleUrl;
  /** Sample width. */
  public int sampleWidth = 0;
  /** Sample height. */
  public int sampleHeight = 0;

  /** Image tags. */
  public Tag[] tags;

  /** Image ID */
  public String id;
  /** Image parent ID. Used when there are multiple similar images. */
  public String parentId;
  /** Parent ID */
  public String pixivId;
  /** Web URL. */
  public String webUrl;
  /** Source URL. */
  public String source;
  /** MD5 hash */
  public String md5;

  /** SFW rating. */
  public ObscenityRating obscenityRating;
  /** Popularity score. */
  public Integer score;
  /** Upload date. */
  public Date createdAt;

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    // Serialize data into a Parcel.
    dest.writeString(fileUrl); //
    dest.writeInt(width); //
    dest.writeInt(height); //
    dest.writeString(previewUrl); //
    dest.writeInt(previewWidth); //
    dest.writeInt(previewHeight); //
    dest.writeString(sampleUrl); //
    dest.writeInt(sampleWidth); //
    dest.writeInt(sampleHeight); //
    dest.writeTypedArray(tags, 0); //
    dest.writeString(id); //
    dest.writeString(parentId); //
    dest.writeString(webUrl); //
    dest.writeString(pixivId); //
    dest.writeInt(obscenityRating.ordinal());
    dest.writeInt(score);
    dest.writeString(source);
    dest.writeString(md5);
    dest.writeLong(createdAt != null ? createdAt.getTime() : -1L);
  }

  /**
   * Extract a Pixiv ID from URL to an image's Pixiv page.
   *
   * @param url Pixiv URL.
   * @return Pixiv ID. Null if an ID could not be matched.
   */
  public static String getPixivIdFromUrl(String url) {
    // Make sure the URL isn't empty or null.
    if (url == null || url.isEmpty()) {
      return null;
    }

    // Match regular expression against URL.
    Matcher matcher = PIXIV_ID_FROM_URL_PATTERN.matcher(url);
    if (matcher.find()) {
      return matcher.group(1);
    }

    // No ID matched.
    return null;
  }

  /**
   * Safe-for-work ratings.
   * Users can choose to hide images with certain SFW ratings.
   */
  public enum ObscenityRating {
    /** Image is safe for work. */
    SAFE,
    /** Image is generally safe, but may contain some sexually-suggestive content */
    QUESTIONABLE,
    /** Image is explicit and not safe for work. */
    EXPLICIT,
    /** Rating is unknown or has not been set. */
    UNDEFINED;

    /**
     * Get a ObscenityRating from a raw String representation returned by the API.
     *
     * @param s String returned by the API.
     * @return ObscenityRating for given value.
     */
    public static ObscenityRating fromString(String s) {
      // Convert string to lower-case and look at first character only.
      switch (s.toLowerCase().charAt(0)) {
        case 's':
          return ObscenityRating.SAFE;
        case 'q':
          return ObscenityRating.QUESTIONABLE;
        case 'e':
          return ObscenityRating.EXPLICIT;
        default:
          return ObscenityRating.UNDEFINED;
      }
    }
  }

}
