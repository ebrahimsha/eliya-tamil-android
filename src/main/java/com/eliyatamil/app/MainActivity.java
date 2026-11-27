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
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String HOME = "https://eliyatamil.com/";
    private WebView webView;
    private ProgressBar progress;
    private TextToSpeech nativeTts;
    private boolean ttsReady = false;
    private String ttsStatus = "Initializing Android text-to-speech...";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        initializeNativeTts();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
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
        title.setText("Eliya Tamil"); title.setTextColor(Color.rgb(30,30,30)); title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-2,-2);
        titleParams.leftMargin = dp(10); header.addView(title,titleParams); root.addView(header);
        FrameLayout webContainer = new FrameLayout(this);
        root.addView(webContainer,new LinearLayout.LayoutParams(-1,0,1));
        webView = new WebView(this);
        progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(245,158,11)));
        webContainer.addView(webView,new FrameLayout.LayoutParams(-1,-1));
        webContainer.addView(progress,new FrameLayout.LayoutParams(-1,dp(3))); setContentView(root);
        WebSettings settings=webView.getSettings(); settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true); settings.setLoadsImagesAutomatically(true); settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(false); settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager cookies=CookieManager.getInstance(); cookies.setAcceptCookie(true); cookies.setAcceptThirdPartyCookies(webView,false);
        webView.addJavascriptInterface(new AndroidSpeechBridge(),"EliyaNativeSpeech");
        webView.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView v,int value){progress.setProgress(value);progress.setVisibility(value>=100?View.GONE:View.VISIBLE);}});
        webView.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){super.onPageFinished(view,url);
                String hide="(function(){var c=document.querySelectorAll('header,nav,.navbar,#header,#site-header,.site-header,[class*=header],[id*=header]');for(var i=0;i<c.length;i++){var e=c[i],r=e.getBoundingClientRect(),t=(e.innerText||e.textContent||'').toLowerCase();if(r.top<160&&r.height>25&&r.height<180&&(t.indexOf('eliya tamil')>=0||t.indexOf('எளிய தமிழ்')>=0)){e.style.setProperty('display','none','important');e.style.setProperty('visibility','hidden','important');}}})();";
                view.evaluateJavascript(hide,null);
                // Reinstall the speech shim after every navigation and retry while Android TTS initializes.
                view.evaluateJavascript(nativeSpeechShim(),null);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){Uri uri=request.getUrl();String host=uri.getHost()==null?"":uri.getHost().toLowerCase();if("https".equals(uri.getScheme())&&(host.equals("eliyatamil.com")||host.equals("www.eliyatamil.com")))return false;try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ignored){}return true;}
        });
        if(state!=null)webView.restoreState(state);else webView.loadUrl(HOME);
    }

    private void initializeNativeTts(){
        ttsStatus="Initializing Android TTS engine";
        nativeTts=new TextToSpeech(this,status->{
            if(status!=TextToSpeech.SUCCESS){ttsReady=false;ttsStatus="TTS engine initialization failed ("+status+")";injectTtsStatus();return;}
            int tamil=nativeTts.setLanguage(new Locale("ta","IN"));
            if(tamil==TextToSpeech.LANG_MISSING_DATA){ttsStatus="Tamil voice data missing. Install Tamil voice data in Android Text-to-speech settings.";}
            else if(tamil==TextToSpeech.LANG_NOT_SUPPORTED){ttsStatus="Tamil voice not supported by current TTS engine. Try Google Speech Services and install Tamil voice.";}
            else {ttsStatus="Tamil TTS ready";}
            ttsReady=true; // Engine initialized; each requested language is checked before speaking.
            nativeTts.setSpeechRate(0.82f);
            nativeTts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                @Override public void onStart(String id){notifySpeechEvent(id,"start");}
                @Override public void onDone(String id){notifySpeechEvent(id,"end");}
                @Override public void onError(String id){notifySpeechEvent(id,"error");}
                @Override public void onError(String id,int errorCode){notifySpeechEvent(id,"error");}
            });injectTtsStatus();
        });
    }
    private void injectTtsStatus(){if(webView!=null)runOnUiThread(()->{if(webView!=null)webView.evaluateJavascript("window.__eliyaNativeTtsStatus="+jsQuote(ttsStatus)+";window.dispatchEvent(new Event('eliya-tts-ready'));",null);});}
    private void notifySpeechEvent(String id,String event){if(webView==null)return;String js="window.__eliyaNativeSpeechEvent&&window.__eliyaNativeSpeechEvent("+jsQuote(id)+","+jsQuote(event)+");";runOnUiThread(()->{if(webView!=null)webView.evaluateJavascript(js,null);});}
    private String jsQuote(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","")+"\"";}

    private class AndroidSpeechBridge{
        @JavascriptInterface public boolean isReady(){return ttsReady;}
        @JavascriptInterface public String getStatus(){return ttsStatus;}
        @JavascriptInterface public void speak(String text,String language,String id){runOnUiThread(()->{
            if(nativeTts==null||text==null||text.trim().isEmpty()){notifySpeechEvent(id,"error");return;}
            if(!ttsReady){notifySpeechEvent(id,"error");return;}
            try{Locale locale=parseLocale(language);int result=nativeTts.setLanguage(locale);
                if(result==TextToSpeech.LANG_MISSING_DATA||result==TextToSpeech.LANG_NOT_SUPPORTED){
                    if(locale.getLanguage().equals("ta")){ttsStatus="Tamil voice data unavailable. Please install Tamil voice data.";injectTtsStatus();notifySpeechEvent(id,"error");return;}
                    nativeTts.setLanguage(Locale.ENGLISH);
                }
                nativeTts.speak(text,TextToSpeech.QUEUE_FLUSH,null,id);
            }catch(Exception ex){ttsStatus="TTS error: "+ex.getMessage();injectTtsStatus();notifySpeechEvent(id,"error");}
        });}
        @JavascriptInterface public void stop(){runOnUiThread(()->{if(nativeTts!=null)nativeTts.stop();});}
    }
    private Locale parseLocale(String language){if(language==null||language.trim().isEmpty())return new Locale("ta","IN");String n=language.replace('_','-').toLowerCase();if(n.equals("ta")||n.startsWith("ta-"))return new Locale("ta","IN");if(n.equals("en")||n.startsWith("en-"))return Locale.ENGLISH;return Locale.forLanguageTag(n);}
    private String nativeSpeechShim(){
        return "(function(){try{if(!window.EliyaNativeSpeech)return;var b=window.EliyaNativeSpeech;window.__eliyaNativeSpeechShimInstalled=true;var active={},count=0;"
        +"window.__eliyaNativeSpeechEvent=function(id,event){var u=active[id];if(!u)return;var e={type:'on'+event,target:u};if(event==='start'&&u.onstart)u.onstart(e);if(event==='end'){if(u.onend)u.onend(e);delete active[id]}if(event==='error'){if(u.onerror)u.onerror(e);delete active[id]}};"
        +"if(!window.SpeechSynthesisUtterance){window.SpeechSynthesisUtterance=function(text){this.text=text||'';this.lang='ta-IN';this.rate=1;this.pitch=1;this.volume=1;this.onstart=null;this.onend=null;this.onerror=null;}}"
        +"var shim={get speaking(){return Object.keys(active).length>0},get pending(){return false},get paused(){return false},speak:function(u){if(!u)return;var id='eliya-native-'+(++count);active[id]=u;var lang=u.lang||'ta-IN';b.speak(String(u.text||''),String(lang),id)},cancel:function(){b.stop();active={}},pause:function(){},resume:function(){},getVoices:function(){return []},onvoiceschanged:null};"
        +"try{Object.defineProperty(window,'speechSynthesis',{configurable:true,writable:true,value:shim})}catch(e){window.speechSynthesis=shim}window.__eliyaTtsBridgeStatus=function(){return b.getStatus?b.getStatus():(b.isReady&&b.isReady()?'Ready':'Not ready')};"
        +"}catch(err){console.warn('Eliya native TTS bridge error',err)}})();";
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    @Override protected void onSaveInstanceState(Bundle out){if(webView!=null)webView.saveState(out);super.onSaveInstanceState(out);}
    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(nativeTts!=null){nativeTts.stop();nativeTts.shutdown();}if(webView!=null){webView.stopLoading();webView.destroy();}super.onDestroy();}
}
