/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.test;

import android.os.Bundle;
import android.test.AndroidTestCase;
import android.util.Log;

import com.cuddlesoft.nori.api.Image;
import com.cuddlesoft.nori.api.Tag;

import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Tests and utilities for testing the {@link com.cuddlesoft.nori.api.Image} class.
 */
public class ImageTests extends AndroidTestCase {

  /** LogCat tag. */
  private static final String TAG = "com.cuddlesoft.nori.test.ImageTests";

  /**
   * RegEx Pattern used for matching URLs.
   * Courtesy of John Gruber (http://daringfireball.net/2010/07/improved_regex_for_matching_urls)
   * (Public Domain)
   */
  private static final Pattern urlPattern = Pattern.compile("(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))");
  /** RegEx pattern for matching numerical Strings (used for IDs). */
  private static final Pattern integerPattern = Pattern.compile("^\\d+$");

  /** Test the {@link Image#writeToParcel(android.os.Parcel, int)} method. */
  public void testWriteToParcel() throws Throwable {
    final Image original = getImageWithTestData();
    final Image unParceled;
    final Bundle bundle = new Bundle();

    // Parcel and un-parcel the original image.
    bundle.putParcelable("image", original);
    unParceled = bundle.getParcelable("image");

    // Verify data in the un-parceled Image object.
    verifyImage(unParceled);
    assertThat(unParceled.fileUrl).isEqualTo(original.fileUrl);
    assertThat(unParceled.width).isEqualTo(original.width);
    assertThat(unParceled.height).isEqualTo(original.height);
    assertThat(unParceled.previewUrl).isEqualTo(original.previewUrl);
    assertThat(unParceled.previewWidth).isEqualTo(original.previewWidth);
    assertThat(unParceled.previewHeight).isEqualTo(original.previewHeight);
    assertThat(unParceled.sampleUrl).isEqualTo(original.sampleUrl);
    assertThat(unParceled.sampleWidth).isEqualTo(original.sampleWidth);
    assertThat(unParceled.sampleHeight).isEqualTo(original.sampleHeight);
    assertThat(unParceled.tags).containsOnly(original.tags);
    assertThat(unParceled.id).isEqualTo(original.id);
    assertThat(unParceled.parentId).isEqualTo(original.parentId);
    assertThat(unParceled.webUrl).isEqualTo(original.webUrl);
    assertThat(unParceled.pixivId).isEqualTo(original.pixivId);
    assertThat(unParceled.obscenityRating).isEqualTo(original.obscenityRating);
    assertThat(unParceled.score).isEqualTo(original.score);
    assertThat(unParceled.md5).isEqualTo(original.md5);
    assertThat(unParceled.createdAt).isEqualTo(original.createdAt);
  }

  /** Tests the {@link Image#getPixivIdFromUrl(String)} method. */
  public void testGetPixivIdFromUrl() throws Throwable {
    assertThat(Image.getPixivIdFromUrl("http://www.pixiv.net/member_illust.php?mode=medium&illust_id=44466677")).isEqualTo("44466677");
  }

  /** Tests the {@link com.cuddlesoft.nori.api.Image.ObscenityRating#fromString(String)} method. */
  public void testObscenityRatingFromString() throws Throwable {
    assertThat(Image.ObscenityRating.fromString("Safe")).isEqualTo(Image.ObscenityRating.SAFE);
    assertThat(Image.ObscenityRating.fromString("Questionable")).isEqualTo(Image.ObscenityRating.QUESTIONABLE);
    assertThat(Image.ObscenityRating.fromString("Explicit")).isEqualTo(Image.ObscenityRating.EXPLICIT);
    assertThat(Image.ObscenityRating.fromString("Undefined")).isEqualTo(Image.ObscenityRating.UNDEFINED);
  }

