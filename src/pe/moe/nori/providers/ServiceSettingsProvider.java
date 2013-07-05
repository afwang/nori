package pe.moe.nori.providers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.AsyncTaskLoader;
import com.android.volley.RequestQueue;
import pe.moe.nori.api.BooruClient;
import pe.moe.nori.api.Danbooru;
import pe.moe.nori.api.DanbooruLegacy;

import java.util.ArrayList;
import java.util.List;


public class ServiceSettingsProvider {
  private static final int SERVICE_TYPE_DANBOORU = 0;
  private static final int SERVICE_TYPE_DANBOORU_LEGACY = 1;
  private static final int SERVICE_SUBTYPE_DANBOORU = 0;
  private static final int SERVICE_SUBTYPE_GELBOORU = 1;
  private static final int SERVICE_SUBTYPE_SHIMMIE2 = 2;
  private final Context mContext;
  private final SQLiteOpenHelper mDatabaseHelper;

  /**
   * Create a new ServiceSettingsProvider
   *
   * @param context Activity context
   */
  public ServiceSettingsProvider(Context context) {
    mContext = context;
    mDatabaseHelper = new DatabaseOpenHelper(context);
  }

  /**
   * Get a new instance of {@link ServiceSettingsLoader} that loads all settings in the database.
   *
   * @return An instance of {@link ServiceSettingsLoader} based on current context.
   */
  public ServiceSettingsLoader getServiceSettingsLoader() {
    return new ServiceSettingsLoader(mContext, mDatabaseHelper);
  }

  /**
   * Get a new instance of {@link ServiceSettingsLoader} that loads settings of service specified by ID.
   *
   * @param serviceId ID of service to query of.
   * @return An instance of {@link ServiceSettingsLoader} based on current context.
   */
  public ServiceSettingsLoader getServiceSettingsLoader(int serviceId) {
    return new ServiceSettingsLoader(mContext, mDatabaseHelper, serviceId);
  }

  public static class ServiceSettings implements Parcelable {
    public static final Parcelable.Creator<ServiceSettings> CREATOR = new Parcelable.Creator<ServiceSettings>() {

      @Override
      public ServiceSettings createFromParcel(Parcel source) {
        return new ServiceSettings(source);
      }

      @Override
      public ServiceSettings[] newArray(int size) {
        return new ServiceSettings[size];
      }
    };
    public int id;
    public String name;
    public int type;
    public int subtype;
    public String apiUrl;
    public boolean requiresAuthentication;
    public String username;
    public String passphrase;

    public ServiceSettings() {
    }

    public ServiceSettings(Parcel in) {
      id = in.readInt();
      name = in.readString();
      type = in.readInt();
      subtype = in.readInt();
      apiUrl = in.readString();
      requiresAuthentication = (in.readByte() == 0x01);
      if (requiresAuthentication) {
        username = in.readString();
        passphrase = in.readString();
      }
    }

    /**
     * Create a new {@link BooruClient} based on {@link ServiceSettings}
     *
     * @param requestQueue Android Volley {@link RequestQueue}
     * @param settings     Client settings
     * @return A new instance of {link @BooruClient}, null if settings are null or incorrect.
     */
    public static BooruClient createClient(RequestQueue requestQueue, ServiceSettings settings) {
      if (settings == null)
        return null;

      if (settings.type == SERVICE_TYPE_DANBOORU)
        return new Danbooru(requestQueue, settings.username, settings.passphrase);
      else if (settings.type == SERVICE_TYPE_DANBOORU_LEGACY) {
        if (settings.subtype == SERVICE_SUBTYPE_DANBOORU)
          return new DanbooruLegacy(settings.apiUrl, DanbooruLegacy.ApiSubtype.DANBOORU, requestQueue);
        else if (settings.subtype == SERVICE_SUBTYPE_GELBOORU)
          return new DanbooruLegacy(settings.apiUrl, DanbooruLegacy.ApiSubtype.GELBOORU, requestQueue);
        else if (settings.subtype == SERVICE_SUBTYPE_SHIMMIE2)
          return new DanbooruLegacy(settings.apiUrl, DanbooruLegacy.ApiSubtype.SHIMMIE2, requestQueue);
      }

      return null;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeInt(id);
      dest.writeString(name);
      dest.writeInt(type);
      dest.writeInt(subtype);
      dest.writeString(apiUrl);
      dest.writeByte((byte) (requiresAuthentication ? 0x01 : 0x00));
      if (requiresAuthentication) {
        dest.writeString(username);
        dest.writeString(passphrase);
      }
    }
  }

