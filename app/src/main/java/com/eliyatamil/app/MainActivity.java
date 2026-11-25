package com.eliyatamil.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.graphics.Color;
import android.graphics.Typeface;

public class MainActivity extends Activity {
    private static final String HOME = "https://eliyatamil.com/";
    private WebView webView;
    private ProgressBar progress;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // Fixed native header: remains visible while website content scrolls.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(5), dp(12), dp(5));
        header.setBackgroundColor(Color.WHITE);
        header.setElevation(dp(3));
        header.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(58)));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.eliya_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(logo, new LinearLayout.LayoutParams(dp(46), dp(46)));

        TextView title = new TextView(this);
        title.setText("Eliya Tamil");
        title.setTextColor(Color.rgb(30, 30, 30));
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-2, -2);
        titleParams.leftMargin = dp(10);
        header.addView(title, titleParams);

        root.addView(header);
        FrameLayout webContainer = new FrameLayout(this);
        root.addView(webContainer, new LinearLayout.LayoutParams(-1, 0, 1));
        webView = new WebView(this);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(245,158,11)));
        webContainer.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        webContainer.addView(progress, new FrameLayout.LayoutParams(-1, dp(3)));
        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Hide the website's own top branding header inside the app only.
                // The native Android header remains visible; bottom navigation is untouched.
                String js = "(function(){"
                    + "var candidates=document.querySelectorAll('header, nav, .navbar, #header, #site-header, .site-header, [class*=header], [id*=header]');"
                    + "for(var i=0;i<candidates.length;i++){"
                    + "var e=candidates[i],r=e.getBoundingClientRect(),t=(e.innerText||e.textContent||'').toLowerCase();"
                    + "if(r.top<160 && r.height>25 && r.height<180 && (t.indexOf('eliya tamil')>=0 || t.indexOf('எளிய தமிழ்')>=0)){"
                    + "e.style.setProperty('display','none','important');e.style.setProperty('visibility','hidden','important');"
                    + "}}})();";
                view.evaluateJavascript(js, null);
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
                if ("https".equals(uri.getScheme()) &&
                    (host.equals("eliyatamil.com") || host.equals("www.eliyatamil.com"))) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) {}
                return true;
            }
        });
        if (state != null) webView.restoreState(state); else webView.loadUrl(HOME);
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    @Override protected void onSaveInstanceState(Bundle outState) { webView.saveState(outState); super.onSaveInstanceState(outState); }
    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { if (webView != null) { webView.stopLoading(); webView.destroy(); } super.onDestroy(); }
}
