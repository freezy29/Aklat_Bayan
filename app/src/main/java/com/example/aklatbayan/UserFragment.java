package com.example.aklatbayan;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class UserFragment extends Fragment {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ImageView profileImg;
    private Button imgPick;
    private TextView userNameText;
    private SessionManager sessionManager;
    private final String[] tabTitles = new String[]{"Favorites", "History", "Settings"};
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private SharedPreferences sharedPreferences;
    private static final String PROFILE_IMAGE_PREF = "profile_image";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        sessionManager = new SessionManager(requireContext());
        sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Activity.MODE_PRIVATE);

        profileImg = view.findViewById(R.id.profileImage);
        imgPick = view.findViewById(R.id.imgPick);
        userNameText = view.findViewById(R.id.userNamebar);
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);

        userNameText.setText(sessionManager.getUsername());

        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        saveImage(imageUri);
                    }
                }
            }
        );

        imgPick.setOnClickListener(v -> openImagePicker());

        loadProfileImage();

        setupViewPager();
        
        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void saveImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);

            File directory = new File(requireContext().getFilesDir(), "profile_images");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }

            String fileName = sessionManager.getUsername() + "_profile.jpg";
            File file = new File(directory, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            Glide.get(requireContext()).clearMemory();
            new Thread(() -> {
                Glide.get(requireContext()).clearDiskCache();
            }).start();

            sharedPreferences.edit()
                    .putString(PROFILE_IMAGE_PREF, file.getAbsolutePath())
                    .apply();

            Glide.with(this)
                    .load(file)
                    .skipMemoryCache(true)
                    .circleCrop()
                    .placeholder(R.drawable.user_svgrepo_com)
                    .error(R.drawable.user_svgrepo_com)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(profileImg);

            Toast.makeText(requireContext(), "Profile image updated", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadProfileImage() {
        String imagePath = sharedPreferences.getString(PROFILE_IMAGE_PREF, null);
        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Glide.with(this)
                        .load(imageFile)
                        .skipMemoryCache(true)
                        .circleCrop()
                        .placeholder(R.drawable.user_svgrepo_com)
                        .error(R.drawable.user_svgrepo_com)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(profileImg);
            } else {
                sharedPreferences.edit().remove(PROFILE_IMAGE_PREF).apply();
                profileImg.setImageResource(R.drawable.user_svgrepo_com);
            }
        } else {
            profileImg.setImageResource(R.drawable.user_svgrepo_com);
        }
    }

    private void setupViewPager() {
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return new FavoriteFragment();
                    case 1:
                        return new HistoryFragment();
                    case 2:
                        return new AccountSettingsFragment();
                    default:
                        return new FavoriteFragment();
                }
            }

            @Override
            public int getItemCount() {
                return tabTitles.length;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();
    }
}