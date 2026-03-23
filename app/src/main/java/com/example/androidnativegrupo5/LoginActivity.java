package com.example.androidnativegrupo5;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidnativegrupo5.model.AuthResponse;
import com.example.androidnativegrupo5.model.LoginRequest;
import com.example.androidnativegrupo5.model.MessageResponse;
import com.example.androidnativegrupo5.model.OtpRequest;
import com.example.androidnativegrupo5.network.ApiService;
import com.example.androidnativegrupo5.network.RetrofitClient;
import com.example.androidnativegrupo5.utils.Constants;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LoginActivity handles the user authentication process.
 * It validates user credentials and requests an OTP code upon successful login.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailEditText, passwordEditText;
    private Button loginButton;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Initialize UI components
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        TextView registerText = findViewById(R.id.registerText);

        // Set listener for the login button
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (validarCampos(email, password)) {
                llamarLogin(email, password);
            }
        });

        // Set listener for the register redirection
        registerText.setOnClickListener(v -> 
            Snackbar.make(v, R.string.register_soon, Snackbar.LENGTH_SHORT).show()
        );
    }

    /**
     * Validates that the input fields are not empty and follow the correct format.
     */
    private boolean validarCampos(String email, String password) {
        boolean isValid = true;

        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.error_email_required));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.error_password_required));
            isValid = false;
        } else if (password.length() < Constants.MIN_PASSWORD_LENGTH) {
            passwordLayout.setError(getString(R.string.error_password_short));
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        return isValid;
    }

    /**
     * Performs the login request to the server using Retrofit.
     */
    private void llamarLogin(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);

        setLoading(true);

        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful()) {
                    // If login is successful, request an OTP
                    solicitarOtpYProceder(email);
                } else {
                    setLoading(false);
                    int errorResId = R.string.error_invalid_credentials;
                    if (response.code() == 404) {
                        errorResId = R.string.error_user_not_found;
                    } else if (response.code() == 401) {
                        errorResId = R.string.error_wrong_credentials;
                    }
                    showError(getString(errorResId));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                showError(getString(R.string.error_connection));
            }
        });
    }

    /**
     * Requests an OTP code for the given email and navigates to the OTP verification screen.
     */
    private void solicitarOtpYProceder(String email) {
        OtpRequest otpRequest = new OtpRequest(email);

        apiService.requestOtp(otpRequest).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                setLoading(false);
                navigateToOtp(email);
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                setLoading(false);
                // Even on failure, we proceed to OTP screen as the service might have sent it anyway
                navigateToOtp(email);
            }
        });
    }

    /**
     * Helper method to navigate to OtpActivity.
     */
    private void navigateToOtp(String email) {
        Intent intent = new Intent(LoginActivity.this, OtpActivity.class);
        intent.putExtra(Constants.EXTRA_EMAIL, email);
        startActivity(intent);
    }

    /**
     * Updates the UI state to reflect a loading process.
     */
    private void setLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        loginButton.setText(isLoading ? R.string.loading : R.string.ingresar);
        emailLayout.setEnabled(!isLoading);
        passwordLayout.setEnabled(!isLoading);
    }

    /**
     * Displays an error message using a Snackbar.
     */
    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}
