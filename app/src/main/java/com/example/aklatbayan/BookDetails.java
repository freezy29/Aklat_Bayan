package com.example.aklatbayan;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ToggleButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.aklatbayan.Recycler.Model;
import com.example.aklatbayan.databinding.ActivityBookDetailsBinding;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookDetails extends AppCompatActivity {
    ImageButton btnBack, btnDownload;
    ToggleButton btnFave;
    Button btnRead, btnCancel, btnDownloadFull;
    ActivityBookDetailsBinding binding;
    Dialog dialog;
    private FirebaseFirestore firestore;
    private Call currentCall;
    private boolean isDownloading = false;
    private ProgressBar downloadProgress;
    private String downloadUrl;
    private String pdfLink;
    private ProgressBar bookReadingProgress;
    private SharedPreferences sharedPreferences;
    private String favoriteBooks;
    private static final String HISTORY_COLLECTION = "reading_history";
    private static final String HISTORY_PREFS = "reading_history_prefs";
    private static final String HISTORY_KEY = "history_list";
    private SharedPreferences historyPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String description = getIntent().getStringExtra("desc");
        String category = getIntent().getStringExtra("category");
        downloadUrl = getIntent().getStringExtra("downloadUrl");
        pdfLink = getIntent().getStringExtra("pdfLink");
        String thumbnail = getIntent().getStringExtra("thumbnailUrl");

        String title = getIntent().getStringExtra("txtTitle");
        String author = getIntent().getStringExtra("author");
        Glide.with(this).load(thumbnail).into(binding.detailThumbnail);

        binding.txtTitle.setText(title);
        binding.txtCategory.setText("Category: " + category);
        binding.txtDescription.setText(description);
        binding.txtAuthor.setText("By " + author);

        btnRead = findViewById(R.id.btnRead);
        btnBack = findViewById(R.id.btnBack);
        btnFave = findViewById(R.id.btnFave);
        btnDownload = findViewById(R.id.btnDownload);
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            btnDownload.setVisibility(View.GONE);
        } else {
            btnDownload.setVisibility(View.VISIBLE);
        }
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        btnFave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDownloadDialog();
            }
        });
        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToHistory();
                Intent intent = new Intent(BookDetails.this, BookPdf.class);
                intent.putExtra("pdfLink", pdfLink);
                intent.putExtra("id", getIntent().getStringExtra("id"));
                intent.putExtra("title", getIntent().getStringExtra("txtTitle"));
                startActivity(intent);
            }
        });

        firestore = FirebaseFirestore.getInstance();
        downloadUrl = getIntent().getStringExtra("downloadUrl");
        pdfLink = getIntent().getStringExtra("pdfLink");

        bookReadingProgress = findViewById(R.id.bookReadingProgress);
        String bookId = getIntent().getStringExtra("id");

        if (bookId != null) {
            loadReadingProgress(bookId);
        }

        if (isBookDownloaded(getIntent().getStringExtra("id"))) {
            btnDownload.setImageResource(R.drawable.baseline_delete_24);
            btnDownload.setOnClickListener(v -> showDeleteDialog());
        } else {
            btnDownload.setImageResource(R.drawable.baseline_download_48);
            btnDownload.setOnClickListener(v -> showDownloadDialog());
        }

        sharedPreferences = getSharedPreferences("Favorites", MODE_PRIVATE);
        favoriteBooks = sharedPreferences.getString("favoriteBooks", "");

        btnFave.setChecked(favoriteBooks.contains(bookId));

        btnFave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bookId = getIntent().getStringExtra("id");
                String currentFavorites = sharedPreferences.getString("favoriteBooks", "");
                
                if (btnFave.isChecked()) {
                    if (currentFavorites.isEmpty()) {
                        currentFavorites = bookId;
                    } else if (!currentFavorites.contains(bookId)) {
                        currentFavorites += "," + bookId;
                    }
                    Toast.makeText(BookDetails.this, "Added to favorites", Toast.LENGTH_SHORT).show();
                } else {
                    String[] favorites = currentFavorites.split(",");
                    StringBuilder newFavorites = new StringBuilder();
                    for (String id : favorites) {
                        if (!id.equals(bookId)) {
                            if (newFavorites.length() > 0) {
                                newFavorites.append(",");
                            }
                            newFavorites.append(id);
                        }
                    }
                    currentFavorites = newFavorites.toString();
                    Toast.makeText(BookDetails.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("favoriteBooks", currentFavorites);
                editor.apply();
                
                favoriteBooks = currentFavorites;
            }
        });

        historyPrefs = getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE);
    }

    private void showDownloadDialog() {
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            Toast.makeText(this, "Download not available for this book", Toast.LENGTH_SHORT).show();
            return;
        }

        dialog = new Dialog(this, R.style.Dialog_style);
        dialog.setContentView(R.layout.activity_download_dialog);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.blue_popup);
        
        btnCancel = dialog.findViewById(R.id.btnCancel);
        btnDownloadFull = dialog.findViewById(R.id.btnDownloadFull);
        downloadProgress = dialog.findViewById(R.id.downloadProgress);
        
        String bookId = getIntent().getStringExtra("id");
        
        btnDownloadFull.setOnClickListener(v -> {
            if (!isDownloading && !isBookDownloaded(bookId)) {
                downloadProgress.setVisibility(View.VISIBLE);
                btnDownloadFull.setEnabled(false);
                btnDownloadFull.setText("Downloading...");
                downloadBook(bookId);
            } else if (isBookDownloaded(bookId)) {
                Toast.makeText(this, "Book already downloaded", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (isDownloading && currentCall != null) {
                currentCall.cancel();
            }
            dialog.dismiss();
        });

        dialog.setOnDismissListener(dialogInterface -> {
            if (isDownloading && currentCall != null) {
                currentCall.cancel();
            }
        });

        dialog.show();
    }

    private void downloadBook(String bookId) {
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            Toast.makeText(this, "Download URL not found", Toast.LENGTH_SHORT).show();
            isDownloading = false;
            downloadProgress.setVisibility(View.GONE);
            dialog.dismiss();
            return;
        }

        isDownloading = true;
        downloadProgress.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Starting download...", Toast.LENGTH_SHORT).show();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        currentCall = client.newCall(request);

        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    String errorMessage;
                    if (call.isCanceled()) {
                        errorMessage = "Download cancelled";
                    } else if (e.getMessage().contains("No address associated with hostname")) {
                        errorMessage = "Download URL is invalid";
                    } else {
                        errorMessage = "Download failed: " + e.getMessage();
                    }
                    Toast.makeText(BookDetails.this, errorMessage, Toast.LENGTH_SHORT).show();
                    isDownloading = false;
                    downloadProgress.setVisibility(View.GONE);
                    btnDownloadFull.setEnabled(true);
                    btnDownloadFull.setText("Download");
                    dialog.dismiss();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(BookDetails.this, "Download failed", Toast.LENGTH_SHORT).show();
                        isDownloading = false;
                        downloadProgress.setVisibility(View.GONE);
                        btnDownloadFull.setEnabled(true);
                        btnDownloadFull.setText("Download");
                        dialog.dismiss();
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

                Model downloadedBook = new Model(bookId, 
                    getIntent().getStringExtra("txtTitle"),
                    getIntent().getStringExtra("author"),
                    getIntent().getStringExtra("desc"),
                    getIntent().getStringExtra("category"),
                    bookFile.getAbsolutePath(), 
                    downloadUrl,
                    getIntent().getStringExtra("thumbnailUrl"));
                
                saveDownloadedBook(downloadedBook);

                runOnUiThread(() -> {
                    Toast.makeText(BookDetails.this, "Download completed", Toast.LENGTH_SHORT).show();
                    isDownloading = false;
                    downloadProgress.setVisibility(View.GONE);
                    btnDownloadFull.setEnabled(true);
                    btnDownloadFull.setText("Download");
                    dialog.dismiss();
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
        android.content.SharedPreferences prefs = getSharedPreferences("downloaded_books", Context.MODE_PRIVATE);
        String bookJson = new com.google.gson.Gson().toJson(book);
        prefs.edit().putString(book.getId(), bookJson).apply();
    }

    private void loadReadingProgress(String bookId) {
        SharedPreferences readingProgress = getSharedPreferences("ReadingProgress", Context.MODE_PRIVATE);
        float progress = readingProgress.getFloat(bookId + "_progress", 0f);
        
        if (progress > 0) {
            bookReadingProgress.setProgress((int) progress);
            bookReadingProgress.setVisibility(View.VISIBLE);
        } else {
            bookReadingProgress.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String bookId = getIntent().getStringExtra("id");
        if (bookId != null) {
            loadReadingProgress(bookId);
        }
    }

    private void showDeleteDialog() {
        Dialog deleteDialog = new Dialog(this, R.style.Dialog_style);
        deleteDialog.setContentView(R.layout.delete_dialog);
        deleteDialog.getWindow().setBackgroundDrawableResource(R.drawable.blue_popup);

        Button btnDelete = deleteDialog.findViewById(R.id.btnDelete);
        Button btnCancel = deleteDialog.findViewById(R.id.btnCancel);
        String bookId = getIntent().getStringExtra("id");

        btnDelete.setOnClickListener(v -> {
            if (bookId != null) {
                File bookFile = new File(getBookDirectory(), bookId + ".pdf");
                if (bookFile.exists() && bookFile.delete()) {
                    firestore.collection("downloads")
                            .document(bookId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Book deleted successfully", Toast.LENGTH_SHORT).show();
                                btnDownload.setVisibility(View.VISIBLE);
                            })
                            .addOnFailureListener(e -> 
                                Toast.makeText(this, "Error updating database", Toast.LENGTH_SHORT).show());
                }
            }
            deleteDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> deleteDialog.dismiss());
        deleteDialog.show();
    }

    private void saveToHistory() {
        String bookId = getIntent().getStringExtra("id");
        if (bookId == null) return;

        SharedPreferences historyPrefs = getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = historyPrefs.edit();

        editor.putString(bookId + "_title", getIntent().getStringExtra("txtTitle"));
        editor.putString(bookId + "_author", getIntent().getStringExtra("author"));
        editor.putString(bookId + "_desc", getIntent().getStringExtra("desc"));
        editor.putString(bookId + "_category", getIntent().getStringExtra("category"));
        editor.putString(bookId + "_pdfLink", getIntent().getStringExtra("pdfLink"));
        editor.putString(bookId + "_downloadUrl", getIntent().getStringExtra("downloadUrl"));
        editor.putString(bookId + "_thumbnailUrl", getIntent().getStringExtra("thumbnailUrl"));
        editor.putLong(bookId + "_timestamp", System.currentTimeMillis());

        String historyOrder = historyPrefs.getString("history_order", "");
        List<String> historyIds = new ArrayList<>();
        if (!historyOrder.isEmpty()) {
            historyIds = new ArrayList<>(Arrays.asList(historyOrder.split(",")));
        }

        historyIds.remove(bookId);
        historyIds.add(0, bookId);

        if (historyIds.size() > 50) {
            String removedId = historyIds.remove(historyIds.size() - 1);
            editor.remove(removedId + "_title");
            editor.remove(removedId + "_author");
            editor.remove(removedId + "_desc");
            editor.remove(removedId + "_category");
            editor.remove(removedId + "_pdfLink");
            editor.remove(removedId + "_downloadUrl");
            editor.remove(removedId + "_thumbnailUrl");
            editor.remove(removedId + "_timestamp");
        }

        editor.putString("history_order", TextUtils.join(",", historyIds));
        editor.apply();
    }
}
