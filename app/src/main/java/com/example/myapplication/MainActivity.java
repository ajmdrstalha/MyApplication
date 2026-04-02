package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONArray;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set Status Bar Color
        getWindow().setStatusBarColor(Color.parseColor("#1e3a8a"));
        
        WebView webView = new WebView(this);
        setContentView(webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidJSInterface(this), "AndroidInterface");

        webView.loadUrl("file:///android_asset/index.html");
    }

    public class AndroidJSInterface {
        private Context context;

        public AndroidJSInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void downloadPdf(String base64Data, String fileName) {
            try {
                byte[] pdfBytes = Base64.decode(base64Data, Base64.DEFAULT);
                File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(directory, fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(pdfBytes);
                }
                
                // Make file visible in Downloads folder immediately
                MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, new String[]{"application/pdf"}, null);

                runOnUiThread(() -> {
                    Toast.makeText(context, "PDF Saved to Downloads", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public String getSavedFiles() {
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            // Updated to list all PDFs in the Downloads folder as the "Report_" prefix is removed.
            // You can refine this filter if you want to be more specific.
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".pdf"));
            JSONArray results = new JSONArray();
            if (files != null) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                for (File file : files) {
                    results.put(file.getName());
                }
            }
            return results.toString();
        }

        @JavascriptInterface
        public void openPdfFile(String fileName) {
            try {
                File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(directory, fileName);
                Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
                
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}