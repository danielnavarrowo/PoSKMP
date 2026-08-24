# Keep java.sql and sun.misc.Unsafe
-keep class java.sql.** { *; }
-dontwarn java.sql.**

-keep class sun.misc.Unsafe { *; }
-dontwarn sun.misc.Unsafe

# SQLDelight and SQLite JDBC
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**
-keep class org.sqlite.** { *; }
-dontwarn org.sqlite.**
-dontwarn org.slf4j.**

# DataStore & Protobuf
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**


# Ktor loads this JSON serialization provider through META-INF/services.
-keep class io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider { *; }

# Skiko & JetBrains
-dontwarn com.jetbrains.**
