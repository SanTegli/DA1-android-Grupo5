package com.example.androidnativegrupo5.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.utils.NetworkUtils;

import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WelcomeFragment extends Fragment {

    private static final String TAG = "WelcomeFragment";

    @Inject
    TokenManager tokenManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "onViewCreated: Iniciando WelcomeFragment");

        Button btnBiometric = view.findViewById(R.id.btnBiometric);
        Button btnClassicLogin = view.findViewById(R.id.btnClassicLogin);

        setupBiometricVisibility(btnBiometric);

        btnClassicLogin.setOnClickListener(v -> {
            Log.d(TAG, "Navegando a Login");
            navigateTo(R.id.action_WelcomeFragment_to_LoginFragment);
        });

        view.findViewById(R.id.tvRegisterWelcome).setOnClickListener(v -> {
            Log.d(TAG, "Navegando a Registro");
            navigateTo(R.id.action_WelcomeFragment_to_RegisterFragment);
        });
    }

    private void setupBiometricVisibility(Button btnBiometric) {
        BiometricManager biometricManager = BiometricManager.from(requireContext());
        int canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS && 
            tokenManager.isBiometricEnabled() && 
            tokenManager.getToken() != null) {
            
            Log.d(TAG, "Biometría disponible y habilitada");
            btnBiometric.setVisibility(View.VISIBLE);
            btnBiometric.setOnClickListener(v -> {
                if (NetworkUtils.isOnline(requireContext())) {
                    setupBiometric();
                } else {
                    Log.w(TAG, "Intento de login biométrico sin conexión");
                    navigateTo(R.id.action_WelcomeFragment_to_OfflineFragment);
                }
            });
        } else {
            Log.d(TAG, "Botón biométrico oculto: no disponible o no configurado");
            btnBiometric.setVisibility(View.GONE);
        }
    }

    private void setupBiometric() {
        Log.d(TAG, "Iniciando BiometricPrompt");
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Log.i(TAG, "Autenticación biométrica exitosa");

                        String token = tokenManager.getToken();
                        if (token != null) {
                            navigateToMain();
                        } else {
                            Log.e(TAG, "Error crítico: Token nulo tras biometría exitosa");
                            navigateTo(R.id.action_WelcomeFragment_to_LoginFragment);
                        }
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Log.e(TAG, "Error de autenticación biométrica (" + errorCode + "): " + errString);
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Ingreso Rápido")
                .setSubtitle("Usa tu huella para entrar")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void navigateToMain() {
        if (NetworkUtils.isOnline(requireContext())) {
            Log.d(TAG, "Navegando a Home (FirstFragment)");
            navigateTo(R.id.action_WelcomeFragment_to_FirstFragment);
        } else {
            Log.w(TAG, "Red perdida durante navegación, enviando a Offline");
            navigateTo(R.id.action_WelcomeFragment_to_OfflineFragment);
        }
    }

    private void navigateTo(int actionId) {
        if (NetworkUtils.isOnline(requireContext()) || actionId == R.id.action_WelcomeFragment_to_OfflineFragment) {
            NavHostFragment.findNavController(this).navigate(actionId);
        } else {
            Log.w(TAG, "Sin red, redirigiendo a OfflineFragment");
            NavHostFragment.findNavController(this).navigate(R.id.action_WelcomeFragment_to_OfflineFragment);
        }
    }
}
