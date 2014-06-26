/*
 * This file is part of nori.
 * Copyright (c) 2014 vomitcuddle <shinku@dollbooru.org>
 * License: ISC
 */

package com.cuddlesoft.nori.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

/**
 * Network utility class.
 */
public abstract class NetworkUtils {

  /**
   * Decides if low-resolution ("samples") images should be fetched by default instead of full-size images, based on:
   * - Screen density
   * - Network link speed and quality
   * - Is the network metered? ($$$ per MB)
   *
   * @return true if low-resolution images should be used.
   */
  public static boolean shouldFetchImageSamples(Context context) {
    // [!] Note that the low-resolution images aren't actually that bad unless the user zooms in on them.
    // [!] They're meant for cases where the original image is much larger than an average desktop browser window.

    // Check screen resolution.
    if (context.getResources().getDisplayMetrics().density <= 1.0) {
      return false;
    }

    // Get system connectivity manager service.
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    // Check if network is metered.
    if (isActiveNetworkMetered(cm)) {
      return false;
    }
    // Check link quality.
    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
    if (!isConnectionFast(networkInfo.getType(), networkInfo.getSubtype())) {
      return false;
    }

    // TODO: Check user preference.
    // Preference should probably default to false for better latency.

    return true;
  }

  /**
   * Check if active connection is metered. (API 16+)
   *
   * @param cm Instance of {@link android.net.ConnectivityManager}
   * @return true if user pays for bandwidth.
   */
  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private static boolean isActiveNetworkMetered(ConnectivityManager cm) {
    return Build.VERSION.SDK_INT >= 16 && cm.isActiveNetworkMetered();
  }

  /**
   * Check if given connection type is fast enough to download high res images.
   *
   * @param type    Connection type constant, as specified in {@link android.net.ConnectivityManager}.
   * @param subType Connection subtype constant, as specified in {@link android.telephony.TelephonyManager}.
   * @return true if high res images should be downloaded by default.
   */
  private static boolean isConnectionFast(int type, int subType) {
    if (type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_WIMAX) {
      // WiFi is good.
      return true;
    } else if (type == ConnectivityManager.TYPE_MOBILE) {
      // Exclude mobile network types with avg speeds below or close to ~1Mbps.
      // Giving new technologies introduced after this code was written benefit of the doubt.
      switch (subType) {
        case TelephonyManager.NETWORK_TYPE_1xRTT: // ~ 50-100 kbps
        case TelephonyManager.NETWORK_TYPE_CDMA: // ~ 14-64 kbps
        case TelephonyManager.NETWORK_TYPE_EDGE: // ~ 50-100 kbps
        case TelephonyManager.NETWORK_TYPE_EVDO_0: // ~ 400-1000 kbps
        case TelephonyManager.NETWORK_TYPE_EVDO_A: // ~ 600-1400 kbps
        case TelephonyManager.NETWORK_TYPE_GPRS: // ~ 100 kbps
        case TelephonyManager.NETWORK_TYPE_HSPA: // ~ 700-1700 kbps
        case TelephonyManager.NETWORK_TYPE_IDEN: // ~ 25 kbps
        case TelephonyManager.NETWORK_TYPE_EHRPD: // ~ 1-2 Mbps
          return false;
        default:
          return true;
      }
    } else {
      return false;
    }
  }

}
