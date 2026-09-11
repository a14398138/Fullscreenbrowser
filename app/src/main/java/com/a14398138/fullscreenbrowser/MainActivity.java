package com.a14398138.fullscreenbrowser;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.ArrayList;
import java.util.regex.Matcher;

public class MainActivity extends Activity {
    private static final String DEFAULT_HOME_URL = "https://www.google.com";
    private WebView webView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        enterImmersiveMode();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !isWebUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !isWebUrl(url);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    this::navigateBack);
        }

        if (state != null) {
            webView.restoreState(state);
        }

        if (!loadFromIntent(getIntent()) && webView.getUrl() == null) {
            // Default to Google search if opened directly with no URL
            webView.loadUrl(DEFAULT_HOME_URL);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!loadFromIntent(intent) && webView.getUrl() == null) {
            webView.loadUrl(DEFAULT_HOME_URL);
        }
        enterImmersiveMode();
    }

    private boolean loadFromIntent(Intent intent) {
        String url = findUrlInIntent(intent);
        if (url == null) return false;
        webView.loadUrl(url);
        return true;
    }

    private String findUrlInIntent(Intent intent) {
        if (intent == null) return null;

        // 1. Direct ACTION_VIEW data
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            String dataUrl = intent.getData().toString();
            if (isWebUrl(dataUrl)) return dataUrl;
        }

        // 2. Extra Text
        String url = extractWebUrl(intent.getCharSequenceExtra(Intent.EXTRA_TEXT));
        if (url != null) return url;

        // 3. Process Text
        url = extractWebUrl(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT));
        if (url != null) return url;

        // 4. Array of texts
        ArrayList<CharSequence> texts = intent.getCharSequenceArrayListExtra(Intent.EXTRA_TEXT);
        if (texts != null) {
            for (CharSequence text : texts) {
                url = extractWebUrl(text);
                if (url != null) return url;
            }
        }

        // 5. ClipData
        ClipData clipData = intent.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                url = extractWebUrl(item.coerceToText(this));
                if (url != null) return url;
                if (item.getUri() != null && isWebUrl(item.getUri().toString())) {
                    return item.getUri().toString();
                }
            }
        }

        return null;
    }

    private String extractWebUrl(CharSequence text) {
        if (text == null) return null;
        String textStr = text.toString().trim();

        // Check if the whole string is an http/https URL
        if (isWebUrl(textStr)) {
            return textStr;
        }

        // Search for URL pattern within shared text (e.g. from social apps)
        Matcher matcher = Patterns.WEB_URL.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group();
            if (isWebUrl(candidate)) {
                return candidate;
            } else if (candidate.startsWith("www.") || candidate.startsWith("http")) {
                // If scheme was missing or lowercase issue
                if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) {
                    return "https://" + candidate;
                }
                return candidate;
            }
        }

        return null;
    }

    private boolean isWebUrl(String value) {
        if (value == null) return false;
        String scheme = Uri.parse(value).getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private void navigateBack() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        navigateBack();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) {
            webView.saveState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveMode();
    }

    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }
}
