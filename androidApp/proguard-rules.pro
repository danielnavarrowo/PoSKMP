# SQLDelight
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# DataStore & Protobuf
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# Ktor loads this JSON serialization provider through META-INF/services.
#noinspection ShrinkerUnresolvedReference
-keep class io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider { *; }

# ML Kit & Firebase ComponentDiscovery (instantiated reflectively via Class.getDeclaredConstructor())
#noinspection ShrinkerUnresolvedReference
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
    *;
}
-keep class com.google.mlkit.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.dnavarro.poskmp.**$$serializer { *; }
-keepclassmembers class com.dnavarro.poskmp.** {
    *** Companion;
}
-keepclasseswithmembers class com.dnavarro.poskmp.** {
    kotlinx.serialization.KSerializer serializer(...);
}
