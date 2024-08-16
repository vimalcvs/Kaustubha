package com.example.kaustubha;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ResetPassword extends AppCompatActivity {
    private EditText otpEditText, newPasswordEditText;
    ImageView back, hide;
    private Button verifyButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private boolean isPasswordVisible = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        otpEditText = findViewById(R.id.otp);
        newPasswordEditText = findViewById(R.id.phone);
        verifyButton = findViewById(R.id.button);
        hide = findViewById(R.id.hide);
        progressBar = findViewById(R.id.progressbar3);
        back = findViewById(R.id.back);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // Hide password
                    newPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    hide.setImageResource(R.drawable.visibilityoff);
                } else {
                    // Show password
                    newPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    hide.setImageResource(R.drawable.removepasswordhide);
                }
                // Move the cursor to the end
                newPasswordEditText.setSelection(newPasswordEditText.length());
                isPasswordVisible = !isPasswordVisible;
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResetPassword.this, ForgotPassword.class);
                startActivity(intent);
            }
        });

        verifyButton.setOnClickListener(v -> {
            String otp = otpEditText.getText().toString().trim();
            String verificationId = getIntent().getStringExtra("verificationId");
            String phoneNumber = getIntent().getStringExtra("phoneNumber");
            String newPassword = newPasswordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(otp) || TextUtils.isEmpty(newPassword)) {
                Toast.makeText(ResetPassword.this, "Please enter OTP and new password", Toast.LENGTH_SHORT).show();
                return;
            }

            resetPassword(verificationId, phoneNumber, otp, newPassword);
        });
    }

    private void resetPassword(String verificationId, String phoneNumber, String otp, String newPassword) {
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            updatePasswordInFirestore(phoneNumber, newPassword);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ResetPassword.this, "OTP verification failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updatePasswordInFirestore(String phoneNumber, String newPassword) {
        firestore.collection("Users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            String userId = document.getId();

                            firestore.collection("Users").document(userId)
                                    .update("password", newPassword)
                                    .addOnSuccessListener(aVoid -> {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(ResetPassword.this, "Password reset successful", Toast.LENGTH_SHORT).show();
                                        // Navigate to login activity
                                        startActivity(new Intent(ResetPassword.this, Login.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(ResetPassword.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ResetPassword.this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ResetPassword.this, "Error retrieving data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

}