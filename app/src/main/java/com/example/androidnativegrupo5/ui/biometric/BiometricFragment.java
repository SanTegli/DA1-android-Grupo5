package com.example.androidnativegrupo5.ui.biometric;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;

import java.util.concurrent.Executor;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BiometricFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

    private static final int ALLOWED_AUTHENTICATORS = Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL;

    private TextView tvStatus;
    private Button btnAuthenticate;
    private Button btnGoToSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_biometric, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStatus = view.findViewById(R.id.tvStatus);
        btnAuthenticate = view.findViewById(R.id.btnAuthenticate);
        btnGoToSettings = view.findViewById(R.id.btnGoToSettings);

        checkBiometricAvailability();

        btnAuthenticate.setOnClickListener(v -> launchBiometricPrompt());

        btnGoToSettings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
            startActivity(intent);
        });
    }

    private void checkBiometricAvailability() {
        BiometricManager manager = BiometricManager.from(requireContext());
        int result = manager.canAuthenticate(ALLOWED_AUTHENTICATORS);

        switch (result) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                tvStatus.setText("Listo para autenticar.");
                btnAuthenticate.setEnabled(true);
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                tvStatus.setText("No hay biometría configurada.");
                btnAuthenticate.setEnabled(false);
                btnGoToSettings.setVisibility(View.VISIBLE);
                break;
            default:
                tvStatus.setText("Biometría no disponible.");
                btnAuthenticate.setEnabled(false);
                break;
        }
    }

    private void launchBiometricPrompt() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación")
                .setSubtitle("Confirmá tu identidad para ver tus reservas")
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build();

        Executor executor = ContextCompat.getMainExecutor(requireContext());

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        if (tokenManager.getToken() != null) {
                            tvStatus.setText("¡Éxito!");
                            NavHostFragment.findNavController(BiometricFragment.this)
                                    .navigate(R.id.MyReservationsFragment);
                        } else {
                            Toast.makeText(getContext(), "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        tvStatus.setText("Huella no reconocida.");
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        tvStatus.setText("Error: " + errString);
                    }
                }
        );

        biometricPrompt.authenticate(promptInfo);
    }
}