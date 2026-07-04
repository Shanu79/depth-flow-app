# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# --- Credential Manager & Google ID ---
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keepnames class com.google.android.libraries.identity.googleid.** { *; }

# --- Kotlin Metadata (Crucial for Suspend/Generics) ---
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations, AnnotationDefault, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }

# --- Retrofit 2 ---
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepattributes Signature, Exceptions
-dontwarn retrofit2.**

# --- Retrofit 2.11.0+ Compatibility (R8 Full Mode) ---
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- DepthFlow API & Data (Total Protection) ---
-keeppackagenames com.shin.depthflow.data.**
-keep class com.shin.depthflow.data.** { *; }
-keep interface com.shin.depthflow.data.** { *; }

# --- Moshi ---
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keepclassmembers class ** {
    @com.squareup.moshi.Json *;
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- Keep BuildConfig ---
-keep class com.shin.depthflow.BuildConfig { *; }
