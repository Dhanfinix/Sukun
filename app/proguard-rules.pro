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

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Application defined models for Retrofit/Gson
-keep class dhanfinix.android.sukun.feature.prayer.data.model.** { *; }

# Keep generic signature information
-keepattributes Signature
-keepattributes *Annotation*

# For AndroidTest
-dontwarn javax.lang.model.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn net.bytebuddy.**
-dontwarn org.mockito.**
-dontwarn junit.**
-dontwarn org.junit.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn sun.misc.**

-keep class androidx.test.** { *; }
-keep interface androidx.test.** { *; }
-keep class androidx.tracing.** { *; }
-dontwarn androidx.test.**
-dontwarn androidx.tracing.**