  private static class DatabaseOpenHelper extends SQLiteOpenHelper {
    public static final String SERVICE_SETTINGS_TABLE_NAME = "service_settings";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_SUBTYPE = "subtype";
    public static final String COLUMN_API_URL = "api_url";
    public static final String COLUMN_REQUIRES_AUTHENTICATION = "req_auth";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSPHRASE = "passphrase";
    public static final String SERVICE_SETTINGS_TABLE_CREATE =
        "CREATE TABLE " + SERVICE_SETTINGS_TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_NAME + " TEXT NOT NULL, " +
            COLUMN_TYPE + " INTEGER NOT NULL, " +
            COLUMN_SUBTYPE + " INTEGER NOT NULL, " +
            COLUMN_API_URL + " TEXT NOT NULL, " +
            COLUMN_REQUIRES_AUTHENTICATION + " INTEGER DEFAULT 0, " +
            COLUMN_USERNAME + " TEXT, " +
            COLUMN_PASSPHRASE + " TEXT" + ");";
    public static final String SERVICE_SETTINGS_TABLE_POPULATE =
        "INSERT INTO " + SERVICE_SETTINGS_TABLE_NAME + " (" +
            COLUMN_NAME + ", " + COLUMN_TYPE + ", " +
            COLUMN_SUBTYPE + ", " + COLUMN_API_URL +
            ") VALUES ('Safebooru'," + SERVICE_TYPE_DANBOORU_LEGACY + "," + SERVICE_SUBTYPE_GELBOORU + "," +
            "'http://safebooru.org'" + ");";
    private static final String DATABASE_NAME = "service_settings.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseOpenHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(SERVICE_SETTINGS_TABLE_CREATE);
      db.execSQL(SERVICE_SETTINGS_TABLE_POPULATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
  }

  /** Loads {@link ServiceSettings} from the database asynchronously. */
  public static class ServiceSettingsLoader extends AsyncTaskLoader<List<ServiceSettings>> {
    private final Integer mServiceId;
    private final SQLiteOpenHelper mDatabaseHelper;
    private List<ServiceSettings> mSettingsList;

    /**
     * Creates a new asynchronous {@link ServiceSettings} loader.
     *
     * @param context  Activity context
     * @param dbHelper Database open helper
     */
    public ServiceSettingsLoader(Context context, SQLiteOpenHelper dbHelper) {
      super(context);
      mDatabaseHelper = dbHelper;
      mServiceId = null;
    }

    /**
     * Creates a new asynchronous {@link ServiceSettings} loader
     *
     * @param context   Activity context
     * @param dbHelper  Database open helper
     * @param serviceId Service ID
     */
    public ServiceSettingsLoader(Context context, SQLiteOpenHelper dbHelper, int serviceId) {
      super(context);
      mDatabaseHelper = dbHelper;
      mServiceId = serviceId;
    }

    @Override
    protected void onStartLoading() {
      if (mSettingsList != null) {
        deliverResult(mSettingsList);
      }
      if (takeContentChanged() || mSettingsList == null) {
        forceLoad();
      }
    }

    @Override
    public List<ServiceSettings> loadInBackground() {
      ArrayList<ServiceSettings> serviceSettingsArrayList = new ArrayList<ServiceSettings>();

      // Query database for cursor.
      SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
      Cursor c = null;
      if (db != null) {
        c = db.query(DatabaseOpenHelper.SERVICE_SETTINGS_TABLE_NAME, null, mServiceId == null ? null : "id = ?",
            mServiceId == null ? null : new String[]{mServiceId.toString()}, null, null, null,
            mServiceId == null ? null : "1");
      } else {
        return serviceSettingsArrayList;
      }

      // Populate list with data.
      ServiceSettings serviceSettings;
      while (c.moveToNext()) {
        serviceSettings = new ServiceSettings();
        serviceSettings.id = c.getInt(c.getColumnIndex(DatabaseOpenHelper.COLUMN_ID));
        serviceSettings.name = c.getString(c.getColumnIndex(DatabaseOpenHelper.COLUMN_NAME));
        serviceSettings.type = c.getInt(c.getColumnIndex(DatabaseOpenHelper.COLUMN_TYPE));
        serviceSettings.subtype = c.getInt(c.getColumnIndex(DatabaseOpenHelper.COLUMN_SUBTYPE));
        serviceSettings.apiUrl = c.getString(c.getColumnIndex(DatabaseOpenHelper.COLUMN_API_URL));
        serviceSettings.requiresAuthentication = c.getInt(c.getColumnIndex(DatabaseOpenHelper.COLUMN_REQUIRES_AUTHENTICATION)) == 1;
        serviceSettings.username = c.getString(c.getColumnIndex(DatabaseOpenHelper.COLUMN_USERNAME));
        serviceSettings.passphrase = c.getString(c.getColumnIndex(DatabaseOpenHelper.COLUMN_PASSPHRASE));

        serviceSettingsArrayList.add(serviceSettings);
      }

      // Clean up.
      c.close();
      mDatabaseHelper.close();
      // Cache result for later use.
      mSettingsList = serviceSettingsArrayList;

      return serviceSettingsArrayList;
    }
  }
}