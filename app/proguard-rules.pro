# Exercise Timer R8 rules.
#
# Most of the app needs nothing here: Hilt, Compose, DataStore, coroutines and Obsidian ship
# consumer rules, and AAPT keeps every Activity, Service and BroadcastReceiver named in the
# manifest. Add a rule only for code reached by reflection.

# Readable crash stack traces (map them back with build/outputs/mapping/release/mapping.txt).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Notification extras and settings intents use framework constants only; nothing to keep.
