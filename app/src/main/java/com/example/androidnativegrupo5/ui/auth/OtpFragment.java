package com.example.androidnativegrupo5.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.androidnativegrupo5.utils.NetworkUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class OtpFragment extends Fragment {

    private static final String TAG = "OtpFragment";

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;

    private String email;
    private EditText[] otpInputs;
    private Button btnVerify;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Iniciando OtpFragment");

        if (getArguments() != null) {
            email = getArguments().getString(Constants.EXTRA_EMAIL);
        }

        otpInputs = new EditText[]{
                view.findViewById(R.id.otp1),
                view.findViewById(R.id.otp2),
                view.findViewById(R.id.otp3),
                view.findViewById(R.id.otp4),
                view.findViewById(R.id.otp5),
                view.findViewById(R.id.otp6)
        };

        setupOtpInputs();

        btnVerify = view.findViewById(R.id.btnVerify);
        TextView resendText = view.findViewById(R.id.btnResend);

        btnVerify.setOnClickListener(v -> {
            String otp = getOtpString();
            if (validarOtp(otp)) {
                if (NetworkUtils.isOnline(requireContext())) {
                    verificarOtp(email, otp);
                } else {
                    Log.w(TAG, "Verificación OTP abortada: Sin conexión");
                    NavHostFragment.findNavController(this).navigate(R.id.OfflineFragment);
                }
            }
        });

        if (resendText != null) {
            resendText.setOnClickListener(v -> reenviarOtp());
        }
    }

    private String getOtpString() {
        StringBuilder otp = new StringBuilder();
        for (EditText et : otpInputs) {
            otp.append(et.getText().toString());
        }
        return otp.toString();
    }

    private void verificarOtp(String email, String otp) {
        Log.i(TAG, "Verificando OTP para: " + email);
        setLoading(true);

        apiService.verifyOtp(new OtpVerifyRequest(email, otp)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!isAdded()) return;
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "OTP verificado con éxito");
                    tokenManager.saveToken(response.body().getToken());
                    Toast.makeText(getContext(), R.string.welcome, Toast.LENGTH_SHORT).show();
                    
                    NavHostFragment.findNavController(OtpFragment.this)
                            .navigate(R.id.action_OtpFragment_to_FirstFragment);
                } else {
                    Log.e(TAG, "OTP inválido: " + response.code());
                    Toast.makeText(getContext(), R.string.error_otp_invalid, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                Log.e(TAG, "Error de red en verificación OTP", t);
                NavHostFragment.findNavController(OtpFragment.this).navigate(R.id.OfflineFragment);
            }
        });
    }

    private void reenviarOtp() {
        if (email == null) return;
        Log.i(TAG, "Solicitando reenvío de OTP");

        apiService.requestOtp(new OtpRequest(email)).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Log.d(TAG, "OTP reenviado");
                    Toast.makeText(getContext(), R.string.otp_resent, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Error de red en reenvío OTP", t);
                Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupOtpInputs() {
        for (int i = 0; i < otpInputs.length; i++) {
            final int index = i;
            otpInputs[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpInputs.length - 1) {
                        otpInputs[index + 1].requestFocus();
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            otpInputs[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && otpInputs[index].getText().toString().isEmpty() && index > 0) {
                    otpInputs[index - 1].requestFocus();
                }
                return false;
            });
        }
    }

    private void setLoading(boolean isLoading) {
        if (getView() == null) return;
        btnVerify.setEnabled(!isLoading);
        for (EditText et : otpInputs) {
            et.setEnabled(!isLoading);
        }
    }

    private boolean validarOtp(String otp) {
        if (otp.length() != 6) {
            Toast.makeText(getContext(), R.string.error_otp_length, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
