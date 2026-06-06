# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# 1. Advanced Obfuscation & Structural Obfuscation
# confounds decompilers by renaming classes into a flat, random structure
-repackageclasses 'com.example.obfuscated'
-allowaccessmodification
-overloadaggressively

# Keep line number and source file info scrambled but useful for crash reporting (hashed)
-keepattributes SourceFile,LineNumberTable,Signature,EnclosingMethod,InnerClasses,RuntimeVisibleAnnotations
-renamesourcefileattribute SourceFile

# 2. Prevent Scrapers - Strip Log outputs (v, d, i, w) completely in final production bytecode
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 3. Model & Serialization Protection (to avoid breaking reflect-based adapters)
# Keep Moshi API Request and Response structures
-keep class com.example.data.api.ConnectRequest { *; }
-keep class com.example.data.api.HeartbeatRequest { *; }
-keep class com.example.data.api.PollRequest { *; }
-keep class com.example.data.api.PollMessage { *; }
-keep class com.example.data.api.PollResponse { *; }
-keep class com.example.data.api.SmsStatusRequest { *; }
-keep class com.example.data.api.IncomingSmsRequest { *; }

# Keep Network interfaces utilized by Retrofit
-keep interface com.example.data.api.SupabaseService { *; }
-keep class * implements com.example.data.api.SupabaseService { *; }

# Keep Room database models and access structures
-keep class com.example.data.database.Message { *; }
-keep class com.example.data.database.OutboxEvent { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep interface com.example.data.database.MessageDao { *; }
-keep interface com.example.data.database.OutboxEventDao { *; }
-keep class com.example.data.database.AppDatabase { *; }

# 4. Standard Compose and Jetpack Library preservation
-keep class androidx.compose.ui.platform.testTag { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# 5. OkHttp Optional Platform library warnings suppression
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**


