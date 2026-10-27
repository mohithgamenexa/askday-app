# kotlinx.serialization: keep generated serializers for the saved data classes.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class app.askday.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.askday.data.**$$serializer { *; }
