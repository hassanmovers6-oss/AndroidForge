-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.Entity <fields>;
}
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
