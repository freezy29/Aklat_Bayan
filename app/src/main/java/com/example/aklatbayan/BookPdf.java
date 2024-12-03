package com.example.aklatbayan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aklatbayan.databinding.ActivityBookPdfBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BookPdf extends AppCompatActivity {

    ActivityBookPdfBinding binding;
    private String bookId;
    private int totalPages = 0;
    private int currentPage = 0;
    private SharedPreferences sharedPreferences;
    private static final String READING_PROGRESS_PREF = "ReadingProgress";
    private TextView txtBookTitle;
    private TextView txtPageNumber;
    ImageButton btnBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookPdfBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        btnBack = findViewById(R.id.btnBack);
        txtBookTitle = findViewById(R.id.txtBookTitle);
        txtPageNumber = findViewById(R.id.txtPageNumber);

        sharedPreferences = getSharedPreferences(READING_PROGRESS_PREF, MODE_PRIVATE);
        
        String pdfLink = getIntent().getStringExtra("pdfLink");
        bookId = getIntent().getStringExtra("id");
        String bookTitle = getIntent().getStringExtra("title");

        txtBookTitle.setText(bookTitle != null ? bookTitle : "Book Reader");

        btnBack.setOnClickListener(v -> onBackPressed());

        if (bookId != null) {
            loadReadingProgress();
            File bookFile = new File(getFilesDir() + "/books/" + bookId + ".pdf");
            if (bookFile.exists()) {
                loadOfflinePdf(bookFile);
                return;
            }
        }

        if (pdfLink != null && !pdfLink.isEmpty()) {
            loadOnlinePdf(pdfLink);
        } else {
            Toast.makeText(this, "Error: PDF not available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadReadingProgress() {
        if (bookId != null) {
            currentPage = sharedPreferences.getInt(bookId + "_page", 0);
            totalPages = sharedPreferences.getInt(bookId + "_total", 0);
            float progress = sharedPreferences.getFloat(bookId + "_progress", 0f);
            updatePageNumber();
        }
    }

    private void saveReadingProgress() {
        if (bookId != null && totalPages > 0) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(bookId + "_page", currentPage);
            editor.putInt(bookId + "_total", totalPages);
            float progressPercentage = ((float) currentPage / totalPages) * 100;
            editor.putFloat(bookId + "_progress", progressPercentage);
            editor.apply();
        }
    }

    private void loadOfflinePdf(File pdfFile) {
        try {
            binding.pdfView.fromFile(pdfFile)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(currentPage)
                    .onPageChange((page, pageCount) -> {
                        currentPage = page;
                        totalPages = pageCount;
                        updatePageNumber();
                        saveReadingProgress();
                    })
                    .onLoad(nbPages -> {
                        totalPages = nbPages;
                        currentPage = 0;
                        updatePageNumber();
                        binding.progressBar.setVisibility(View.GONE);
                    })
                    .onError(throwable -> {
                        Toast.makeText(BookPdf.this, 
                            "Error loading PDF: " + throwable.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                        binding.progressBar.setVisibility(View.GONE);
                    })
                    .load();
        } catch (Exception e) {
            Toast.makeText(this, "Error loading PDF file", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            finish();
        }
    }

    private void updatePageNumber() {
        if (totalPages > 0) {
            runOnUiThread(() -> {
                txtPageNumber.setText(String.format("%d/%d", currentPage + 1, totalPages));
                txtPageNumber.setVisibility(View.VISIBLE);
            });
        } else {
            txtPageNumber.setVisibility(View.GONE);
        }
    }

    private void loadOnlinePdf(String pdfLink) {
        binding.progressBar.setVisibility(View.VISIBLE);
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(pdfLink)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(BookPdf.this, 
                            "Error loading PDF: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                        binding.progressBar.setVisibility(View.GONE);
                        finish();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(BookPdf.this, 
                                "Error: Could not load PDF", 
                                Toast.LENGTH_SHORT).show();
                            binding.progressBar.setVisibility(View.GONE);
                            finish();
                        });
                        return;
                    }

                    try {
                        InputStream inputStream = response.body().byteStream();
                        runOnUiThread(() -> {
                            binding.pdfView.fromStream(inputStream)
                                    .enableSwipe(true)
                                    .swipeHorizontal(false)
                                    .enableDoubletap(true)
                                    .defaultPage(currentPage)
                                    .onPageChange((page, pageCount) -> {
                                        currentPage = page;
                                        totalPages = pageCount;
                                        updatePageNumber();
                                        saveReadingProgress();
                                    })
                                    .onLoad(nbPages -> {
                                        totalPages = nbPages;
                                        currentPage = 0;
                                        updatePageNumber();
                                        binding.progressBar.setVisibility(View.GONE);
                                    })
                                    .onError(throwable -> {
                                        Toast.makeText(BookPdf.this, 
                                            "Error loading PDF: " + throwable.getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                        binding.progressBar.setVisibility(View.GONE);
                                    })
                                    .load();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(BookPdf.this, 
                                "Error processing PDF", 
                                Toast.LENGTH_SHORT).show();
                            binding.progressBar.setVisibility(View.GONE);
                            finish();
                        });
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error setting up PDF download", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveReadingProgress();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveReadingProgress();
    }
}