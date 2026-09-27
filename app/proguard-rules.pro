# Nestling keeps everything on device; nothing here is reflective except Room + Glance,
# both of which ship their own consumer rules. These keeps are belt-and-braces so a
# release build can never strip the data layer that owns the parent's log.

-keep class com.nestling.baby.data.local.** { *; }
-keepclassmembers class com.nestling.baby.data.local.** { *; }

# Glance app widget receiver is referenced from the manifest only.
-keep class com.nestling.baby.widget.** { *; }

# Foreground timer service, also manifest-only.
-keep class com.nestling.baby.timer.TimerService { *; }
