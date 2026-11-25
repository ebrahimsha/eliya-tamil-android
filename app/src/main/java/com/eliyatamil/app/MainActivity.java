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
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String HOME = "https://eliyatamil.com/";
    private WebView webView;
    private ProgressBar progress;
    private TextToSpeech nativeTts;
    private boolean ttsReady = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        initializeNativeTts();

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
        webView.addJavascriptInterface(new AndroidSpeechBridge(), "EliyaNativeSpeech");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Hide website branding header inside the app only; the native header stays visible.
                String hideHeaderJs = "(function(){"
                    + "var candidates=document.querySelectorAll('header, nav, .navbar, #header, #site-header, .site-header, [class*=header], [id*=header]');"
                    + "for(var i=0;i<candidates.length;i++){"
                    + "var e=candidates[i],r=e.getBoundingClientRect(),t=(e.innerText||e.textContent||'').toLowerCase();"
                    + "if(r.top<160 && r.height>25 && r.height<180 && (t.indexOf('eliya tamil')>=0 || t.indexOf('எளிய தமிழ்')>=0)){"
                    + "e.style.setProperty('display','none','important');e.style.setProperty('visibility','hidden','important');"
                    + "}}})();";
                view.evaluateJavascript(hideHeaderJs, null);
                // Route browser-generated speech through Android's native TTS engine.
                view.evaluateJavascript(nativeSpeechShim(), null);
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

    private void initializeNativeTts() {
        nativeTts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true;
                // Prefer Tamil when available; individual requests can switch language.
                int result = nativeTts.setLanguage(new Locale("ta", "IN"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    nativeTts.setLanguage(Locale.getDefault());
                }
                nativeTts.setSpeechRate(0.90f);
                nativeTts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                    @Override public void onStart(String id) { notifySpeechEvent(id, "start"); }
                    @Override public void onDone(String id) { notifySpeechEvent(id, "end"); }
                    @Override public void onError(String id) { notifySpeechEvent(id, "error"); }
                    @Override public void onError(String id, int errorCode) { notifySpeechEvent(id, "error"); }
                });
            }
        });
    }

    private void notifySpeechEvent(String utteranceId, String event) {
        if (webView == null) return;
        final String js = "window.__eliyaNativeSpeechEvent && window.__eliyaNativeSpeechEvent(" +
                JSONObjectQuote(utteranceId) + "," + JSONObjectQuote(event) + ");";
        runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(js, null); });
    }

    private String JSONObjectQuote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'";
    }

    private class AndroidSpeechBridge {
        @JavascriptInterface public void speak(String text, String language, String id) {
            runOnUiThread(() -> {
                if (!ttsReady || nativeTts == null || text == null || text.trim().isEmpty()) {
                    notifySpeechEvent(id, "error"); return;
                }
                try {
                    Locale locale = parseLocale(language);
                    int languageResult = nativeTts.setLanguage(locale);
                    if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Fall back to Tamil for Tamil-learning content.
                        nativeTts.setLanguage(new Locale("ta", "IN"));
                    }
                    nativeTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id);
                } catch (Exception e) { notifySpeechEvent(id, "error"); }
            });
        }
        @JavascriptInterface public void stop() {
            runOnUiThread(() -> { if (nativeTts != null) nativeTts.stop(); });
        }
    }

    private Locale parseLocale(String language) {
        if (language == null || language.trim().isEmpty()) return new Locale("ta", "IN");
        String normalized = language.replace('_', '-');
        if (normalized.equalsIgnoreCase("ta") || normalized.toLowerCase().startsWith("ta-")) return new Locale("ta", "IN");
        if (normalized.equalsIgnoreCase("en") || normalized.toLowerCase().startsWith("en-")) return Locale.ENGLISH;
        return Locale.forLanguageTag(normalized);
    }

    private String nativeSpeechShim() {
        return "(function(){try{if(!window.EliyaNativeSpeech||window.__eliyaSpeechShimInstalled)return;window.__eliyaSpeechShimInstalled=true;"
            + "var nativeBridge=window.EliyaNativeSpeech,active={},counter=0;"
            + "window.__eliyaNativeSpeechEvent=function(id,event){var u=active[id];if(!u)return;var e;try{e=new Event('on'+event)}catch(x){e={type:'on'+event}};"
            + "if(event==='start'&&u.onstart)u.onstart(e);if(event==='end'){if(u.onend)u.onend(e);delete active[id]}if(event==='error'){if(u.onerror)u.onerror(e);delete active[id]}};"
            + "var shim={get speaking(){return Object.keys(active).length>0},get pending(){return false},get paused(){return false},"
            + "speak:function(u){if(!u)return;var id='eliya-'+(++counter);active[id]=u;nativeBridge.speak(String(u.text||''),String(u.lang||'ta-IN'),id)},"
            + "cancel:function(){nativeBridge.stop();active={}},pause:function(){},resume:function(){},getVoices:function(){return []},onvoiceschanged:null};"
            + "try{Object.defineProperty(window,'speechSynthesis',{configurable:true,value:shim})}catch(e){window.speechSynthesis=shim}"
            + "}catch(err){console.log('Eliya native speech bridge:',err)}})();";
    }

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    @Override protected void onSaveInstanceState(Bundle outState) { webView.saveState(outState); super.onSaveInstanceState(outState); }
    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() {
        if (nativeTts != null) { nativeTts.stop(); nativeTts.shutdown(); }
        if (webView != null) { webView.stopLoading(); webView.destroy(); }
        super.onDestroy();
    }
}
