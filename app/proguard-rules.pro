# Add project specific ProGuard rules here.

# Keep Room entities
-keep class com.runshare.app.data.** { *; }

# Keep Gson serialization classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.runshare.app.model.** { *; }

# OSMDroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# ZXing
-keep class com.google.zxing.** { *; }
