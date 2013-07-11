package pe.moe.nori.api;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/** An image and its associated metadata. */
public class Image implements Parcelable {
  /** Class loader used when deserializing from {@link Parcel}. */
  public static final Parcelable.Creator<Image> CREATOR = new Parcelable.Creator<Image>() {

    @Override
    public Image createFromParcel(Parcel in) {
      // Use the parcel constructor.
      return new Image(in);
    }

    @Override
    public Image[] newArray(int size) {
      // Create a new array.
      return new Image[size];
    }
  };
  /** Image URL */
  public String fileUrl;
  /** Image width */
  public Integer width;
  /** Image height */
  public Integer height;
  /** Web URL */
  public String webUrl;
  /** Thumbnail URL */
  public String previewUrl;
  /** Thumbnail width */
  public Integer previewWidth;
  /** Thumbnail height */
  public Integer previewHeight;
  /** Sample URL */
  public String sampleUrl;
  /** Sample width */
  public Integer sampleWidth;
  /** Sample height */
  public Integer sampleHeight;
  /** General tags */
  public String[] generalTags;
  /** Artist tags */
  public String[] artistTags;
  /** Character tags */
  public String[] characterTags;
  /** Copyright tags */
  public String[] copyrightTags;
  /** Image ID */
  public Long id;
  /** Parent ID */
  public Long parentId;
  /** Pixiv ID */
  public Long pixivId = -1L;
  /** Obscenity rating */
  public ObscenityRating obscenityRating;
  /** Popularity score */
  public Integer score;
  /** Source URL */
  public String source;
  /** MD5 hash */
  public String md5;
  /** Has comments */
  public boolean hasComments = false;
  /** Creation date */
  public Date createdAt;

  /** Default constructor */
  public Image() {
  }

  /**
   * Constructor used when deserializing from a {@link Parcel}.
   *
   * @param in {@link Parcel} to read values from.
   */
  protected Image(Parcel in) {
    // Read values from parcel.
    fileUrl = in.readString();
    width = in.readInt();
    height = in.readInt();
    webUrl = in.readString();
    previewUrl = in.readString();
    previewWidth = in.readInt();
    previewHeight = in.readInt();
    sampleUrl = in.readString();
    sampleWidth = in.readInt();
    sampleHeight = in.readInt();
    generalTags = in.createStringArray();
    artistTags = in.createStringArray();
    characterTags = in.createStringArray();
    copyrightTags = in.createStringArray();
    id = in.readLong();
    parentId = in.readLong();
    pixivId = in.readLong();
    obscenityRating = ObscenityRating.values()[in.readInt()];
    score = in.readInt();
    source = in.readString();
    md5 = in.readString();
    hasComments = in.readByte() != 0x00;
    final long tmpCreatedAt = in.readLong();
    createdAt = tmpCreatedAt != -1 ? new Date(tmpCreatedAt) : null;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    // Write values to parcel.
    dest.writeString(fileUrl);
    dest.writeInt(width);
    dest.writeInt(height);
    dest.writeString(webUrl);
    dest.writeString(previewUrl);
    dest.writeInt(previewWidth);
    dest.writeInt(previewHeight);
    dest.writeString(sampleUrl);
    dest.writeInt(sampleWidth);
    dest.writeInt(sampleHeight);
    dest.writeStringArray(generalTags);
    dest.writeStringArray(artistTags);
    dest.writeStringArray(characterTags);
    dest.writeStringArray(copyrightTags);
    dest.writeLong(id);
    dest.writeLong(parentId);
    dest.writeLong(pixivId);
    dest.writeInt(obscenityRating.ordinal());
    dest.writeInt(score);
    dest.writeString(source);
    dest.writeString(md5);
    dest.writeByte((byte) (hasComments ? 0x01 : 0x00));
    dest.writeLong(createdAt != null ? createdAt.getTime() : -1L);
  }

  /** Obscenity ratings */
  public enum ObscenityRating {
    UNDEFINED,
    SAFE,
    QUESTIONABLE,
    EXPLICIT
  }
}
