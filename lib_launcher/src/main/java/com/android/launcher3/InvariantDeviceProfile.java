/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.launcher3;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import androidx.annotation.VisibleForTesting;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.android.launcher3.util.Thunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class InvariantDeviceProfile {

    // This is a static that we use for the default icon size on a 4/5-inch phone
    private static final float DEFAULT_ICON_SIZE_DP = 60;

    private static final float ICON_SIZE_DEFINED_IN_APP_DP = 48;

    // Constants that affects the interpolation curve between statically defined device profile
    // buckets.
    private static final float KNEARESTNEIGHBOR = 3;
    private static final float WEIGHT_POWER = 5;

    // used to offset float not being able to express extremely small weights in extreme cases.
    private static final float WEIGHT_EFFICIENT = 100000f;

    // Profile-defining invariant properties
    String name;
    float minWidthDps;
    float minHeightDps;

    /**
     * Number of icons per row and column in the workspace.
     */
    public int numRows;
    public int numColumns;

    /**
     * Number of icons per row and column in the folder.
     */
    public int numFolderRows;
    public int numFolderColumns;
    public float iconSize;
    public float landscapeIconSize;
    public float iconTextSize;

    /**
     * Number of icons inside the hotseat area.
     */
    public int numHotseatIcons;

    public DeviceProfile landscapeProfile;
    public DeviceProfile portraitProfile;

    public Point defaultWallpaperSize;

    @VisibleForTesting
    public InvariantDeviceProfile() {
    }

    private InvariantDeviceProfile(InvariantDeviceProfile p) {
        this(p.name, p.minWidthDps, p.minHeightDps, p.numRows, p.numColumns,
                p.numFolderRows, p.numFolderColumns,
                p.iconSize, p.landscapeIconSize, p.iconTextSize, p.numHotseatIcons);
    }

    private InvariantDeviceProfile(String n, float w, float h, int r, int c, int fr, int fc,
                                   float is, float lis, float its, int hs) {
        name = n;
        minWidthDps = w;
        minHeightDps = h;
        numRows = r;
        numColumns = c;
        numFolderRows = fr;
        numFolderColumns = fc;
        iconSize = is;
        landscapeIconSize = lis;
        iconTextSize = its;
        numHotseatIcons = hs;
    }

    public InvariantDeviceProfile(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);

        Point smallestSize = new Point();
        Point largestSize = new Point();
        display.getCurrentSizeRange(smallestSize, largestSize);

        // This guarantees that width < height
        minWidthDps = Utilities.dpiFromPx(Math.min(smallestSize.x, smallestSize.y), dm);
        minHeightDps = Utilities.dpiFromPx(Math.min(largestSize.x, largestSize.y), dm);

        ArrayList<InvariantDeviceProfile> closestProfiles = findClosestDeviceProfiles(
                minWidthDps, minHeightDps, getPredefinedDeviceProfiles(context));
        InvariantDeviceProfile interpolatedDeviceProfileOut =
                invDistWeightedInterpolate(minWidthDps, minHeightDps, closestProfiles);

        InvariantDeviceProfile closestProfile = closestProfiles.get(0);
        Log.d("InvariantDeviceProfile", "closestProfile=" + closestProfile);
        numRows = closestProfile.numRows;
        numColumns = closestProfile.numColumns;
        numHotseatIcons = closestProfile.numHotseatIcons;
        numFolderRows = closestProfile.numFolderRows;
        numFolderColumns = closestProfile.numFolderColumns;

        iconSize = interpolatedDeviceProfileOut.iconSize;
        Log.d("InvariantDeviceProfile", "iconSize=" + iconSize);
        landscapeIconSize = interpolatedDeviceProfileOut.landscapeIconSize;
        iconTextSize = interpolatedDeviceProfileOut.iconTextSize;

//        Point realSize = new Point();
//        display.getRealSize(realSize);
//        // The real size never changes. smallSide and largeSide will remain the
//        // same in any orientation.
//        int smallSide = Math.min(realSize.x, realSize.y);
//        int largeSide = Math.max(realSize.x, realSize.y);
//
//        landscapeProfile = new DeviceProfile(context, this, smallestSize, largestSize,
//                largeSide, smallSide, true /* isLandscape */, false /* isMultiWindowMode */);
//        portraitProfile = new DeviceProfile(context, this, smallestSize, largestSize,
//                smallSide, largeSide, false /* isLandscape */, false /* isMultiWindowMode */);
//
//        // We need to ensure that there is enough extra space in the wallpaper
//        // for the intended parallax effects
//        if (context.getResources().getConfiguration().smallestScreenWidthDp >= 720) {
//            defaultWallpaperSize = new Point(
//                    (int) (largeSide * wallpaperTravelToScreenWidthRatio(largeSide, smallSide)),
//                    largeSide);
//        } else {
//            defaultWallpaperSize = new Point(Math.max(smallSide * 2, largeSide), largeSide);
//        }
        update(context);
    }

    public void update(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);

        Point smallestSize = new Point();
        Point largestSize = new Point();
        display.getCurrentSizeRange(smallestSize, largestSize);
        Point realSize = new Point();
        display.getRealSize(realSize);
        // The real size never changes. smallSide and largeSide will remain the
        // same in any orientation.
        int smallSide = Math.min(realSize.x, realSize.y);
        int largeSide = Math.max(realSize.x, realSize.y);

        landscapeProfile = new DeviceProfile(context, this, smallestSize, largestSize,
                largeSide, smallSide, true /* isLandscape */, false /* isMultiWindowMode */);
        portraitProfile = new DeviceProfile(context, this, smallestSize, largestSize,
                smallSide, largeSide, false /* isLandscape */, false /* isMultiWindowMode */);

        // We need to ensure that there is enough extra space in the wallpaper
        // for the intended parallax effects
        if (context.getResources().getConfiguration().smallestScreenWidthDp >= 720) {
            defaultWallpaperSize = new Point(
                    (int) (largeSide * wallpaperTravelToScreenWidthRatio(largeSide, smallSide)),
                    largeSide);
        } else {
            defaultWallpaperSize = new Point(Math.max(smallSide * 2, largeSide), largeSide);
        }
    }

    ArrayList<InvariantDeviceProfile> getPredefinedDeviceProfiles(Context context) {
//        ArrayList<InvariantDeviceProfile> profiles = new ArrayList<>();
//        try (XmlResourceParser parser = context.getResources().getXml(R.xml.device_profiles)) {
//            final int depth = parser.getDepth();
//            int type;
//
//            while (((type = parser.next()) != XmlPullParser.END_TAG ||
//                    parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
//                if ((type == XmlPullParser.START_TAG) && "profile".equals(parser.getName())) {
//                    TypedArray a = context.obtainStyledAttributes(
//                            Xml.asAttributeSet(parser), R.styleable.InvariantDeviceProfile);
//                    int numRows = a.getInt(R.styleable.InvariantDeviceProfile_numRows, 0);
//                    int numColumns = a.getInt(R.styleable.InvariantDeviceProfile_numColumns, 0);
//                    float iconSize = a.getFloat(R.styleable.InvariantDeviceProfile_iconSize, 0);
////                    float iconSize = 48;
//                    profiles.add(new InvariantDeviceProfile(
//                            a.getString(R.styleable.InvariantDeviceProfile_name),
//                            a.getFloat(R.styleable.InvariantDeviceProfile_minWidthDps, 0),
//                            a.getFloat(R.styleable.InvariantDeviceProfile_minHeightDps, 0),
//                            numRows,
//                            numColumns,
//                            a.getInt(R.styleable.InvariantDeviceProfile_numFolderRows, numRows),
//                            a.getInt(R.styleable.InvariantDeviceProfile_numFolderColumns, numColumns),
//                            iconSize,
//                            a.getFloat(R.styleable.InvariantDeviceProfile_landscapeIconSize, iconSize),
//                            a.getFloat(R.styleable.InvariantDeviceProfile_iconTextSize, 0),
//                            a.getInt(R.styleable.InvariantDeviceProfile_numHotseatIcons, numColumns)));
//                    a.recycle();
//                }
//            }
//        } catch (IOException | XmlPullParserException e) {
//            throw new RuntimeException(e);
//        }
//        return profiles;
        ArrayList<InvariantDeviceProfile> predefinedDeviceProfiles = new ArrayList<>();
        // width, height, #rows, #columns, #folder rows, #folder columns,
        // iconSize, iconTextSize, #hotseat, #hotseatIconSize, defaultLayoutId.
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Super Short Stubby",
                255, 300, 2, 3, 2, 3, 48, 48, 13, 3));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Shorter Stubby",
                255, 400, 3, 3, 3, 3, 48, 48, 13, 3));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Short Stubby",
                275, 420, 3, 4, 3, 4, 48, 48, 13, 5));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Stubby",
                255, 450, 3, 4, 3, 4, 48, 48, 13, 5));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus S",
                296, 491.33f, 4, 4, 4, 4, 48, 48, 13, 5));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus 4",
                335, 567, 4, 4, 4, 4, 56, DEFAULT_ICON_SIZE_DP, 13, 5));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus 5",
                359, 567, 4, 4, 4, 4, 56, DEFAULT_ICON_SIZE_DP, 13, 5));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Large Phone",
                406, 694, 5, 5, 4, 4, 56, 64, 14f, 5));
        // The tablet profile is odd in that the landscape orientation
        // also includes the nav bar on the side
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus 7",
                575, 904, 5, 6, 4, 5, 60, 72, 14.4f, 7));
        // Larger tablet profiles always have system bars on the top & bottom
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("Nexus 10",
                727, 1207, 5, 6, 4, 5, 64, 76, 14.4f, 7));
        predefinedDeviceProfiles.add(new InvariantDeviceProfile("20-inch Tablet",
                1527, 2527, 7, 7, 6, 6, 72, 100, 20, 7));
        return predefinedDeviceProfiles;
    }

    private int getLauncherIconDensity(int requiredSize) {
        // Densities typically defined by an app.
        int[] densityBuckets = new int[]{
                DisplayMetrics.DENSITY_LOW,
                DisplayMetrics.DENSITY_MEDIUM,
                DisplayMetrics.DENSITY_TV,
                DisplayMetrics.DENSITY_HIGH,
                DisplayMetrics.DENSITY_XHIGH,
                DisplayMetrics.DENSITY_XXHIGH,
                DisplayMetrics.DENSITY_XXXHIGH
        };

        int density = DisplayMetrics.DENSITY_XXXHIGH;
        for (int i = densityBuckets.length - 1; i >= 0; i--) {
            float expectedSize = ICON_SIZE_DEFINED_IN_APP_DP * densityBuckets[i]
                    / DisplayMetrics.DENSITY_DEFAULT;
            if (expectedSize >= requiredSize) {
                density = densityBuckets[i];
            }
        }

        return density;
    }

    @Thunk
    float dist(float x0, float y0, float x1, float y1) {
        return (float) Math.hypot(x1 - x0, y1 - y0);
    }

    /**
     * Returns the closest device profiles ordered by closeness to the specified width and height
     */
    // Package private visibility for testing.
    ArrayList<InvariantDeviceProfile> findClosestDeviceProfiles(
            final float width, final float height, ArrayList<InvariantDeviceProfile> points) {

        // Sort the profiles by their closeness to the dimensions
        ArrayList<InvariantDeviceProfile> pointsByNearness = points;
        Collections.sort(pointsByNearness, new Comparator<InvariantDeviceProfile>() {
            public int compare(InvariantDeviceProfile a, InvariantDeviceProfile b) {
                return Float.compare(dist(width, height, a.minWidthDps, a.minHeightDps),
                        dist(width, height, b.minWidthDps, b.minHeightDps));
            }
        });

        return pointsByNearness;
    }

    // Package private visibility for testing.
    InvariantDeviceProfile invDistWeightedInterpolate(float width, float height,
                                                      ArrayList<InvariantDeviceProfile> points) {
        float weights = 0;

        InvariantDeviceProfile p = points.get(0);
        if (dist(width, height, p.minWidthDps, p.minHeightDps) == 0) {
            return p;
        }

        InvariantDeviceProfile out = new InvariantDeviceProfile();
        for (int i = 0; i < points.size() && i < KNEARESTNEIGHBOR; ++i) {
            p = new InvariantDeviceProfile(points.get(i));
            float w = weight(width, height, p.minWidthDps, p.minHeightDps, WEIGHT_POWER);
            weights += w;
            out.add(p.multiply(w));
        }
        return out.multiply(1.0f / weights);
    }

    private void add(InvariantDeviceProfile p) {
        iconSize += p.iconSize;
        landscapeIconSize += p.landscapeIconSize;
        iconTextSize += p.iconTextSize;
    }

    private InvariantDeviceProfile multiply(float w) {
        iconSize *= w;
        landscapeIconSize *= w;
        iconTextSize *= w;
        return this;
    }

    public DeviceProfile getDeviceProfile(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE ? landscapeProfile : portraitProfile;
    }

    private float weight(float x0, float y0, float x1, float y1, float pow) {
        float d = dist(x0, y0, x1, y1);
        if (Float.compare(d, 0f) == 0) {
            return Float.POSITIVE_INFINITY;
        }
        return (float) (WEIGHT_EFFICIENT / Math.pow(d, pow));
    }

    /**
     * As a ratio of screen height, the total distance we want the parallax effect to span
     * horizontally
     */
    private static float wallpaperTravelToScreenWidthRatio(int width, int height) {
        float aspectRatio = width / (float) height;

        // At an aspect ratio of 16/10, the wallpaper parallax effect should span 1.5 * screen width
        // At an aspect ratio of 10/16, the wallpaper parallax effect should span 1.2 * screen width
        // We will use these two data points to extrapolate how much the wallpaper parallax effect
        // to span (ie travel) at any aspect ratio:

        final float ASPECT_RATIO_LANDSCAPE = 16 / 10f;
        final float ASPECT_RATIO_PORTRAIT = 10 / 16f;
        final float WALLPAPER_WIDTH_TO_SCREEN_RATIO_LANDSCAPE = 1.5f;
        final float WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT = 1.2f;

        // To find out the desired width at different aspect ratios, we use the following two
        // formulas, where the coefficient on x is the aspect ratio (width/height):
        //   (16/10)x + y = 1.5
        //   (10/16)x + y = 1.2
        // We solve for x and y and end up with a final formula:
        final float x =
                (WALLPAPER_WIDTH_TO_SCREEN_RATIO_LANDSCAPE - WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT) /
                        (ASPECT_RATIO_LANDSCAPE - ASPECT_RATIO_PORTRAIT);
        final float y = WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT - x * ASPECT_RATIO_PORTRAIT;
        return x * aspectRatio + y;
    }

    @Override
    public String toString() {
        return "InvariantDeviceProfile{" +
                "name='" + name + '\'' +
                ", minWidthDps=" + minWidthDps +
                ", minHeightDps=" + minHeightDps +
                ", numRows=" + numRows +
                ", numColumns=" + numColumns +
                ", numFolderRows=" + numFolderRows +
                ", numFolderColumns=" + numFolderColumns +
                ", iconSize=" + iconSize +
                ", landscapeIconSize=" + landscapeIconSize +
                ", iconTextSize=" + iconTextSize +
                ", numHotseatIcons=" + numHotseatIcons +
                ", landscapeProfile=" + landscapeProfile +
                ", portraitProfile=" + portraitProfile +
                ", defaultWallpaperSize=" + defaultWallpaperSize +
                '}';
    }
}