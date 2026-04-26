package com.example.androidnativegrupo5.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.biometric.BiometricManager;

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

import java.util.concurrent.Executor;

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

        setupBiometric(view);

        emailLayout = view.findViewById(R.id.emailLayout);
        passwordLayout = view.findViewById(R.id.passwordLayout);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        loginButton = view.findViewById(R.id.loginButton);
        TextView registerText = view.findViewById(R.id.registerText);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (password.isEmpty()) {
                if (validarSoloEmail(email)) {
                    enviarSoloOtp(email);
                }
            } else {
                if (validarCamposFull(email, password)) {
                    llamarLoginTradicional(email, password);
                }
            }
        });

        registerText.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_LoginFragment_to_RegisterFragment)
        );
    }

    private boolean validarSoloEmail(String email) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            return false;
        }
        emailLayout.setError(null);
        return true;
    }

    private boolean validarCamposFull(String user, String password) {
        boolean isValid = true;
        if (user.isEmpty()) {
            emailLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        if (password.length() < Constants.MIN_PASSWORD_LENGTH) {
            passwordLayout.setError(getString(R.string.error_password_short));
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }
        return isValid;
    }

    private void llamarLoginTradicional(String user, String password) {
        setLoading(true);
        LoginRequest request = new LoginRequest(user, password);

        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!isAdded() || getView() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveToken(response.body().getToken());
                    NavHostFragment.findNavController(LoginFragment.this)
                            .navigate(R.id.action_LoginFragment_to_FirstFragment);
                } else {
                    setLoading(false);
                    showError(getString(R.string.error_invalid_credentials));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                showError(getString(R.string.error_connection));
            }
        });
    }

    private void enviarSoloOtp(String email) {
        setLoading(true);
        OtpRequest otpRequest = new OtpRequest(email);

        apiService.requestOtp(otpRequest).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (!isAdded() || getView() == null) return;
                setLoading(false);
                if (response.isSuccessful()) {
                    navigateToOtp(email);
                } else {
                    showError("Email no registrado para OTP");
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                showError(getString(R.string.error_connection));
            }
        });
    }

    private void navigateToOtp(String email) {
        if (!isAdded()) return;
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

    private void setupBiometric(View root) {
        Button btnBiometric = root.findViewById(R.id.btnAuthenticate);

        BiometricManager biometricManager = BiometricManager.from(requireContext());
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        if (btnBiometric == null) return;

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            btnBiometric.setVisibility(View.GONE);
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Inicio de Sesión Biométrico")
                .setSubtitle("Usá tu huella para entrar")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);

                        tokenManager.setBiometricEnabled(true);
                        String savedToken = tokenManager.getToken();

                        if (savedToken != null && !savedToken.isEmpty()) {
                            Toast.makeText(getContext(), "Acceso biométrico exitoso", Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(LoginFragment.this)
                                    .navigate(R.id.action_LoginFragment_to_FirstFragment);
                        } else {
                            Toast.makeText(getContext(), "Inicia sesión con contraseña una vez para activar la huella", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(getContext(), "Error: " + errString, Toast.LENGTH_SHORT).show();
                    }
                });

        btnBiometric.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));
    }
}
