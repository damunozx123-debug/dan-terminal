# Shizuku library uses reflection to load its provider.
-keep class rikka.shizuku.** { *; }
-keepclassmembers class * extends android.content.ContentProvider {
    <init>(...);
}
