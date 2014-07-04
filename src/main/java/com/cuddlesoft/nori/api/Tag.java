/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.api;

import android.os.Parcel;
import android.os.Parcelable;

/** Image tag */
public class Tag implements Comparable<Tag>, Parcelable {
  /** Class loader used when deserializing from a {@link Parcel}. */
  public static final Parcelable.Creator<Tag> CREATOR = new Parcelable.Creator<Tag>() {

    @Override
    public Tag createFromParcel(Parcel source) {
      // Use the Parcel constructor.
      return new Tag(source);
    }

    @Override
    public Tag[] newArray(int size) {
      return new Tag[size];
    }
  };

  /** Tag name */
  private final String name;
  /** Tag type */
  private final Type type;

  /**
   * Create a new {@link com.cuddlesoft.nori.api.Image} tag of the {@link Type#GENERAL} type.
   *
   * @param name Tag name.
   */
  public Tag(String name) {
    this.name = name;
    this.type = Type.GENERAL;
  }

  /**
   * Create a new {@link com.cuddlesoft.nori.api.Image} tag.
   *
   * @param name Tag name.
   * @param type Tag type.
   */
  public Tag(String name, Type type) {
    this.name = name;
    this.type = type;
  }

  /**
   * Re-create a serialize tag from {@link android.os.Parcel}.
   *
   * @param in Parcel containing a serialized {@link com.cuddlesoft.nori.api.Tag}.
   */
  protected Tag(Parcel in) {
    this.name = in.readString();
    this.type = Type.values()[in.readInt()];
  }

  /**
   * Get tag's name.
   *
   * @return Tag name.
   */
  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Tag tag = (Tag) o;

    return !(name != null ? !name.equals(tag.name) : tag.name != null) && type == tag.type;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }

  @Override
  public int compareTo(Tag another) {
    return this.name.compareTo(another.getName());
  }

  /**
   * Get tag's type.
   * Some APIs do not use tag types and will only use the {@link Type#GENERAL} type.
   *
   * @return Tag type.
   */
  public Type getType() {
    return type;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel p, int flags) {
    p.writeString(name);
    p.writeInt(type.ordinal());
  }

  /**
   * Convert an array of tags into a querystring suitable for use with {@link com.cuddlesoft.nori.api.clients.SearchClient#search(String)}.
   *
   * @param tags Tags
   * @return A space-separated list of tags.
   */
  public static String stringFromArray(Tag[] tags) {
    final StringBuilder sb = new StringBuilder();
    for (Tag tag : tags) {
      sb.append(tag.name).append(" ");
    }
    // Return string while trimming final trailing space.
    return sb.toString().trim();
  }

  /**
   * Create a Tag array from a space-separated list of tags.
   * Sets type for each tag to {@link Tag.Type#GENERAL}.
   *
   * @param query Space-separated list of tags.
   * @return Tag array from given String.
   * @see #arrayFromString(String, com.cuddlesoft.nori.api.Tag.Type)
   */
  public static Tag[] arrayFromString(String query) {
    return arrayFromString(query, Type.GENERAL);
  }

  /**
   * Create a Tag array from a space-separated list of tags.
   *
   * @param query Space-separated list of tags.
   * @param type  Type to set for each tag.
   * @return Tag array from given String.
   * @see #arrayFromString(String)
   */
  public static Tag[] arrayFromString(String query, Tag.Type type) {
    // Return empty array for empty strings.
    if (query == null || query.isEmpty()) {
      return new Tag[0];
    }
    // Split the space-separated string into a String array.
    final String[] strings = query.trim().split(" ");

    // Convert each String into a Tag object.
    final Tag[] tags = new Tag[strings.length];
    for (int i = 0; i < strings.length; i++) {
        tags[i] = new Tag(strings[i], type);
    }
    return tags;
  }

  /**
   * Tag types.
   * Some APIs do not use tag types and will only use the {@link #GENERAL} type.
   */
  public enum Type {
    /** General tags. Describe physical attributes, objects, etc. */
    GENERAL,
    /** Artist tags. Usually Pixiv username(s) of the author of the image. */
    ARTIST,
    /** Character tags. List the characters in the image. */
    CHARACTER,
    /** Copyright tags. List the copyrights (shows, comics, etc.) in the image. */
    COPYRIGHT
  }

}
