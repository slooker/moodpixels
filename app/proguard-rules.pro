# Add project specific ProGuard rules here.

# Preserve stack trace line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Room ─────────────────────────────────────────────────────────────────────
# Keep all Room entities, DAOs, and the database class
-keep class us.slooker.moodpixels.data.db.** { *; }

# ── Gson / JSON export-import ─────────────────────────────────────────────────
# Keep all export data classes used for serialization/deserialization
-keep class us.slooker.moodpixels.export.JsonExporter$* { *; }
-keep class us.slooker.moodpixels.export.JsonImporter$* { *; }

# Gson generic type handling
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ── Data model ────────────────────────────────────────────────────────────────
-keep class us.slooker.moodpixels.data.model.** { *; }

# ── Kotlin coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**