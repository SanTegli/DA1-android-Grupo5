package com.example.androidnativegrupo5.ui.auth;

import android.os.Bundle;
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

import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WelcomeFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnBiometric = view.findViewById(R.id.btnBiometric);
        Button btnClassicLogin = view.findViewById(R.id.btnClassicLogin);

        if (!tokenManager.isBiometricEnabled()) {
            btnBiometric.setVisibility(View.GONE);
        }

        btnBiometric.setOnClickListener(v -> setupBiometric());

        btnClassicLogin.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_WelcomeFragment_to_LoginFragment)
        );

        view.findViewById(R.id.tvRegisterWelcome).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_WelcomeFragment_to_RegisterFragment)
        );
    }

    private void setupBiometric() {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        if (tokenManager.getToken() != null) {
                            NavHostFragment.findNavController(WelcomeFragment.this)
                                    .navigate(R.id.action_WelcomeFragment_to_FirstFragment);
                        }
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Ingreso Rápido")
                .setSubtitle("Usa tu huella para entrar")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
