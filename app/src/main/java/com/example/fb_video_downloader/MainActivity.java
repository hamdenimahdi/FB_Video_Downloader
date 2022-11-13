package com.example.fb_video_downloader;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;
import static android.content.ContentValues.TAG;
import static com.example.fb_video_downloader.Utils.RootDirectoryFacebook;
import static com.example.fb_video_downloader.Utils.createFileFolder;
import static com.example.fb_video_downloader.Utils.startDownload;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.example.fb_video_downloader.databinding.ActivityMainBinding;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    MainActivity activity;
    private String videoUrl;
    private ClipboardManager clipBoard;
    private String strName = "facebook";
    private String strNameSecond = "fb";
//    InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        activity = this;

        createFileFolder();
        initViews();
        InterstitialAdsINIT();

    }

    @Override
    protected void onResume() {
        super.onResume();
        activity = this;
        assert activity != null;
        clipBoard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
        pasteText();
    }

    private void initViews() {
        clipBoard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);

        binding.tvPaste.setOnClickListener(view -> {
            pasteText();
        });

        binding.loginBtn1.setOnClickListener(v -> {
            String ll = binding.etText.getText().toString();
            if (ll.equals("")) {
                Utils.setToast(activity, getResources().getString(R.string.enter_url));
            } else if (!Patterns.WEB_URL.matcher(ll).matches()) {
                Utils.setToast(activity, getResources().getString(R.string.enter_valid_url));
            } else {
                getFacebookData();
//                showInterstitialAds();
            }

//            showInterstitialAds();
        });

    }


    private void getFacebookData() {
        try {
            createFileFolder();
            URL url = new URL(binding.etText.getText().toString());
            String host = url.getHost();
            if (host.contains(strName) || host.contains(strNameSecond)) {
                Utils.showProgressDialog(activity);
                new CallGetFacebookData().execute(binding.etText.getText().toString());
//                showInterstitialAds();
            } else {
                Utils.setToast(activity, getResources().getString(R.string.enter_valid_url));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pasteText() {
        try {
            binding.etText.setText("");
            String copyIntent = getIntent().getStringExtra("CopyIntent");
//            copyIntent=MainActivity.extractLinks(copyIntent);
            if (copyIntent == null || copyIntent.equals("")) {
                if (!(clipBoard.hasPrimaryClip())) {
                    Log.d(TAG, "PasteText");
                } else if (!(clipBoard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN))) {
                    if (clipBoard.getPrimaryClip().getItemAt(0).getText().toString().contains(strName)) {
                        binding.etText.setText(clipBoard.getPrimaryClip().getItemAt(0).getText().toString());
                    } else if (clipBoard.getPrimaryClip().getItemAt(0).getText().toString().contains(strNameSecond)) {
                        binding.etText.setText(clipBoard.getPrimaryClip().getItemAt(0).getText().toString());
                    }

                } else {
                    ClipData.Item item = clipBoard.getPrimaryClip().getItemAt(0);
                    if (item.getText().toString().contains(strName)) {
                        binding.etText.setText(item.getText().toString());
                    } else if (item.getText().toString().contains(strNameSecond)) {
                        binding.etText.setText(item.getText().toString());
                    }

                }
            } else {
                if (copyIntent.contains(strName)) {
                    binding.etText.setText(copyIntent);
                } else if (copyIntent.contains(strNameSecond)) {
                    binding.etText.setText(copyIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class CallGetFacebookData extends AsyncTask<String, Void, Document> {
        Document facebookDoc;

        @Override
        protected Document doInBackground(String... urls) {
            try {
                facebookDoc = Jsoup.connect(urls[0]).get();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "doInBackground: Error");
            }
            return facebookDoc;
        }

        @Override
        protected void onPostExecute(Document result) {
            Utils.hideProgressDialog(activity);
            try {
                videoUrl = result.select("meta[property=\"og:video\"]").last().attr("content");
                if (!videoUrl.equals("")) {
                    startDownload(videoUrl, RootDirectoryFacebook, activity, "facebook_" + System.currentTimeMillis() + ".mp4");
                    videoUrl = "";
//                    showInterstitialAds();
                    binding.etText.setText("");
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == 100 && resultCode == RESULT_OK) {
                getFacebookUserData();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void getFacebookUserData() {
        AndroidNetworking.post("https://www.facebook.com/api/graphql/")
                .addHeaders("accept-language", "en,en-US;q=0.9,fr;q=0.8,ar;q=0.7")
                .addHeaders(
                        "user-agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36"
                )
                .addHeaders("Content-Type", "application/json")
                .addBodyParameter(
                        "variables",
                        "{\"bucketsCount\":200,\"initialBucketID\":null,\"pinnedIDs\":[\"\"],\"scale\":3}"
                )
                .addBodyParameter("doc_id", "2893638314007950")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println("JsonResp- " + response);
                    }

                    @Override
                    public void onError(ANError anError) {

                    }
                });
    }

    public void InterstitialAdsINIT() {

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });


/*      mInterstitialAd = new com.google.android.gms.ads.InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.admob_interstitial_ad));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());*/

    }


/*    private void showInterstitialAds() {
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }*/
}

