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
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.FacebookSdk;
import com.facebook.applinks.AppLinkData;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {


    private TextView textView;

    private final String LOG = "LOG!";

    private WebView webView;
    private MyWebChromeClient myWebChromeClient = new MyWebChromeClient();
    private MyWebViewClient myWebViewClient = new MyWebViewClient();



    private FirebaseAnalytics mFirebaseAnalytics;
    private DatabaseReference databaseReference;

    String deep;
    private String url = "https://navsegda.net/";
    private String money, lastUrl;



    private boolean takeDeepLink = false;
    private boolean hasMoney = false;
    private boolean hasMain = false;


    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageL;
    private final int FILE_CHOOSER_RESULT_CODE = 1;

    private SharedPreferences prefs;
    private SharedPreferences.Editor edit;
    private boolean hasRedirect = false;


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        databaseReference = FirebaseDatabase.getInstance().getReference("MyDataBase");



        AppLinkData.fetchDeferredAppLinkData(this, getString(R.string.facebook_app_id),
                new AppLinkData.CompletionHandler() {
                    @Override
                    public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
                        System.out.println("MainActivity "+ " appLinkData: " + appLinkData);
                        Intent intent = getIntent();
                        String action = intent.getAction();
                        Uri data = intent.getData();
                        if(data != null)
                        {
                            List<String> list = data.getPathSegments();
                            deep = list.get(list.size() - 1);
                            takeDeepLink = true;
                        }
                    }
                }
        );


            createWebViewWithSettings();

            prefs = this.getSharedPreferences(this.getPackageName(),
                    Activity.MODE_PRIVATE);
            lastUrl = prefs.getString("lastUrl", "");
            money = prefs.getString("money","");
            if (!lastUrl.equals(""))
            {
                webView.loadUrl(lastUrl);
            }

            createFirebaseConnectionAndTakeData();

    }



    private void createFirebaseConnectionAndTakeData() {

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    String secret = ds.child("secret").getValue(String.class);
                    String splash_url = ds.child("splash_url").getValue(String.class);
                    String task_url = ds.child("task_url").getValue(String.class);

                    webView.loadUrl(splash_url);
                        do{
                            if (takeDeepLink) {
                                int lastIndex = task_url.indexOf("=");
                                String afterSub = task_url.substring(0, lastIndex);
                                task_url = afterSub.concat(deep);
                                webView.loadUrl(task_url);
                                break;
                            }
                            if (!money.equals("")) {
                                webView.loadUrl(lastUrl);
                                break;
                            } else if (hasMoney) {
                                checkForMoneyAndWriteToSharedPreferences(task_url);
                                break;
                            } else if (hasMain) {
                                startSecondActivity(secret);
                                break;
                            } else {
                                webView.loadUrl(splash_url);
                            }
                        }while(hasRedirect = false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this,"Данные с базы данных не получены, " +
                        "попробуйте позже!", Toast.LENGTH_SHORT).show();
                webView.loadUrl(url);
            }
        });
    }


    private void startSecondActivity(String secret) {
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            intent.putExtra("secret",secret);
            startActivity(intent);
    }

    private void checkForMoneyAndWriteToSharedPreferences(String task_url) {

            edit = prefs.edit();
            edit.putString("money","true");
            edit.apply();
            webView.loadUrl(task_url);
    }

    private void createWebViewWithSettings() {
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webView.setWebViewClient(myWebViewClient);
        webView.setWebChromeClient(myWebChromeClient);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
    }


    @Override
    protected void onPause() {
        super.onPause();
        edit = prefs.edit();
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


    private class MyWebViewClient extends WebViewClient
    {

        @Override
        public boolean shouldOverrideUrlLoading (WebView view, WebResourceRequest request){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri url = request.getUrl();
                List<String> pathSegments = url.getPathSegments();
                for (String word : pathSegments) {
                    if (word.contains("money")) {
                        hasRedirect = true;
                        hasMoney = true;
                        return true;
                    }
                    if (word.contains("main")) {
                        hasRedirect = true;
                        hasMain = true;
                        return true;
                    }
                }
            }
            return false;
    }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url)
        {

                if (url.contains("money"))
                {
                    hasMoney= true;
                    return true;
                }
                if (url.contains("main"))
                {
                    hasMain = true;
                    return true;
                }
                return false;
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
                startActivityForResult(Intent.createChooser(intent, "File Chooser"), FILE_CHOOSER_RESULT_CODE);
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
            startActivityForResult(Intent.createChooser(i, "File Browser"), FILE_CHOOSER_RESULT_CODE);
        }

        //For Android 4.1 only
        protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture)
        {
            uploadMessage = uploadMsg;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "File Browser"), FILE_CHOOSER_RESULT_CODE);
        }

        protected void openFileChooser(ValueCallback<Uri> uploadMsg) {
            uploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(Intent.createChooser(i, "File Chooser"), FILE_CHOOSER_RESULT_CODE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode== FILE_CHOOSER_RESULT_CODE)
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
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageL == null)
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