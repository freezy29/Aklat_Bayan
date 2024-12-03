package com.example.aklatbayan;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aklatbayan.Recycler.Adapter;
import com.example.aklatbayan.Recycler.Model;

import java.io.File;
import java.util.ArrayList;

public class DownloadFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar loadingIndicator;
    private TextView emptyView;
    private TextView headerText;
    private Adapter adapter;
    private ArrayList<Model> downloadedBooks;
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_download, container, false);
        
        recyclerView = view.findViewById(R.id.rcv);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        emptyView = view.findViewById(R.id.emptyView);
        headerText = view.findViewById(R.id.textView15);
        
        downloadedBooks = new ArrayList<>();
        adapter = new Adapter(requireContext(), downloadedBooks, true);
        
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        loadDownloadedBooks();
        
        return view;
    }

    private void loadDownloadedBooks() {
        loadingIndicator.setVisibility(View.VISIBLE);
        File downloadDir = new File(requireContext().getFilesDir(), "books");
        
        if (!downloadDir.exists() || downloadDir.listFiles() == null || downloadDir.listFiles().length == 0) {
            loadingIndicator.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            headerText.setVisibility(View.GONE);
            return;
        }

        // Load books from local storage
        downloadedBooks.clear();
        File[] files = downloadDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".pdf")) {
                    String bookId = file.getName().replace(".pdf", "");
                    // Get book details from shared preferences
                    Model book = getBookDetails(bookId);
                    if (book != null) {
                        downloadedBooks.add(book);
                    }
                }
            }
        }

        loadingIndicator.setVisibility(View.GONE);
        if (downloadedBooks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            headerText.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            headerText.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private Model getBookDetails(String bookId) {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("downloaded_books", android.content.Context.MODE_PRIVATE);
        String bookJson = prefs.getString(bookId, null);
        if (bookJson != null) {
            return new com.google.gson.Gson().fromJson(bookJson, Model.class);
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDownloadedBooks();
    }

    private void showDeleteDialog(Model book) {
        Dialog deleteDialog = new Dialog(requireContext(), R.style.Dialog_style);
        deleteDialog.setContentView(R.layout.delete_dialog);
        deleteDialog.getWindow().setBackgroundDrawableResource(R.drawable.blue_popup);

        Button btnDelete = deleteDialog.findViewById(R.id.btnDelete);
        Button btnCancel = deleteDialog.findViewById(R.id.btnCancel);

        btnDelete.setOnClickListener(v -> {
            File bookFile = new File(requireContext().getFilesDir() + "/books/" + book.getId() + ".pdf");
            if (bookFile.exists() && bookFile.delete()) {
                android.content.SharedPreferences prefs = requireContext().getSharedPreferences("downloaded_books", android.content.Context.MODE_PRIVATE);
                prefs.edit().remove(book.getId()).apply();
                
                Toast.makeText(requireContext(), "Book deleted successfully", Toast.LENGTH_SHORT).show();
                loadDownloadedBooks();
            }
            deleteDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> deleteDialog.dismiss());
        deleteDialog.show();
    }
}