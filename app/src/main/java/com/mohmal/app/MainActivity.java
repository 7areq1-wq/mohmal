package com.mohmal.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;

public class MainActivity extends Activity {

    private static final String HOME = "https://www.mohmal.com/ar";

    private static final String[] OWN_HOSTS = {
            "mohmal.com", "mohmal.in", "mohmal.im",
            "mailna.co", "mailna.in", "mailna.me", "mozej.com"
    };

    private static final String[] BLOCKED_HOSTS = {
            "googlesyndication.com", "doubleclick.net", "googleadservices.com",
            "adservice.google.com", "google-analytics.com", "googletagmanager.com",
            "googletagservices.com", "adsterra.com", "propellerads.com",
            "popads.net", "popcash.net", "exoclick.com", "juicyads.com",
            "clickadu.com", "hilltopads.net", "adnxs.com", "taboola.com",
            "outbrain.com", "criteo.com", "amazon-adsystem.com",
            "rubiconproject.com", "mc.yandex.ru", "an.yandex.ru",
            "scorecardresearch.com", "adform.net", "mgid.com",
            "revcontent.com", "monetag.com", "a-ads.com",
            "coinzilla.com", "bidvertiser.com"
    };

    private static final String HIDE_ADS_JS =
            "(function(){"
                    + "var css='ins.adsbygoogle,.adsbygoogle,[id^=\"google_ads\"],[id*=\"ad-container\"],"
                    + "[class*=\"ad-container\"],[class*=\"adsbox\"],[id^=\"aswift\"],"
                    + "iframe[src*=\"doubleclick\"],iframe[src*=\"googlesyndication\"],"
                    + "iframe[src*=\"/ads/\"]"
                    + "{display:none!important;height:0!important;visibility:hidden!important}';"
                    + "var s=document.createElement('style');s.innerHTML=css;"
                    + "(document.head||document.documentElement).appendChild(s);"
                    + "})();";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(false);
        ws.setSupportMultipleWindows(false);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                if (host != null && matches(host, BLOCKED_HOSTS)) {
                    return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (host != null && matches(host, OWN_HOSTS)) {
                    return false;
                }
                if (request.hasGesture()) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) { }
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                view.evaluateJavascript(HIDE_ADS_JS, null);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(HIDE_ADS_JS, null);
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(HOME);
        }
    }

    private static boolean matches(String host, String[] list) {
        host = host.toLowerCase();
        for (String d : list) {
            if (host.equals(d) || host.endsWith("." + d)) return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
