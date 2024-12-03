package com.example.aklatbayan;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aklatbayan.Recycler.Adapter;
import com.example.aklatbayan.Recycler.Model;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FavoriteFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar loadingIndicator;
    private TextView emptyView;
    private Adapter adapter;
    private ArrayList<Model> favoriteList;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_catalog, container, false);
        
        recyclerView = view.findViewById(R.id.rcv);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        emptyView = view.findViewById(R.id.emptyView);
        
        favoriteList = new ArrayList<>();
        adapter = new Adapter(requireContext(), favoriteList, true);
        
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = requireContext().getSharedPreferences("Favorites", requireContext().MODE_PRIVATE);
        
        loadFavoriteBooks();
        
        return view;
    }

    private void loadFavoriteBooks() {
        loadingIndicator.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        String favoriteBooks = sharedPreferences.getString("favoriteBooks", "");
        if (favoriteBooks.isEmpty()) {
            loadingIndicator.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        List<String> favoriteIds = new ArrayList<>(Arrays.asList(favoriteBooks.split(",")));

        firestore.collection("Books")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && isAdded()) {
                        favoriteList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bookId = document.getId();
                            if (favoriteIds.contains(bookId)) {
                                Model book = document.toObject(Model.class);
                                book.setId(bookId);
                                favoriteList.add(book);
                            }
                        }
                        
                        loadingIndicator.setVisibility(View.GONE);
                        if (favoriteList.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        loadingIndicator.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        Toast.makeText(requireContext(), "Error loading favorites", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavoriteBooks();
    }
}