package com.example.kaustubha;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** @noinspection deprecation, deprecation, deprecation, deprecation, deprecation */
public class Register extends AppCompatActivity {
    private boolean doubleBackToExitPressedOnce = false;
    ImageView user, doctor, googleBtn, hide;
    EditText mailorphone, fullname, pass;
    Button login;
    ProgressBar progressBar;
    FirebaseAuth mAuth;
    DatabaseReference reference;
    FirebaseDatabase database;
    private FirebaseFirestore firestore;
    private GoogleSignInClient client;
    private boolean isPasswordVisible = false;
    private boolean isDoctorSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        user = findViewById(R.id.user);
        doctor = findViewById(R.id.docter);
        googleBtn = findViewById(R.id.google);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("Users");

        mailorphone = findViewById(R.id.emailEditText);
        fullname = findViewById(R.id.fullNameEditText);
        pass = findViewById(R.id.passwordEditText);
        login = findViewById(R.id.logInButton);
        hide = findViewById(R.id.hide);
        progressBar = findViewById(R.id.progressbar);

        user.setImageResource(R.drawable.user);
        doctor.setImageResource(R.drawable.doctor);

        user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDoctorSelected = false;
                user.setImageResource(R.drawable.user);
                doctor.setImageResource(R.drawable.doctor);
            }
        });

        doctor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDoctorSelected = true;
                user.setImageResource(R.drawable.user1);
                doctor.setImageResource(R.drawable.doctor1);
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Register.this, Login.class);
                startActivity(intent);
            }
        });

        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // Hide password
                    pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    hide.setImageResource(R.drawable.visibilityoff);
                } else {
                    // Show password
                    pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    hide.setImageResource(R.drawable.removepasswordhide);
                }
                // Move the cursor to the end
                pass.setSelection(pass.length());
                isPasswordVisible = !isPasswordVisible;
            }
        });

        googleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already authenticated, redirect to main activity
            startActivity(new Intent(Register.this, MainActivity.class));
            finish();
            Toast.makeText(Register.this, "Welcome!", Toast.LENGTH_SHORT).show();
        }

        CheckBox agreeCheckBox = findViewById(R.id.agreeCheckBox);
        Button signUpButton = findViewById(R.id.signUpButton);

        agreeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                signUpButton.setEnabled(isChecked);
                if (isChecked) {
                    signUpButton.setBackgroundResource(R.drawable.btn_gradient_style1);
                } else {
                    signUpButton.setBackgroundResource(R.drawable.btn_gradient_style1_disabled);
                }
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = fullname.getText().toString();
                String emailOrPhone = mailorphone.getText().toString().trim();
                String password = pass.getText().toString();

                if (emailOrPhone.isEmpty()) {
                    mailorphone.setError("Please fill your email");
                    return;
                }
                if (name.isEmpty()) {
                    fullname.setError("Please fill your name");
                    return;
                }
                if (password.isEmpty()) {
                    pass.setError("Please provide the password");
                    return;
                }
                if (password.length() < 8) {
                    pass.setError("Password must be at least 8 characters long");
                    return;
                }

                if (emailOrPhone.contains("@")) {
                    checkemail(name, emailOrPhone, password);
                } else {
                    if (!emailOrPhone.startsWith("+")) {
                        // Assuming the phone number is entered without country code, e.g., "1234567890"
                        emailOrPhone = "+91" + emailOrPhone; // You can dynamically set the country code or ask the user to input it
                    }
                    checkphone(name, emailOrPhone, password);
                }
            }
        });
    }

    private void checkphone(String name, String phone, String password) {
        progressBar.setVisibility(View.VISIBLE);

        // First, check in the "Users" collection
        firestore.collection("Users")
                .whereEqualTo("phoneNumber", phone)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Phone number found in "Users" collection
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(Register.this, "Phone number is already registered as User. Please login.", Toast.LENGTH_SHORT).show();
                    } else {
                        // If not found in "Users" collection, check in "Doctors" collection
                        firestore.collection("Doctors")
                                .whereEqualTo("phoneNumber", phone)
                                .get()
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful() && !task2.getResult().isEmpty()) {
                                        // Phone number found in "Doctors" collection
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(Register.this, "Phone number is already registered as Doctor. Please login.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Phone number not found in Firestore, now check Firebase Authentication
                                        mAuth.fetchSignInMethodsForEmail(phone + "@phone.com").addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                                                progressBar.setVisibility(View.GONE);
                                                if (task.isSuccessful()) {
                                                    boolean isPhoneRegistered = !task.getResult().getSignInMethods().isEmpty();
                                                    if (isPhoneRegistered) {
                                                        Toast.makeText(Register.this, "Phone number is already registered in Firebase. Please login.", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        // If not registered, proceed with the sign-up process
                                                        signUpWithPhone(name, phone, password);
                                                    }
                                                } else {
                                                    Toast.makeText(Register.this, "Failed to check if phone number is registered. Please try again.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(Register.this, "Error retrieving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void signUpWithPhone(String name, String phone, String password) {
        progressBar.setVisibility(View.VISIBLE);

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phone)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                signInWithPhoneAuthCredential(phoneAuthCredential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Toast.makeText(Register.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                Intent intent = new Intent(Register.this, OTP.class);
                                intent.putExtra("verificationId", verificationId);
                                intent.putExtra("resendToken", forceResendingToken);
                                intent.putExtra("name", name);
                                intent.putExtra("phoneNumber", phone);
                                intent.putExtra("password", password);
                                intent.putExtra("isDoctorSelected", isDoctorSelected);// Pass the password to the next activity if needed
                                startActivity(intent);
                                progressBar.setVisibility(View.GONE);
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Register.this, "Phone sign-up successful", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        // Save the password for the user here
                    } else {
                        Toast.makeText(Register.this, "Phone sign-up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void checkemail(String name, String email, String password) {
        progressBar.setVisibility(View.VISIBLE);

        // Check if email is already registered
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                    @Override
                    public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                        if (task.isSuccessful()) {
                            boolean isEmailRegistered = !task.getResult().getSignInMethods().isEmpty();
                            if (isEmailRegistered) {
                                Toast.makeText(Register.this, "Email is already registered, please login.", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                            } else {
                                signUpWithEmail(name, email, password);
                            }
                        } else {
                            Toast.makeText(Register.this, "Failed to check if email is registered. Please try again.", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }
    private  void signUpWithEmail(String name, String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(Register.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            sendEmailVerification(name, email, password);
                        } else {
                            Exception exception = task.getException();
                            if (exception != null) {
                                Log.e(TAG, "Registration failed: " + exception.getMessage());
                                showToast("Registration failed: " + exception.getMessage());
                            } else {
                                showToast("Registration failed.");
                            }
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void  sendEmailVerification(String name, String email, String password) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            firebaseUser.sendEmailVerification()
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(Register.this, "Verification email sent to " + firebaseUser.getEmail(), Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            checkEmailVerification(name, email, password);
                        } else {
                            Toast.makeText(Register.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    });
        }
    }


    private void checkEmailVerification(String name, String email, String password) {
        FirebaseUser user1 = mAuth.getCurrentUser();
        if (user1 != null) {
            user1.reload().addOnCompleteListener(task -> {
                if (user1.isEmailVerified()) {
                    Toast.makeText(Register.this, "Email verified", Toast.LENGTH_SHORT).show();
                    String userId = user1.getUid();

                    // Save user data in Firestore
                    Map<String, Object> user = new HashMap<>();
                    user.put("userId", userId);
                    user.put("name", name);
                    user.put("email", email);
                    user.put("password", password);

                    String collection = isDoctorSelected ? "Doctors" : "Users";
                    firestore.collection(collection).document(userId)
                            .set(user)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(Register.this, "Sign-up successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(Register.this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(Register.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(Register.this, "Please verify your email", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !user.isEmailVerified()) {
            checkEmailVerification(fullname.getText().toString(), mailorphone.getText().toString(), pass.getText().toString());
        }
    }

    private void showToast(String message) {
        Toast.makeText(Register.this, message, Toast.LENGTH_SHORT).show();
    }

    private void signIn() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        client = GoogleSignIn.getClient(this, options);

        Intent signInIntent = client.getSignInIntent();
        startActivityForResult(signInIntent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String email = account.getEmail();

                    // Check if the email is already registered
                    mAuth.fetchSignInMethodsForEmail(email)
                            .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                                @Override
                                public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                                    if (task.isSuccessful()) {
                                        boolean isEmailRegistered = !task.getResult().getSignInMethods().isEmpty();
                                        if (isEmailRegistered) {
                                            // Email is already registered
                                            Toast.makeText(Register.this, "This email is already registered. Please log in.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Email is not registered, proceed with Google sign-in
                                            firebaseAuthWithGoogle(account);
                                        }
                                    } else {
                                        Toast.makeText(Register.this, "Failed to check if email is registered. Please try again.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            } catch (ApiException e) {
                showToast("Something went wrong" + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();
                                String name = user.getDisplayName();
                                String email = user.getEmail();
                                String phone = user.getPhoneNumber();

                                // Save user data in Firestore
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("userId", userId);
                                userData.put("name", name);
                                userData.put("email", email);
                                userData.put("phone", phone);

                                String collection = isDoctorSelected ? "Doctors" : "Users";
                                firestore.collection(collection).document(userId)
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(Register.this, "Google sign-in successful", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(Register.this, MainActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(Register.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            Toast.makeText(Register.this, "Google sign-in failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            // If the user presses back again within a short time, close the app
            super.onBackPressed();
            finishAffinity(); // Close all activities
        } else {
            // Inform the user to press back again to exit
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();

            // Reset the flag after a short time (e.g., 2 seconds)
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            doubleBackToExitPressedOnce = false;
                        }
                    }, 2000
            );
        }
    }
}