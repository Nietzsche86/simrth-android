# SIMRTH (Sistem Informasi Madrasah Roudhotut Thowalib) Proguard / R8 Configuration

# Preserve Serialization Annotations and Reflection Attributes
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Data Transfer Objects (DTO) for LAN Offline Sync
-keep class id.mts.rth.data.remote.dto.** { *; }
-keepclassmembers class id.mts.rth.data.remote.dto.** { *; }

# Room SQLite Entities and DAOs
-keep class id.mts.rth.data.local.entity.** { *; }
-keepclassmembers class id.mts.rth.data.local.entity.** { *; }
-keep interface id.mts.rth.data.local.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Domain Models
-keep class id.mts.rth.domain.model.** { *; }
-keepclassmembers class id.mts.rth.domain.model.** { *; }

# Koin Dependency Injection
-keep class org.koin.** { *; }

# Ktor Client & OkHttp Engine
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
