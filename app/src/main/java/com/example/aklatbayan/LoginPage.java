package com.example.aklatbayan;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

public class LoginPage extends AppCompatActivity {
    EditText etEmail, etPassword;
    Button btnLogin;
    ImageButton btnBack;
    private DBHelper dbHelper;
    ToggleButton passwordVisibilityToggle;
    private SessionManager sessionManager;
    private CheckBox keepSignedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomBar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        passwordVisibilityToggle = findViewById(R.id.tgl3);

        etEmail = findViewById(R.id.txtEmail2);
        etPassword = findViewById(R.id.txtPassword2);

        btnLogin = findViewById(R.id.btnLogin);
        btnBack = findViewById(R.id.btnBack);

        dbHelper = new DBHelper(this);
        sessionManager = new SessionManager(this);

        keepSignedIn = findViewById(R.id.checkBox);

        if (sessionManager.keepSignedIn()) {
            etEmail.setText(sessionManager.getEmail());
        }

        passwordVisibilityToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked){
                etPassword.setTransformationMethod(null);
            }
            else {
                etPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
        });
        etPassword.setSelection(etPassword.getText().length());

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String EMAIL = etEmail.getText().toString();
                String PASSWORD = etPassword.getText().toString();

                if (EMAIL.isEmpty() || PASSWORD.isEmpty()) {
                    Toast.makeText(LoginPage.this, "Please enter your email and password", Toast.LENGTH_SHORT).show();
                } else {
                    Boolean checkCredentials = dbHelper.checkUser(EMAIL, PASSWORD);
                    if (checkCredentials) {
                        try {
                            File booksDir = new File(getFilesDir(), "books");
                            if (!booksDir.exists()) {
                                booksDir.mkdirs();
                            }

                            String username = dbHelper.getUsername(EMAIL);
                            sessionManager.setLogin(true, username, EMAIL, keepSignedIn.isChecked());
                            
                            Toast.makeText(LoginPage.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), MainScreen.class);
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(LoginPage.this, "Error during login: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginPage.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }
}