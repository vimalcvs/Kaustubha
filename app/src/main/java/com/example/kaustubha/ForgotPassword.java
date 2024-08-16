package com.example.kaustubha;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class ForgotPassword extends AppCompatActivity {
    EditText txtEmail, phonen;
    Button mreset;
    ImageView back;
    ProgressBar progressBar;
    String email, phone;
    FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        txtEmail = findViewById(R.id.email);
        phonen = findViewById(R.id.phone);
        mreset = findViewById(R.id.button);
        progressBar = findViewById(R.id.progressbar3);
        back = findViewById(R.id.back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ForgotPassword.this, Login.class);
                startActivity(intent);
            }
        });

        mreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }

    void validateData(){
        email = txtEmail.getText().toString();
        phone = phonen.getText().toString();
        if (email.isEmpty()){
            if (phone.isEmpty()) {
                txtEmail.setError("Required");
                phonen.setError("Required");
            }
            else if (phone.length() == 10) {
                if (!phone.startsWith("+")) {
                    // Assuming the phone number is entered without country code, e.g., "1234567890"
                    phone = "+91" + phone; // You can dynamically set the country code or ask the user to input it
                }
                sendVerificationCode(phone);
            }
            else {
                Toast.makeText(this, "Check Phone Number", Toast.LENGTH_SHORT).show();
            }
        }else {
            if (phone.length() == 10) {
                Toast.makeText(this, "Enter either email or phone number only", Toast.LENGTH_SHORT).show();
            }
            else {
                forgetPass();
            }
        }
    }

    private void sendVerificationCode(String phoneNumber) {
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(ForgotPassword.this)                 // Activity (for callback binding)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ForgotPassword.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        // Code sent, navigate to OTP verification activity
                        progressBar.setVisibility(View.GONE);
                        Intent intent = new Intent(ForgotPassword.this, ResetPassword.class);
                        intent.putExtra("verificationId", verificationId);
                        intent.putExtra("phoneNumber", phoneNumber);
                        startActivity(intent);
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }
    void forgetPass(){
        progressBar.setVisibility(View.VISIBLE);
        auth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Toast.makeText(ForgotPassword.this,"Check your Email",Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ForgotPassword.this, Login.class));
                    finish();
                }else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ForgotPassword.this,"Error : " + task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}