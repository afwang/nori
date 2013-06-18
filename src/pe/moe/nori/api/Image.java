/**
 * This file is part of Nori.
 * Copyright (c) 2013 Obscure Reference
 * License: GPLv3
 */
package pe.moe.nori.api;

import java.util.Date;

/**
 * An image and its associated metadata.
 */
public class Image {
	/** Obscenity rating */
	public enum ObscenityRating {
	    UNDEFINED,
		SAFE,
		QUESTIONABLE,
		EXPLICIT
	}
	
	/** Image URL */
	public String fileUrl;
	/** Image width */
	public int width;
	/** Image height */
	public int height;
	
	/** Thumbnail URL */
	public String previewUrl;
	/** Thumbnail width */
	public int previewWidth;
	/** Thumbnail height */
	public int previewHeight;
	
	/** Sample URL */
	public String sampleUrl;
	/** Sample width */
	public int sampleWidth;
	/** Sample height */
	public int sampleHeight;
	
	/** General tags */
	public String[] generalTags;
	/** Artist tags */
	public String[] artistTags;
	/** Character tags */
	public String[] characterTags;
	/** Copyright tags */
	public String[] copyrightTags;
	
	/** Image ID */
	public long id;
	/** Parent ID */
	public long parentId;
	
	/** Obscenity rating */
	public ObscenityRating obscenityRating;
	/** Popularity score */
	public int score;
	/** Source URL */
	public String source;
	/** MD5 hash */
	public String md5;
	
	/** Has comments */
	public boolean hasComments;
	/** Creation date */
	public Date createdAt;
}
