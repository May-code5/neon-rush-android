# Keep app classes
-keep class com.maycode.neonrush.** { *; }

# Unity Ads
-keep class com.unity3d.** { *; }
-keep interface com.unity3d.** { *; }
-dontwarn com.unity3d.**

# Google Play Billing
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Keep generic signatures
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
