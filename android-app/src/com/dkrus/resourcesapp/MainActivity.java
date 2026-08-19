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
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {
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
                String prefix = "https://appassets.androidplatform.net/assets/";
                if (!url.startsWith(prefix)) return null;
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
