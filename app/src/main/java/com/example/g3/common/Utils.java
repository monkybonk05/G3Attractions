/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.g3.common;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;

import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;

/**
 * This class contains shared static utility methods that both the mobile and
 * wearable apps can use.
 */
public class Utils
{
    private static final String TAG = Utils.class.getSimpleName();

    private static final String PREFERENCES_LAT = "lat";
    private static final String PREFERENCES_LNG = "lng";
    private static final String PREFERENCES_GEOFENCE_ENABLED = "geofence";
    private static final String DISTANCE_KM_POSTFIX = "km";
    private static final String DISTANCE_M_POSTFIX = "m";

    /**
     * Check if the app has access to fine location permission. On pre-M
     * devices this will always return true.
     */
    public static boolean checkFineLocationPermission(Context context) {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    /**
     * Calculate distance between two LatLng points and format it nicely for
     * display. As this is a sample, it only statically supports metric units.
     * A production app should check locale and support the correct units.
     */
    public static String formatDistanceBetween(LatLng point1, LatLng point2) {
        if (point1 == null || point2 == null) {
            return null;
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        double distance = Math.round(SphericalUtil.computeDistanceBetween(point1, point2));

        // Adjust to KM if M goes over 1000 (see javadoc of method for note
        // on only supporting metric)
        if (distance >= 1000) {
            numberFormat.setMaximumFractionDigits(1);
            return numberFormat.format(distance / 1000) + DISTANCE_KM_POSTFIX;
        }
        return numberFormat.format(distance) + DISTANCE_M_POSTFIX;
    }

    /**
     * Store the location in the app preferences.
     */
    public static void storeLocation(Context context, LatLng location) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PREFERENCES_LAT, Double.doubleToRawLongBits(location.latitude));
        editor.putLong(PREFERENCES_LNG, Double.doubleToRawLongBits(location.longitude));
        editor.apply();
    }

    /**
     * Fetch the location from app preferences.
     */
    public static LatLng getLocation(Context context) {
        if (!checkFineLocationPermission(context)) {
            return null;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Long lat = prefs.getLong(PREFERENCES_LAT, Long.MAX_VALUE);
        Long lng = prefs.getLong(PREFERENCES_LNG, Long.MAX_VALUE);
        if (lat != Long.MAX_VALUE && lng != Long.MAX_VALUE) {
            Double latDbl = Double.longBitsToDouble(lat);
            Double lngDbl = Double.longBitsToDouble(lng);
            return new LatLng(latDbl, lngDbl);
        }
        return null;
    }

    /**
     * Store if geofencing triggers will show a notification in app preferences.
     */
    public static void storeGeofenceEnabled(Context context, boolean enable) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREFERENCES_GEOFENCE_ENABLED, enable);
        editor.apply();
    }

    /**
     * Retrieve if geofencing triggers should show a notification from app preferences.
     */
    public static boolean getGeofenceEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFERENCES_GEOFENCE_ENABLED, true);
    }

    /**
     * Calculates the square insets on a round device. If the system insets are not set
     * (set to 0) then the inner square of the circle is applied instead.
     *
     * @param display device default display
     * @param systemInsets the system insets
     * @return adjusted square insets for use on a round device
     */
    public static Rect calculateBottomInsetsOnRoundDevice(Display display, Rect systemInsets) {
        Point size = new Point();
        display.getSize(size);
        int width = size.x + systemInsets.left + systemInsets.right;
        int height = size.y + systemInsets.top + systemInsets.bottom;

        // Minimum inset to use on a round screen, calculated as a fixed percent of screen height
        int minInset = (int) (height * Constants.WEAR_ROUND_MIN_INSET_PERCENT);

        // Use system inset if it is larger than min inset, otherwise use min inset
        int bottomInset = systemInsets.bottom > minInset ? systemInsets.bottom : minInset;

        // Calculate left and right insets based on bottom inset
        double radius = width / 2;
        double apothem = radius - bottomInset;
        double chord = Math.sqrt(Math.pow(radius, 2) - Math.pow(apothem, 2)) * 2;
        int leftRightInset = (int) ((width - chord) / 2);

        Log.d(TAG, "calculateBottomInsetsOnRoundDevice: " + bottomInset + ", " + leftRightInset);

        return new Rect(leftRightInset, 0, leftRightInset, bottomInset);
    }
}
