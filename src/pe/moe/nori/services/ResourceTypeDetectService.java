package pe.moe.nori.services;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import org.apache.http.HttpStatus;
import pe.moe.nori.providers.ServiceSettingsProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/** Service used for auto-detecting the type of imageboard API used by the server. */
public class ResourceTypeDetectService extends IntentService {
  /** Sent in the BroadcastReceiver when STATUS == OK */
  public static final int STATUS_OK = 0x00;
  /** Sent in the BroadcastReceiver when user supplied an invalid URL */
  public static final int STATUS_INVALID_URI = 0xfe;
  /** Sent in the BroadcastReceiver when no valid API was found on the server */
  public static final int STATUS_RESOURCE_NOT_FOUND = 0xff;
  /** Expected API path for Danbooru 2.x APIs */
  private static final String API_PATH_DANBOORU = "/posts.xml";
  /** Expected API path for Danbooru 1.x APIs */
  private static final String API_PATH_DANBOORU_LEGACY = "/post.xml";
  /** Expected API path for Gelbooru APIs */
  private static final String API_PATH_GELBOORU = "/index.php?page=dapi&s=post&q=index";
  /** Expected API path for Shimmie2 APIs */
  private static final String API_PATH_SHIMMIE2 = "/api/danbooru/find_posts/index.xml";

  /** Creates an IntentService. */
  public ResourceTypeDetectService() {
    super("ResourceTypeDetectService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    final ServiceSettingsProvider.ServiceSettings settings = intent.getParcelableExtra("service_settings");
    final Uri uri = Uri.parse(settings.apiUrl);
    final String uriScheme;
    final Intent broadcastIntent = new Intent("pe.moe.nori.services.ResourceTypeDetectService.result");

    // Validate the URL.
    if (uri.getHost() == null || uri.getScheme() == null) {
      sendBroadcast(broadcastIntent.putExtra("status", STATUS_INVALID_URI));
      return;
    }
    // Create a base URL, discarding any path and query the user may have supplied.
    final String baseUrl = uri.getScheme() + "://" + uri.getHost();

    // Loop over API paths.
    for (String apiPath : new String[]{API_PATH_DANBOORU, API_PATH_DANBOORU_LEGACY, API_PATH_GELBOORU, API_PATH_SHIMMIE2}) {
      try {
        // Create a new HTTP connection.
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + apiPath).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        // Get HTTP response code.
        int responseCode = connection.getResponseCode();

        // If response code is okay, put the resource type into the Broadcast intent.
        if (responseCode == HttpStatus.SC_OK) {
          if (apiPath.equals(API_PATH_DANBOORU)) {
            settings.type = ServiceSettingsProvider.SERVICE_TYPE_DANBOORU;
            settings.subtype = ServiceSettingsProvider.SERVICE_SUBTYPE_DANBOORU;
          } else if (apiPath.equals(API_PATH_GELBOORU)) {
            settings.type = ServiceSettingsProvider.SERVICE_TYPE_DANBOORU_LEGACY;
            settings.subtype = ServiceSettingsProvider.SERVICE_SUBTYPE_GELBOORU;
          } else if (apiPath.equals(API_PATH_SHIMMIE2)) {
            settings.type = ServiceSettingsProvider.SERVICE_TYPE_DANBOORU_LEGACY;
            settings.subtype = ServiceSettingsProvider.SERVICE_SUBTYPE_SHIMMIE2;
          }
          // Replace user-supplied API URL with baseUrl.
          settings.apiUrl = baseUrl;

          // Send broadcast.
          broadcastIntent.putExtra("status", STATUS_OK);
          broadcastIntent.putExtra("service_settings", settings);
          sendBroadcast(broadcastIntent);
          return;
        }

      } catch (MalformedURLException e) {
        // Bad URL.
        sendBroadcast(broadcastIntent.putExtra("status", STATUS_INVALID_URI));
        return;
      } catch (IOException e) {
        // Connection error, try next resource type.
        continue;
      }
    }

    // Resource not found.
    sendBroadcast(broadcastIntent.putExtra("status", STATUS_RESOURCE_NOT_FOUND));
  }
}