  /**
   * Get an image with made-up data suitable for testing.
   */
  private static Image getImageWithTestData() {
    final Image image = new Image();
    image.fileUrl = "http://awesomeboorusite.org/data/images/image.png";
    image.width = 1000;
    image.height = 900;
    image.previewUrl = "http://awesomeboorusite.org/data/previews/image.png";
    image.previewWidth = 150;
    image.previewHeight = 130;
    image.sampleUrl = "http://awesomeboorusite.org/data/samples/image.png";
    image.sampleWidth = 850;
    image.sampleHeight = 800;
    image.tags = new Tag[]{new Tag("duck", Tag.Type.GENERAL), new Tag("revolutionary_girl_utena", Tag.Type.COPYRIGHT)};
    image.id = "123456";
    image.parentId = "123455";
    image.webUrl = "http://awesomeboorusite.org/post/view/image";
    image.pixivId = "111222333";
    image.obscenityRating = Image.ObscenityRating.SAFE;
    image.score = 23;
    image.source = "http://pixiv.com/duck.png";
    image.md5 = "cfaf278e8f522c72644cee2a753d2845";
    image.createdAt = new Date(1398902400);

    return image;
  }

  /**
   * Verify validity of an {@link com.cuddlesoft.nori.api.Image} object.
   * Used to ensure that Image values returned by each individual API client are correct.
   *
   * @throws Throwable Assertion failure.
   */
  public static void verifyImage(Image image) throws Throwable {
    // Verify URLs.
    assertThat(image.fileUrl).matches(urlPattern);
    assertThat(image.previewUrl).matches(urlPattern);
    assertThat(image.sampleUrl).matches(urlPattern);
    assertThat(image.webUrl).matches(urlPattern);

    // Verify image sizes are set and that they are positive integers.
    assertThat(image.width).isPositive();
    assertThat(image.height).isPositive();
    if (image.previewWidth == 0)
      Log.w(TAG, String.format(Locale.US, "Preview width was 0 for image: %s", image.webUrl));
    assertThat(image.previewWidth).isGreaterThanOrEqualTo(0);
    if (image.previewHeight == 0)
      Log.w(TAG, String.format(Locale.US, "Preview height was 0 for image: %s", image.webUrl));
    assertThat(image.previewHeight).isGreaterThanOrEqualTo(0);
    if (image.sampleWidth == 0)
      Log.w(TAG, String.format(Locale.US, "Sample width was 0 for image: %s", image.webUrl));
    assertThat(image.sampleWidth).isGreaterThanOrEqualTo(0);
    if (image.sampleHeight == 0)
      Log.w(TAG, String.format(Locale.US, "Sample height was 0 for image: %s", image.webUrl));
    assertThat(image.sampleHeight).isGreaterThanOrEqualTo(0);

    // Verify tags.
    if (image.tags.length == 0)
      Log.w(TAG, String.format(Locale.US, "No tags for image: %s", image.webUrl));
    for (Tag tag : image.tags) {
      assertThat(tag.getName()).isNotEmpty();
      assertThat(tag.getType()).isNotNull();
    }

    // Verify numerical strings (IDs)
    assertThat(image.id).isNotEmpty().matches(integerPattern);
    if (image.parentId != null && !image.parentId.isEmpty())
      assertThat(image.parentId).matches(integerPattern);
    else
      Log.w(TAG, String.format(Locale.US, "No parent ID for image: %s", image.webUrl));
    if (image.pixivId != null)
      assertThat(image.pixivId).isNotEmpty().matches(integerPattern);
    else
      Log.w(TAG, String.format(Locale.US, "No Pixiv ID for image: %s", image.webUrl));

    // Misc stuff.
    assertThat(image.obscenityRating).isNotNull();
    if (image.source == null || image.source.isEmpty())
      Log.w(TAG, String.format(Locale.US, "No source for image: %s", image.webUrl));
    else
      assertThat(image.source).isNotEmpty();
    assertThat(image.md5).hasSize(32); // MD5 hashes are always 32 characters long.
    assertThat(image.createdAt).overridingErrorMessage("createdAt null for image: %s", image.webUrl).isNotNull();
  }

}
