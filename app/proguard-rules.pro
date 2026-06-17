# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep our data entities, response wrappers, and Dao interface methods intact for Room and Moshi
-keep class com.example.data.** { *; }

# Keep androidx.room components
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Entity class * { *; }

# Keep Moshi JSON fields from being renamed by R8 reflection
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.squareup.moshi.** { *; }

# Preserve Coroutines and Lifecycle components
-keep class kotlinx.coroutines.** { *; }
-keep class androidx.lifecycle.** { *; }

# Keep Coil Image loaders
-keep class coil.** { *; }

# Keep OkHttp & Retrofit helper reflection elements
-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault, *Annotation*
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**

