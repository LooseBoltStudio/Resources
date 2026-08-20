package com.dkrus.resourcesapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String APP_ASSET_PREFIX = "https://appassets.androidplatform.net/assets/";
    private static final String REMOTE_SCHEDULE_URL = "https://raw.githubusercontent.com/LooseBoltStudio/Resources/main/work-resources/schedules/beaumont-assignment-schedule/jobs.csv";
    private static final String RELEASE_API_URL = "https://api.github.com/repos/LooseBoltStudio/Resources/releases/latest";

    private WebView webView;

    private class AppBridge {
        @JavascriptInterface
        public void setTextZoom(final int percent) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (webView != null) webView.getSettings().setTextZoom(percent);
                }
            });
        }

        @JavascriptInterface
        public void shareText(final String title, final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_SUBJECT, title);
                    share.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(share, "Share from Railroad Resources"));
                }
            });
        }
    }

    private WebResourceResponse remoteResponse(String url, String mimeType) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", "Railroad-Resources-Android/1.2");
            if ("application/json".equals(mimeType)) {
                connection.setRequestProperty("Accept", "application/vnd.github+json");
            } else {
                connection.setRequestProperty("Accept", "text/csv,text/plain;q=0.9,*/*;q=0.8");
            }

            int status = connection.getResponseCode();
            InputStream source = status >= 200 && status < 400
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            if (source == null) throw new IOException("Remote request returned " + status);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int count;
            while ((count = source.read(chunk)) != -1) buffer.write(chunk, 0, count);
            source.close();

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Cache-Control", "no-store, no-cache, must-revalidate");
            headers.put("Pragma", "no-cache");

            String reason = connection.getResponseMessage();
            if (reason == null || reason.length() == 0) reason = status >= 200 && status < 400 ? "OK" : "Remote Error";
            return new WebResourceResponse(
                    mimeType,
                    "UTF-8",
                    status,
                    reason,
                    headers,
                    new ByteArrayInputStream(buffer.toByteArray())
            );
        } catch (Exception error) {
            Log.e("ResourcesWeb", "Remote request failed: " + url, error);
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Cache-Control", "no-store");
            byte[] message = ("Remote request failed: " + error.getMessage()).getBytes(StandardCharsets.UTF_8);
            return new WebResourceResponse(
                    "text/plain",
                    "UTF-8",
                    502,
                    "Bad Gateway",
                    headers,
                    new ByteArrayInputStream(message)
            );
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        webView.addJavascriptInterface(new AppBridge(), "RailroadNative");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("https://appassets.androidplatform.net/")) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Proxy the two live GitHub calls through native networking. This avoids
                // WebView cross-origin/CORS failures while keeping the UI itself offline-first.
                if (REMOTE_SCHEDULE_URL.equals(url)) return remoteResponse(url, "text/csv");
                if (RELEASE_API_URL.equals(url)) return remoteResponse(url, "application/json");

                if (!url.startsWith(APP_ASSET_PREFIX)) return null;
                String path = request.getUrl().getPath();
                if (path.startsWith("/assets/")) path = path.substring(8);
                try {
                    InputStream stream;
                    try {
                        stream = getAssets().open(path);
                    } catch (IOException firstError) {
                        stream = getAssets().open(path.replace("/", "\\"));
                    }
                    String mime = "application/octet-stream";
                    if (path.endsWith(".html")) mime = "text/html";
                    else if (path.endsWith(".js") || path.endsWith(".jsx") || path.endsWith(".mjs")) mime = "text/javascript";
                    else if (path.endsWith(".css")) mime = "text/css";
                    else if (path.endsWith(".csv")) mime = "text/csv";
                    else if (path.endsWith(".json")) mime = "application/json";
                    else if (path.endsWith(".pdf")) mime = "application/pdf";
                    else if (path.endsWith(".wasm")) mime = "application/wasm";
                    return new WebResourceResponse(mime, "UTF-8", stream);
                } catch (IOException error) {
                    Log.e("ResourcesWeb", "Missing asset: " + path, error);
                    return null;
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage message) {
                Log.e("ResourcesWeb", message.message() + " @ "
                        + message.sourceId() + ":" + message.lineNumber());
                return true;
            }
        });
        setContentView(webView);
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
            return;
        }
        webView.evaluateJavascript(
                "(window.railroadBack && window.railroadBack()) === true",
                new android.webkit.ValueCallback<String>() {
                @Override
                public void onReceiveValue(String handled) {
                    if (!"true".equals(handled)) {
                        if (webView.canGoBack()) webView.goBack();
                        else MainActivity.super.onBackPressed();
                    }
                }
                });
    }
}
