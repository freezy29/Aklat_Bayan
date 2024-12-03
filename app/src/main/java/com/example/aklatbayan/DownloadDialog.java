package com.example.aklatbayan;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aklatbayan.Recycler.Model;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadDialog extends AppCompatActivity {
    private String bookId;
    private String downloadUrl;
    private String title;
    private String author;
    private String desc;
    private String category;
    private String thumbnailUrl;
    private FirebaseFirestore firestore;
    private ProgressBar downloadProgress;
    private Button btnDownloadFull;
    private Button btnCancel;
    private Call currentCall;
    private boolean isDownloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_dialog);

        firestore = FirebaseFirestore.getInstance();

        downloadProgress = findViewById(R.id.downloadProgress);
        btnDownloadFull = findViewById(R.id.btnDownloadFull);
        btnCancel = findViewById(R.id.btnCancel);

        bookId = getIntent().getStringExtra("bookId");
        downloadUrl = getIntent().getStringExtra("downloadUrl");
        title = getIntent().getStringExtra("title");
        author = getIntent().getStringExtra("author");
        desc = getIntent().getStringExtra("desc");
        category = getIntent().getStringExtra("category");
        thumbnailUrl = getIntent().getStringExtra("thumbnailUrl");

        btnDownloadFull.setOnClickListener(v -> {
            if (!isDownloading && !isBookDownloaded(bookId)) {
                downloadBook();
            } else if (isBookDownloaded(bookId)) {
                Toast.makeText(DownloadDialog.this, "Book already downloaded", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (isDownloading && currentCall != null) {
                currentCall.cancel();
            }
            finish();
        });
    }

    private void downloadBook() {
        isDownloading = true;
        downloadProgress.setVisibility(View.VISIBLE);
        btnDownloadFull.setEnabled(false);
        Toast.makeText(this, "Starting download...", Toast.LENGTH_SHORT).show();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        currentCall = client.newCall(request);

        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (call.isCanceled()) {
                        Toast.makeText(DownloadDialog.this, "Download cancelled", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DownloadDialog.this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    isDownloading = false;
                    downloadProgress.setVisibility(View.GONE);
                    btnDownloadFull.setEnabled(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(DownloadDialog.this, "Download failed", Toast.LENGTH_SHORT).show();
                        isDownloading = false;
                        downloadProgress.setVisibility(View.GONE);
                        btnDownloadFull.setEnabled(true);
                    });
                    return;
                }

                InputStream inputStream = response.body().byteStream();
                File bookFile = new File(getBookDirectory(), bookId + ".pdf");
                
                FileOutputStream outputStream = new FileOutputStream(bookFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (currentCall.isCanceled()) {
                        outputStream.close();
                        inputStream.close();
                        bookFile.delete();
                        return;
                    }
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                outputStream.close();
                inputStream.close();

                Model downloadedBook = new Model(bookId, title, author, desc, 
                    category, bookFile.getAbsolutePath(), downloadUrl, thumbnailUrl);
                
                saveDownloadedBook(downloadedBook);

                runOnUiThread(() -> {
                    Toast.makeText(DownloadDialog.this, "Download completed", Toast.LENGTH_SHORT).show();
                    isDownloading = false;
                    downloadProgress.setVisibility(View.GONE);
                    finish();
                });
            }
        });
    }

    private File getBookDirectory() {
        File directory = new File(getFilesDir(), "books");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    private boolean isBookDownloaded(String bookId) {
        File bookFile = new File(getBookDirectory(), bookId + ".pdf");
        return bookFile.exists();
    }

    private void saveDownloadedBook(Model book) {
        firestore.collection("downloads")
                .document(book.getId())
                .set(book)
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isDownloading && currentCall != null) {
            currentCall.cancel();
        }
    }
}