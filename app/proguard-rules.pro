# SQLCipher JNI preservation
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room SQLite DAOs and Entities
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep interface * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# Strip debug logging in release builds to eliminate cleartext leakages
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
