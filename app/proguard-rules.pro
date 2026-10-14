# Keep classes used by the app and avoid stripping needed runtime behavior.
-keep class com.onyangodatasolutions.bingwabot.** { *; }

# Keep Android framework lifecycle and accessibility-related classes if used by reflection.
-keepattributes *Annotation*
