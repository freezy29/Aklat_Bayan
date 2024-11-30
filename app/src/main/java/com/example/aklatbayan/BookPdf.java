package com.example.aklatbayan;

import android.os.Bundle;
import android.view.View;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BookPdf extends AppCompatActivity {

    ActivityBookPdfBinding binding;
    private FirebaseFirestore firestore;
    private String bookId;
    private int totalPages = 0;
    private int currentPage = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookPdfBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestore = FirebaseFirestore.getInstance();
        String pdfLink = getIntent().getStringExtra("pdfLink");
        bookId = getIntent().getStringExtra("id");

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
        firestore.collection("reading_progress")
                .document(bookId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentPage = documentSnapshot.getLong("currentPage").intValue();
                    }
                });
    }

    private void saveReadingProgress() {
        if (bookId != null && totalPages > 0) {
            Map<String, Object> progress = new HashMap<>();
            progress.put("currentPage", currentPage);
            progress.put("totalPages", totalPages);
            progress.put("progress", (float) currentPage / totalPages * 100);

            firestore.collection("reading_progress")
                    .document(bookId)
                    .set(progress)
                    .addOnFailureListener(e -> 
                        Toast.makeText(this, "Failed to save progress", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadOfflinePdf(File pdfFile) {
        binding.pdfView.fromFile(pdfFile)
                .defaultPage(currentPage)
                .onPageChange((page, pageCount) -> {
                    currentPage = page;
                    totalPages = pageCount;
                    saveReadingProgress();
                })
                .onLoad(nbPages -> {
                    totalPages = nbPages;
                    binding.progressBar.setVisibility(View.GONE);
                })
                .onError(throwable -> {
                    Toast.makeText(this, "Error loading PDF: " + throwable.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.GONE);
                })
                .load();
    }

    private void loadOnlinePdf(String pdfLink) {
        binding.progressBar.setVisibility(View.VISIBLE);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(pdfLink).build();

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

                InputStream inputStream = response.body().byteStream();
                runOnUiThread(() -> {
                    binding.pdfView.fromStream(inputStream)
                            .defaultPage(currentPage)
                            .onPageChange((page, pageCount) -> {
                                currentPage = page;
                                totalPages = pageCount;
                                saveReadingProgress();
                            })
                            .onLoad(nbPages -> {
                                totalPages = nbPages;
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
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveReadingProgress();
    }
}