package com.example.kaustubha;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.facebook.CallbackManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/** @noinspection deprecation, deprecation, deprecation, deprecation, deprecation */
public class Login extends AppCompatActivity {

    EditText email, passw;
    Button login;
    ImageView google, hide;
    TextView forgot;
    ProgressBar progressBar;
    FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    boolean pass;
    private GoogleSignInClient client;
    CallbackManager callbackManager;
    private boolean isPasswordVisible = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.emailEditText);
        passw = findViewById(R.id.passwordEditText);
        login = findViewById(R.id.login1);
        google = findViewById(R.id.google1);
        forgot = findViewById(R.id.forgot);
        hide = findViewById(R.id.hide);
        progressBar = findViewById(R.id.progressbar1);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        callbackManager = CallbackManager.Factory.create();

        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // Hide password
                    passw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    hide.setImageResource(R.drawable.visibilityoff);
                } else {
                    // Show password
                    passw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    hide.setImageResource(R.drawable.removepasswordhide);
                }
                // Move the cursor to the end
                passw.setSelection(passw.length());
                isPasswordVisible = !isPasswordVisible;
            }
        });

        forgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, ForgotPassword.class);
                startActivity(intent);
            }
        });

        TextView textViewCreateNow = findViewById(R.id.textView3);

        SpannableString spannableString = new SpannableString("Don't have account? Create now ");

        // Set the color and click listener for "Create now"
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Handle the click event and redirect to another page
                Intent intent = new Intent(Login.this, Register.class);
                startActivity(intent);
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(Login.this, R.color.purple));
                ds.setUnderlineText(false);
            }
        };
        // Apply the clickable span to the "Create now" part
        spannableString.setSpan(clickableSpan, 19, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Make the TextView clickable
        textViewCreateNow.setMovementMethod(LinkMovementMethod.getInstance());
        textViewCreateNow.setText(spannableString);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already authenticated, redirect to main activity
            startActivity(new Intent(Login.this, MainActivity.class));
            finish();
        }

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String identifier = email.getText().toString().trim();
                String password = passw.getText().toString().trim();

                if (TextUtils.isEmpty(identifier)) {
                    Toast.makeText(Login.this, "Enter email or phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(Login.this, "Enter password", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                    if (!validateEmail() | !validatePassword()) {
                        showToast("Email or password is incorrect");
                    } else {
                        loginUser();
                    }
                } else if (Patterns.PHONE.matcher(identifier).matches()) {
                    if (!identifier.startsWith("+")) {
                        // Assuming the phone number is entered without country code, e.g., "1234567890"
                        identifier = "+91" + identifier; // You can dynamically set the country code or ask the user to input it
                    }
                    signInWithPhone(identifier, password);
                } else {
                    Toast.makeText(Login.this, "Invalid email or phone number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

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

                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(),null);
                FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()){
                                    // Sign in success, update UI with the signed-in user's information
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        showToast("Login successful!");
                                        Intent intent = new Intent(Login.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                }else {
                                    Toast.makeText(Login.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }

                            }
                        });

            } catch (ApiException e) {
                showToast("Something went wrong" + e.getMessage());
            }
        }
    }
    public Boolean validateEmail() {
        String val = email.getText().toString().trim();
        if (val.isEmpty()) {
            email.setError("Email cannot be Empty");
            return false;
        } else {
            email.setError(null);
            return true;
        }
    }
    public Boolean validatePassword() {
        String val = passw.getText().toString().trim();
        if (val.isEmpty()) {
            passw.setError("Password cannot be Empty");
            return false;
        } else {
            passw.setError(null);
            return true;
        }
    }

    private void loginUser() {
        final String userEmail = email.getText().toString().trim();
        String userPassword = passw.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);

        FirebaseAuth.getInstance().signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                showToast("Login successful!");
                                Intent intent = new Intent(Login.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            else {
                                showToast("Failed to retrieve user information");
                            }
                        }
                        else {
                            // If sign in fails, display a message to the user.
                            String errorMessage = task.getException().getMessage();
                            if (errorMessage.contains("no user record")) {
                                showToast("Email does not exist. Please register.");
                            }
                            else {
                                showToast("Login failed. " + errorMessage);
                            }
                        }
                    }
                });
    }

    private void signInWithPhone(String phoneNumber, String password) {
        progressBar.setVisibility(View.VISIBLE);

        // First, check in the "Users" collection
        firestore.collection("Users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // User found in "Users" collection
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String storedPassword = document.getString("password");

                        if (storedPassword != null && storedPassword.equals(password)) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(Login.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            // Redirect to MainActivity
                            startActivity(new Intent(Login.this, MainActivity.class));
                            finish();
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(Login.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // If not found in "Users" collection, check in "Doctors" collection
                        firestore.collection("Doctors")
                                .whereEqualTo("phoneNumber", phoneNumber)
                                .get()
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful() && !task2.getResult().isEmpty()) {
                                        // User found in "Doctors" collection
                                        DocumentSnapshot document = task2.getResult().getDocuments().get(0);
                                        String storedPassword = document.getString("password");

                                        if (storedPassword != null && storedPassword.equals(password)) {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(Login.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                            // Redirect to MainActivity
                                            startActivity(new Intent(Login.this, MainActivity.class));
                                            finish();
                                        } else {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(Login.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(Login.this, "User not found", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(Login.this, "Error retrieving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showToast(String message) {
        Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show();
    }
}