package com.bdixstream.app;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private WebChromeClient chromeClient;
    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(4);

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setAllowFileAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setBackgroundColor(Color.parseColor("#09090F"));
        webView.addJavascriptInterface(new BDIXNativeBridge(), "BDIXNative");

        webView.setWebViewClient(new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("appassets.androidplatform.net".equals(uri.getHost())) {
                    return false;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } catch (Exception ignored) {
                }
                return true;
            }
        });

        chromeClient = new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    onHideCustomView();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                fullscreenContainer.addView(customView,
                        new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT));
                fullscreenContainer.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                fullscreenContainer.removeView(customView);
                fullscreenContainer.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                }
                customView = null;
                customViewCallback = null;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        };
        webView.setWebChromeClient(chromeClient);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (customView != null) {
                    if (chromeClient != null) chromeClient.onHideCustomView();
                    return;
                }
                webView.evaluateJavascript(
                        "(function(){ return window.__handleBack ? window.__handleBack() : false; })()",
                        value -> {
                            if (!"true".equals(value)) {
                                if (webView.canGoBack()) {
                                    webView.goBack();
                                } else {
                                    finish();
                                }
                            }
                        }
                );
            }
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    @Override
    protected void onDestroy() {
        networkExecutor.shutdownNow();
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    /**
     * Native Android Bridge exposed to JS as window.BDIXNative.
     * Bypasses browser CORS & mixed-content restrictions for local BDIX FTP/HTTP servers.
     */
    private class BDIXNativeBridge {

        @JavascriptInterface
        public boolean isNative() {
            return true;
        }

        @JavascriptInterface
        public void fetchUrlAsync(final String requestId, final String targetUrl) {
            networkExecutor.execute(() -> {
                JSONObject res = new JSONObject();
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(targetUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(9000);
                    conn.setReadTimeout(12000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36");
                    conn.setRequestProperty("Accept", "text/html,application/json,*/*");

                    int status = conn.getResponseCode();
                    InputStream stream = (status >= 200 && status < 400)
                            ? conn.getInputStream()
                            : conn.getErrorStream();

                    StringBuilder sb = new StringBuilder();
                    if (stream != null) {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                            char[] buffer = new char[8192];
                            int total = 0;
                            int maxChars = 600_000;
                            int read;
                            while ((read = reader.read(buffer)) != -1 && total < maxChars) {
                                sb.append(buffer, 0, read);
                                total += read;
                            }
                        }
                    }

                    res.put("ok", status >= 200 && status < 400);
                    res.put("status", status);
                    res.put("finalUrl", conn.getURL().toString());
                    res.put("text", sb.toString());
                } catch (Exception e) {
                    try {
                        res.put("ok", false);
                        res.put("status", 0);
                        res.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
                    } catch (Exception ignored) {
                    }
                } finally {
                    if (conn != null) conn.disconnect();
                }

                final String payload = res.toString();
                runOnUiThread(() -> webView.evaluateJavascript(
                        "window.__bdixNativeCallback && window.__bdixNativeCallback("
                                + JSONObject.quote(requestId) + "," + payload + ");",
                        null
                ));
            });
        }

        @JavascriptInterface
        public void openExternalPlayer(final String streamUrl, final String title, final String packageName) {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(streamUrl), "video/*");
                intent.putExtra("title", title != null ? title : "BDIXStream");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (packageName != null && !packageName.isEmpty()) {
                    intent.setPackage(packageName);
                    try {
                        startActivity(intent);
                        return;
                    } catch (ActivityNotFoundException ignored) {
                        // Fall through to chooser if chosen player isn't installed
                        intent.setPackage(null);
                    }
                }
                try {
                    startActivity(Intent.createChooser(intent, "Play with external player"));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "No compatible video player found", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void openInBrowser(final String url) {
            runOnUiThread(() -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Could not open URL", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void copyText(final String text) {
            runOnUiThread(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("BDIXStream", text));
                    Toast.makeText(MainActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void showToast(final String msg) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
        }
    }
}
