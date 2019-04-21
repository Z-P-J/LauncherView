# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep,allowshrinking,allowoptimization class com.android.launcher3.** {
  *;
}

-keep class com.android.launcher3.allapps.AllAppsBackgroundDrawable {
  public void setAlpha(int);
  public int getAlpha();
}

-keep class com.android.launcher3.BaseRecyclerViewFastScrollBar {
  public void setThumbWidth(int);
  public int getThumbWidth();
  public void setTrackWidth(int);
  public int getTrackWidth();
}

-keep class com.android.launcher3.BaseRecyclerViewFastScrollPopup {
  public void setAlpha(float);
  public float getAlpha();
}

-keep class com.android.launcher3.ButtonDropTarget {
  public int getTextColor();
}

-keep class com.android.launcher3.CellLayout {
  public float getBackgroundAlpha();
  public void setBackgroundAlpha(float);
}

-keep class com.android.launcher3.CellLayout$LayoutParams {
  public void setWidth(int);
  public int getWidth();
  public void setHeight(int);
  public int getHeight();
  public void setX(int);
  public int getX();
  public void setY(int);
  public int getY();
}

-keep class com.android.launcher3.views.BaseDragLayer$LayoutParams {
  public void setWidth(int);
  public int getWidth();
  public void setHeight(int);
  public int getHeight();
  public void setX(int);
  public int getX();
  public void setY(int);
  public int getY();
}

-keep class com.android.launcher3.FastBitmapDrawable {
  public void setDesaturation(float);
  public float getDesaturation();
  public void setBrightness(float);
  public float getBrightness();
}

-keep class com.android.launcher3.MemoryDumpActivity {
  *;
}

-keep class com.android.launcher3.PreloadIconDrawable {
  public float getAnimationProgress();
  public void setAnimationProgress(float);
}

-keep class com.android.launcher3.pageindicators.CaretDrawable {
  public float getCaretProgress();
  public void setCaretProgress(float);
}

-keep class com.android.launcher3.Workspace {
  public float getBackgroundAlpha();
  public void setBackgroundAlpha(float);
}

# Proguard will strip new callbacks in LauncherApps.Callback from
# WrappedCallback if compiled against an older SDK. Don't let this happen.
-keep class com.android.launcher3.compat.** {
  *;
}

-keep class com.android.launcher3.graphics.ShadowDrawable {
  public <init>(...);
}

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**

# Proguard will strip methods required for talkback to properly scroll to
# next row when focus is on the last item of last row when using a RecyclerView
# Keep optimized and shrunk proguard to prevent issues like this when using
# support jar.
-keep class android.support.v7.widget.RecyclerView { *; }

# LauncherAppTransitionManager
-keep class com.android.launcher3.LauncherAppTransitionManagerImpl {
    public <init>(...);
}

# InstantAppResolver
-keep class com.android.quickstep.InstantAppResolverImpl {
    public <init>(...);
}

# MainProcessInitializer
-keep class com.android.quickstep.QuickstepProcessInitializer {
    public <init>(...);
}

# UserEventDispatcherExtension
-keep class com.android.quickstep.logging.UserEventDispatcherExtension {
    public <init>(...);
}

-keep interface com.android.launcher3.userevent.nano.LauncherLogProto.** {
  *;
}

-keep interface com.android.launcher3.model.nano.LauncherDumpProto.** {
  *;
}

# Discovery bounce animation
-keep class com.android.launcher3.allapps.DiscoveryBounce$VerticalProgressWrapper {
  public void setProgress(float);
  public float getProgress();
}

# BUG(70852369): Surpress additional warnings after changing from Proguard to R8
-dontwarn android.app.**
-dontwarn android.view.**
-dontwarn android.os.**
