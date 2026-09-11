package com.a14398138.fullscreenbrowser;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
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
    private static final String TAG = "FullscreenBrowser";
    private static final String DEFAULT_HOME_URL = "https://www.google.com";
    private WebView webView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            enterImmersiveMode();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to enter immersive mode", t);
        }

        try {
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
                    if (request != null && request.getUrl() != null) {
                        return !isWebUrl(request.getUrl().toString());
                    }
                    return false;
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return !isWebUrl(url);
                }
            });

            if (state != null) {
                webView.restoreState(state);
            }

            if (!loadFromIntent(getIntent()) && (webView.getUrl() == null || webView.getUrl().isEmpty())) {
                webView.loadUrl(DEFAULT_HOME_URL);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Initialization error", t);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!loadFromIntent(intent) && webView != null && (webView.getUrl() == null || webView.getUrl().isEmpty())) {
            webView.loadUrl(DEFAULT_HOME_URL);
        }
        try {
            enterImmersiveMode();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to enter immersive mode", t);
        }
    }

    private boolean loadFromIntent(Intent intent) {
        if (webView == null) return false;
        String url = findUrlInIntent(intent);
        if (url == null) return false;
        webView.loadUrl(url);
        return true;
    }

    private String findUrlInIntent(Intent intent) {
        if (intent == null) return null;

        try {
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
        } catch (Throwable t) {
            Log.e(TAG, "Error parsing intent", t);
        }

        return null;
    }

    private String extractWebUrl(CharSequence text) {
        if (text == null) return null;
        String textStr = text.toString().trim();

        if (isWebUrl(textStr)) {
            return textStr;
        }

        Matcher matcher = Patterns.WEB_URL.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group();
            if (isWebUrl(candidate)) {
                return candidate;
            } else if (candidate.startsWith("www.") || candidate.startsWith("http")) {
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
        try {
            String scheme = Uri.parse(value).getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) {
            try {
                webView.saveState(outState);
            } catch (Throwable t) {
                Log.e(TAG, "Error saving state", t);
            }
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            enterImmersiveMode();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to enter immersive mode in onResume", t);
        }
    }

    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                getWindow().setDecorFitsSystemWindows(false);
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } catch (Throwable t) {
                Log.e(TAG, "WindowInsetsController error", t);
            }
        } else {
            try {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            } catch (Throwable t) {
                Log.e(TAG, "SystemUiVisibility error", t);
            }
        }
    }
}
