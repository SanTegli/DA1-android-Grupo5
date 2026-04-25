package com.example.androidnativegrupo5.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.AuthResponse;
import com.example.androidnativegrupo5.data.model.LoginRequest;
import com.example.androidnativegrupo5.data.model.MessageResponse;
import com.example.androidnativegrupo5.data.model.OtpRequest;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.utils.Constants;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LoginFragment handles the user authentication process.
 */

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailEditText, passwordEditText;
    private Button loginButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        emailLayout = view.findViewById(R.id.emailLayout);
        passwordLayout = view.findViewById(R.id.passwordLayout);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        loginButton = view.findViewById(R.id.loginButton);
        TextView registerText = view.findViewById(R.id.registerText);

        // Set listener for the login button
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (validarCampos(email, password)) {
                llamarLogin(email, password);
            }
        });

        registerText.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_LoginFragment_to_RegisterFragment)
        );
    }

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

    private void llamarLogin(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);

        setLoading(true);

        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful()) {
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
                navigateToOtp(email);
            }
        });
    }

    private void navigateToOtp(String email) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EXTRA_EMAIL, email);
        NavHostFragment.findNavController(this).navigate(R.id.action_LoginFragment_to_OtpFragment, bundle);
    }

    private void setLoading(boolean isLoading) {
        if (getView() == null) return;
        loginButton.setEnabled(!isLoading);
        loginButton.setText(isLoading ? R.string.loading : R.string.ingresar);
        emailLayout.setEnabled(!isLoading);
        passwordLayout.setEnabled(!isLoading);
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
