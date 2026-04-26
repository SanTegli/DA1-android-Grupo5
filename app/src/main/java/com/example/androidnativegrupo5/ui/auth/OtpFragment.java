package com.example.androidnativegrupo5.ui.auth;

import android.os.Bundle;
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
import com.example.androidnativegrupo5.data.model.MessageResponse;
import com.example.androidnativegrupo5.data.model.OtpRequest;
import com.example.androidnativegrupo5.data.model.OtpVerifyRequest;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.utils.Constants;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class OtpFragment extends Fragment {

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    private String email;
    private TextInputLayout tilOtp;
    private TextInputEditText etOtp;
    private Button btnVerify;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            email = getArguments().getString(Constants.EXTRA_EMAIL);
        }

        tilOtp = view.findViewById(R.id.tilOtp);
        etOtp = view.findViewById(R.id.etOtp);
        btnVerify = view.findViewById(R.id.btnVerify);
        TextView resendText = view.findViewById(R.id.btnResend);

        btnVerify.setOnClickListener(v -> {
            if (etOtp.getText() == null) return;
            String otp = etOtp.getText().toString().trim();

            if (validarOtp(otp)) {
                verificarOtp(email, otp);
            }
        });

        if (resendText != null) {
            resendText.setOnClickListener(v -> reenviarOtp());
        }
    }

    private void verificarOtp(String email, String otp) {
        OtpVerifyRequest request = new OtpVerifyRequest(email, otp);
        setLoading(true);

        apiService.verifyOtp(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!isAdded() || getView() == null) return;

                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getToken();
                    tokenManager.saveToken(token);

                    Toast.makeText(getContext(), R.string.welcome, Toast.LENGTH_SHORT).show();

                    NavHostFragment.findNavController(OtpFragment.this)
                            .navigate(R.id.action_OtpFragment_to_FirstFragment);
                } else {
                    Toast.makeText(getContext(), R.string.error_otp_invalid, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(getContext(), R.string.error_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reenviarOtp() {
        if (email == null) return;
        OtpRequest request = new OtpRequest(email);

        apiService.requestOtp(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), R.string.otp_resent, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (getView() == null) return;
        btnVerify.setEnabled(!isLoading);
        btnVerify.setText(isLoading ? R.string.verifying : R.string.verify);
        etOtp.setEnabled(!isLoading);
    }

    private boolean validarOtp(String otp) {
        if (otp.isEmpty()) {
            etOtp.setError(getString(R.string.error_otp_required));
            return false;
        } else if (otp.length() != 6) { // Usamos 6 que es lo estándar para tu App
            etOtp.setError(getString(R.string.error_otp_length));
            return false;
        }
        return true;
    }
}