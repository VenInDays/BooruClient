# ProGuard rules for BooruClient
# Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.booru.client.data.model.** { *; }
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
