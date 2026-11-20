# Keep JavaScript Bridge methods
-keepclassmembers class com.bdixstream.app.MainActivity$BDIXNativeBridge {
    @android.webkit.JavascriptInterface <methods>;
}
