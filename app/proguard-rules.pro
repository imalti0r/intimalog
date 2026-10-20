# IntimaLog ProGuard 规则

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.intima.log.**$$serializer { *; }
-keepclassmembers class com.intima.log.** { *** Companion; }
-keepclasseswithmembers class com.intima.log.** { kotlinx.serialization.KSerializer serializer(...); }

# SQLCipher
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }

# protobuf
-keep class com.google.protobuf.** { *; }
-keep class com.intima.log.proto.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Glance / AppWidget
-keep class com.intima.log.widget.** { *; }
