package com.test.myapplication;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {


    private final String URL = "https://navsegda.net/";
    WebView webView;

    MyWebChromeClient myWebChromeClient = new MyWebChromeClient();
    SharedPreferences prefs;

    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageL;
    private final int FILECHOOSER_RESULT_CODE = 1;


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(myWebChromeClient);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        prefs = this.getSharedPreferences(this.getPackageName(),
                Activity.MODE_PRIVATE);
        String s = prefs.getString("lastUrl", "");
        if(s.equals("")) {
            webView.loadUrl(s);
        }
        else
            {
            webView.loadUrl(URL);
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = this.
                getSharedPreferences(this.getPackageName(), Activity.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("lastUrl",webView.getUrl());
        edit.apply();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }


    private class MyWebChromeClient extends WebChromeClient {

        // Для API Android >= 21 (ОС 5.0)
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

            if (uploadMessageL != null)
            {
                uploadMessageL.onReceiveValue(null);
                uploadMessageL = null;
            }

            uploadMessageL = filePathCallback;
            Intent intent = fileChooserParams.createIntent();
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            try {
                startActivityForResult(Intent.createChooser(intent, "File Chooser"), FILECHOOSER_RESULT_CODE);
            } catch (ActivityNotFoundException e) {
                uploadMessageL = null;
                return false;
            }
            return true;
        }
        // Для API Android <11 (ОС 3.0)
        protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
            uploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_RESULT_CODE);
        }

        //For Android 4.1 only
        protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture)
        {
            uploadMessage = uploadMsg;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "File Browser"), FILECHOOSER_RESULT_CODE);
        }

        protected void openFileChooser(ValueCallback<Uri> uploadMsg) {
            uploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULT_CODE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==FILECHOOSER_RESULT_CODE)
        {
            if(null == uploadMessage && null == uploadMessageL)
            {
                return;
            }
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (uploadMessageL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (uploadMessage != null) {
                uploadMessage.onReceiveValue(result);
                uploadMessage = null;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
        if (requestCode != FILECHOOSER_RESULT_CODE || uploadMessageL == null)
            return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        uploadMessageL.onReceiveValue(results);
        uploadMessageL = null;
    }

}