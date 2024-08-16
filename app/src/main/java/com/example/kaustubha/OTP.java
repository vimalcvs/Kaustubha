package com.example.kaustubha;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class OTP extends AppCompatActivity {

    private boolean doubleBackToExitPressedOnce = false;
    String phoneNumber;
    EditText otp;
    Button next;
    ProgressBar progressBar;
    TextView resend;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore firestore;
    Long timeoutSeconds = 60L;
    private String mVerificationId;
    private String mPassword;
    private String namefull;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private boolean isDoctorSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        String userId = getIntent().getStringExtra("userId");

        otp = findViewById(R.id.code);
        next = findViewById(R.id.verify);
        progressBar = findViewById(R.id.progressbar1);
        resend = findViewById(R.id.resend);
        firestore = FirebaseFirestore.getInstance();

        mVerificationId = getIntent().getStringExtra("verificationId");
        mResendToken = getIntent().getParcelableExtra("resendToken");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        mPassword = getIntent().getStringExtra("password");
        namefull = getIntent().getStringExtra("name");
        isDoctorSelected = getIntent().getBooleanExtra("isDoctorSelected", false);

        sendOtp(phoneNumber, false);

        TextView textMobile = findViewById(R.id.textview5);
        textMobile.setText(String.format(" %s ", phoneNumber));

        next.setOnClickListener(v -> {
            String code = otp.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(OTP.this, "Enter verification code", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyPhoneNumberWithCode(mVerificationId, code, phoneNumber, mPassword);
        });


        resend.setOnClickListener((v)->{
            sendOtp(phoneNumber, true);
        });
    }

    void sendOtp(String phoneNumber, boolean isResend) {
        startResendTimer();
        setInProgress(true);
        PhoneAuthOptions.Builder builder =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                setInProgress(false);
                                storeUserData();
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Toast.makeText(OTP.this, "OTP verification failed " + e , Toast.LENGTH_SHORT).show();
                                setInProgress(false);
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                super.onCodeSent(verificationId, forceResendingToken);
                                mVerificationId = verificationId;
                                mResendToken = forceResendingToken;
                                Toast.makeText(OTP.this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                                setInProgress(false);
                            }
                        });
        if (isResend) {
            PhoneAuthProvider.verifyPhoneNumber(builder.setForceResendingToken(mResendToken).build());
        } else {
            PhoneAuthProvider.verifyPhoneNumber(builder.build());
        }
    }

    void setInProgress(boolean inProgress) {
        if (!inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            next.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            next.setVisibility(View.VISIBLE);
        }
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code,String phoneNumber, String password) {
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        storeUserData();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(OTP.this, "Verification failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void storeUserData() {
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> userData = new HashMap<>();
        userData.put("phoneNumber", phoneNumber);
        userData.put("name", namefull);
        userData.put("password", mPassword);
        userData.put("userId", userId);

        String collection = isDoctorSelected ? "Doctors" : "Users";
        firestore.collection(collection).document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(OTP.this, "Sign-up successful", Toast.LENGTH_SHORT).show();
                    // Navigate to login activity
                    startActivity(new Intent(OTP.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(OTP.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    void startResendTimer() {
        resend.setEnabled(false);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timeoutSeconds--;
                runOnUiThread(()-> {
                    resend.setText("Resend OTP in " + timeoutSeconds + " seconds");
                });
                if (timeoutSeconds <= 0 ) {
                    timeoutSeconds = 60L;
                    timer.cancel();
                    runOnUiThread(() -> {
                        resend.setText("Resend Now");
                        resend.setEnabled(true);
                    });
                }
            }
        },0, 1000);
    }
}