# R8 rules for the optimised build. Room, DataStore, Compose and the other libraries ship their own rules.

# Keep readable stack traces in debug.log.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Libraries reference optional classes that are not in an Android build.
-dontwarn org.jetbrains.annotations.**
-dontwarn java.lang.management.**
