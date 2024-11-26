package com.example.aklatbayan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.aklatbayan.databinding.ActivityBookDetailsBinding;

public class book_details extends AppCompatActivity {
    Button btnBack;
    ToggleButton btnFave;
    ActivityBookDetailsBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        String description = getIntent().getStringExtra("desc");
        String category = getIntent().getStringExtra("category");
        String downloadUrl = getIntent().getStringExtra("downloadUrl");
        String pdfLink = getIntent().getStringExtra("pdfLink");
        String thumbnail = getIntent().getStringExtra("thumbnailUrl");

        String title = getIntent().getStringExtra("txtTitle");
        Glide.with(this).load(thumbnail).into(binding.detailThumbnail);

        binding.txtTitle.setText(title);
        binding.txtCategory.setText(category);
        binding.txtDescription.setText(description);

        btnBack = findViewById(R.id.btnBack);
        btnFave = findViewById(R.id.tglOFF);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(book_details.this, Homepage.class);
                startActivity(myIntent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });
        btnFave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }
}
