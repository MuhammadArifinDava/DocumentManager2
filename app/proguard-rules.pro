# Jangan obfuscate semua model dan kelas yang perlu untuk Firestore
-keep class com.epic.documentmanager.models.** { *; }
-keepclassmembers class com.epic.documentmanager.models.** { *; }

# (opsional aman) keep adapter parcelize/serializable
-keep class ** implements java.io.Serializable { *; }

# (opsional) Firebase/Play Services (umumnya tidak wajib, tapi aman)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
# ==== SLF4J (hilangkan warning saat R8) ====
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.**

