package com.lordgem15.spellmaster;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private WebView webView;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        tts = new TextToSpeech(this, this);

        webView.addJavascriptInterface(new AndroidTTSBridge(), "AndroidTTS");

        webView.loadUrl("https://macaron.im/share/699066d80a458a105ff58247?pwd=8RWV");
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            ttsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED);
        } else {
            ttsReady = false;
        }
    }

    private void speakInternal(final String text) {
        if (!ttsReady || text == null) {
            notifyDone();
            return;
        }

        final String utteranceId = UUID.randomUUID().toString();
        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override
            public void onStart(String id) { }

            @Override
            public void onDone(String id) {
                if (utteranceId.equals(id)) {
                    notifyDone();
                }
            }

            @Override
            public void onError(String id) {
                if (utteranceId.equals(id)) {
                    notifyDone();
                }
            }
        });

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    private void notifyDone() {
        runOnUiThread(() -> webView.evaluateJavascript(
                "window.__androidTtsDone && window.__androidTtsDone();",
                null
        ));
    }

    private class AndroidTTSBridge {
        @JavascriptInterface
        public boolean isReady() {
            return ttsReady;
        }

        @JavascriptInterface
        public void speak(String text) {
            speakInternal(text);
        }

        @JavascriptInterface
        public void stop() {
            if (tts != null) tts.stop();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
