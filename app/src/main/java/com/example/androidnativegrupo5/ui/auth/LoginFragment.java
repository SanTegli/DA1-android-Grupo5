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
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnSubmit, btnRequestOtp;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnRequestOtp = view.findViewById(R.id.btnRequestOtp);
        SwitchMaterial switchQuickLogin = view.findViewById(R.id.switchQuickLogin);
        TextView registerText = view.findViewById(R.id.registerText);

        if (switchQuickLogin != null) {
            switchQuickLogin.setChecked(tokenManager.isBiometricEnabled());

            switchQuickLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
                tokenManager.setBiometricEnabled(isChecked);
                String msg = isChecked ? "Huella activada para la próxima" : "Huella desactivada";
                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show();
            });
        }

        btnSubmit.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            if (validarCamposFull(email, pass)) {
                llamarLoginTradicional(email, pass);
            }
        });

        btnRequestOtp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                tilEmail.setError("Ingresá tu email para recibir el código");
                Toast.makeText(getContext(), "Primero necesitamos tu email", Toast.LENGTH_SHORT).show();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Formato de email inválido");
            } else {
                tilEmail.setError(null);
                enviarSoloOtp(email);
            }
        });

        registerText.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_LoginFragment_to_RegisterFragment)
        );
    }

    private boolean validarCamposFull(String email, String pass) {
        boolean isValid = true;
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email inválido");
            isValid = false;
        } else { tilEmail.setError(null); }

        if (pass.isEmpty()) {
            tilPassword.setError("Contraseña requerida");
            isValid = false;
        } else { tilPassword.setError(null); }

        return isValid;
    }

    private void llamarLoginTradicional(String email, String password) {
        setLoading(true);
        apiService.login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!isAdded() || getView() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveToken(response.body().getToken());
                    NavHostFragment.findNavController(LoginFragment.this)
                            .navigate(R.id.action_LoginFragment_to_FirstFragment);
                } else {
                    setLoading(false);
                    showError("Credenciales incorrectas");
                }
            }
            @Override public void onFailure(Call<AuthResponse> call, Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                showError("Error de conexión");
            }
        });
    }

    private void enviarSoloOtp(String email) {
        setLoading(true);
        apiService.requestOtp(new OtpRequest(email)).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (!isAdded() || getView() == null) return;
                setLoading(false);
                if (response.isSuccessful()) {
                    navigateToOtp(email);
                } else {
                    showError("Email no registrado");
                }
            }
            @Override public void onFailure(Call<MessageResponse> call, Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                showError("Error de red");
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
        btnSubmit.setEnabled(!isLoading);
        btnSubmit.setText(isLoading ? "Cargando..." : "Ingresar");
        tilEmail.setEnabled(!isLoading);
        tilPassword.setEnabled(!isLoading);
        btnRequestOtp.setEnabled(!isLoading);
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

}
