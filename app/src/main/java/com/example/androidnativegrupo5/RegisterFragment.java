package com.example.androidnativegrupo5;

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
import androidx.navigation.Navigation;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.model.AuthResponse;
import com.example.androidnativegrupo5.model.MessageResponse;
import com.example.androidnativegrupo5.model.OtpRequest;
import com.example.androidnativegrupo5.model.RegisterRequest;
import com.example.androidnativegrupo5.network.ApiService;
import com.example.androidnativegrupo5.network.RetrofitClient;
import com.example.androidnativegrupo5.utils.Constants;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment {

    private TextInputLayout usernameLayout, emailLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private ApiService apiService;

    public RegisterFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        usernameLayout = view.findViewById(R.id.usernameLayout);
        emailLayout = view.findViewById(R.id.emailLayout);
        passwordLayout = view.findViewById(R.id.passwordLayout);
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordLayout);

        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText);

        registerButton = view.findViewById(R.id.registerButton);
        TextView loginText = view.findViewById(R.id.loginText);

        registerButton.setOnClickListener(v -> {
            String username = usernameEditText.getText() != null ? usernameEditText.getText().toString().trim() : "";
            String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
            String password = passwordEditText.getText() != null ? passwordEditText.getText().toString().trim() : "";
            String confirmPassword = confirmPasswordEditText.getText() != null ? confirmPasswordEditText.getText().toString().trim() : "";

            if (validarCampos(username, email, password, confirmPassword)) {
                llamarRegister(username, email, password, view);
            }
        });

        loginText.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack()
        );
    }

    private boolean validarCampos(String username, String email, String password, String confirmPassword) {
        boolean isValid = true;

        if (username.isEmpty()) {
            usernameLayout.setError("El username es obligatorio");
            isValid = false;
        } else if (username.length() < 3 || username.length() > 50) {
            usernameLayout.setError("El username debe tener entre 3 y 50 caracteres");
            isValid = false;
        } else {
            usernameLayout.setError(null);
        }

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
        } else if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            passwordLayout.setError("Debe tener mayúscula, minúscula y número");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError("Confirmá la contraseña");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordLayout.setError("Las contraseñas no coinciden");
            isValid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        return isValid;
    }

    private void llamarRegister(String username, String email, String password, View rootView) {
        RegisterRequest request = new RegisterRequest(username, email, password, "USER");
        setLoading(true);

        apiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful()) {
                    solicitarOtpYProceder(email, rootView);
                } else {
                    setLoading(false);
                    if (response.code() == 409) {
                        showError(rootView, "El username o email ya está registrado");
                    } else if (response.code() == 400) {
                        showError(rootView, "Datos inválidos de registro");
                    } else {
                        showError(rootView, getString(R.string.error_connection));
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                showError(rootView, getString(R.string.error_connection));
            }
        });
    }

    private void solicitarOtpYProceder(String email, View rootView) {
        OtpRequest otpRequest = new OtpRequest(email);

        apiService.requestOtp(otpRequest).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                setLoading(false);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.EXTRA_EMAIL, email);
                Navigation.findNavController(rootView)
                        .navigate(R.id.action_registerFragment_to_otpFragment, bundle);
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                setLoading(false);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.EXTRA_EMAIL, email);
                Navigation.findNavController(rootView)
                        .navigate(R.id.action_registerFragment_to_otpFragment, bundle);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        registerButton.setEnabled(!isLoading);
        registerButton.setText(isLoading ? R.string.loading : R.string.registrarse);
        usernameLayout.setEnabled(!isLoading);
        emailLayout.setEnabled(!isLoading);
        passwordLayout.setEnabled(!isLoading);
        confirmPasswordLayout.setEnabled(!isLoading);
    }

    private void showError(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }
}
