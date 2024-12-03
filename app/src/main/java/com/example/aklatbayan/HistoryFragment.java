package com.example.aklatbayan;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.Calendar;
import android.text.format.DateUtils;
import android.content.SharedPreferences;

public class HistoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar loadingIndicator;
    private TextView emptyView;
    private Adapter adapter;
    private ArrayList<Model> historyList;
    private static final String HISTORY_PREFS = "reading_history_prefs";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        
        recyclerView = view.findViewById(R.id.rcv);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        emptyView = view.findViewById(R.id.emptyView);
        
        historyList = new ArrayList<>();
        adapter = new Adapter(requireContext(), historyList, true);
        
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        loadHistory();
        
        return view;
    }

    private void loadHistory() {
        SharedPreferences historyPrefs = requireContext().getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE);
        String historyOrder = historyPrefs.getString("history_order", "");
        
        historyList.clear();
        
        if (!historyOrder.isEmpty()) {
            String[] bookIds = historyOrder.split(",");
            String currentLabel = null;
            
            for (String bookId : bookIds) {
                long timestamp = historyPrefs.getLong(bookId + "_timestamp", 0);
                String timeLabel = getTimeLabel(timestamp);

                if (!timeLabel.equals(currentLabel)) {
                    Model header = new Model();
                    header.setTitle(timeLabel);
                    header.setId("header_" + timestamp);
                    historyList.add(header);
                    currentLabel = timeLabel;
                }

                Model book = new Model(
                    bookId,
                    historyPrefs.getString(bookId + "_title", ""),
                    historyPrefs.getString(bookId + "_author", ""),
                    historyPrefs.getString(bookId + "_desc", ""),
                    historyPrefs.getString(bookId + "_category", ""),
                    historyPrefs.getString(bookId + "_pdfLink", ""),
                    historyPrefs.getString(bookId + "_downloadUrl", ""),
                    historyPrefs.getString(bookId + "_thumbnailUrl", "")
                );
                book.setTimestamp(timestamp);
                historyList.add(book);
            }
        }
        
        if (historyList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
        
        adapter.notifyDataSetChanged();
    }

    public void clearHistory() {
        SharedPreferences historyPrefs = requireContext().getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE);
        historyPrefs.edit().clear().apply();
        loadHistory();
    }

    private String getTimeLabel(long timestamp) {
        if (DateUtils.isToday(timestamp)) {
            return "Today";
        } else if (isYesterday(timestamp)) {
            return "Yesterday";
        } else if (isThisWeek(timestamp)) {
            return "This Week";
        } else if (isLastWeek(timestamp)) {
            return "Last Week";
        } else if (isThisMonth(timestamp)) {
            return "This Month";
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return android.text.format.DateFormat.format("MMMM yyyy", cal).toString();
        }
    }

    private boolean isYesterday(long timestamp) {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);
        yesterday.set(Calendar.MILLISECOND, 0);
        
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timestamp);
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        
        return yesterday.getTimeInMillis() == date.getTimeInMillis();
    }

    private boolean isThisWeek(long timestamp) {
        Calendar now = Calendar.getInstance();
        Calendar timeToCheck = Calendar.getInstance();
        timeToCheck.setTimeInMillis(timestamp);
        
        return now.get(Calendar.YEAR) == timeToCheck.get(Calendar.YEAR) 
            && now.get(Calendar.WEEK_OF_YEAR) == timeToCheck.get(Calendar.WEEK_OF_YEAR);
    }

    private boolean isLastWeek(long timestamp) {
        Calendar now = Calendar.getInstance();
        Calendar timeToCheck = Calendar.getInstance();
        timeToCheck.setTimeInMillis(timestamp);
        
        now.add(Calendar.WEEK_OF_YEAR, -1);
        return now.get(Calendar.YEAR) == timeToCheck.get(Calendar.YEAR) 
            && now.get(Calendar.WEEK_OF_YEAR) == timeToCheck.get(Calendar.WEEK_OF_YEAR);
    }

    private boolean isThisMonth(long timestamp) {
        Calendar now = Calendar.getInstance();
        Calendar timeToCheck = Calendar.getInstance();
        timeToCheck.setTimeInMillis(timestamp);
        
        return now.get(Calendar.YEAR) == timeToCheck.get(Calendar.YEAR) 
            && now.get(Calendar.MONTH) == timeToCheck.get(Calendar.MONTH);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }
}
