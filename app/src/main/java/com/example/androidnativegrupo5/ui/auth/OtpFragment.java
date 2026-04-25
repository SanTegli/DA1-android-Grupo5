package com.example.androidnativegrupo5.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;
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

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * OtpFragment handles the verification of the One-Time Password (OTP) sent to the user.
 */
@AndroidEntryPoint
public class OtpFragment extends Fragment {

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    private String email;
    private TextInputEditText otpEditText;
    private Button verifyButton;

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

        otpEditText = view.findViewById(R.id.otpEditText);
        verifyButton = view.findViewById(R.id.verifyButton);
        TextView resendText = view.findViewById(R.id.resendText);

        verifyButton.setOnClickListener(v -> {
            if (otpEditText.getText() == null) return;
            String otp = otpEditText.getText().toString().trim();

            if (validarOtp(otp)) {
                verificarOtp(email, otp);
            }
        });

        if (resendText != null) {
            resendText.setOnClickListener(v -> reenviarOtp());
        }
    }

    private boolean validarOtp(String otp) {
        if (otp.isEmpty()) {
            otpEditText.setError(getString(R.string.error_otp_required));
            return false;
        } else if (otp.length() != Constants.OTP_LENGTH) {
            otpEditText.setError(getString(R.string.error_otp_length));
            return false;
        }
        return true;
    }

    private void verificarOtp(String email, String otp) {
        OtpVerifyRequest request = new OtpVerifyRequest(email, otp);

        setLoading(true);

        apiService.verifyOtp(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    //saveToken(response.body().getToken());
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
                setLoading(false);
                String message = getString(R.string.error_connection_detail, t.getMessage());
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveToken(String token) {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("auth_token", token).apply();
    }

    private void reenviarOtp() {
        OtpRequest request = new OtpRequest(email);

        apiService.resendOtp(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), R.string.otp_resent, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), R.string.error_resend_otp, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (getView() == null) return;
        verifyButton.setEnabled(!isLoading);
        verifyButton.setText(isLoading ? R.string.verifying : R.string.verify);
        otpEditText.setEnabled(!isLoading);
    }
}
