package com.example.androidnativegrupo5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidnativegrupo5.model.AuthResponse;
import com.example.androidnativegrupo5.model.MessageResponse;
import com.example.androidnativegrupo5.model.OtpRequest;
import com.example.androidnativegrupo5.model.OtpVerifyRequest;
import com.example.androidnativegrupo5.network.ApiService;
import com.example.androidnativegrupo5.network.RetrofitClient;
import com.example.androidnativegrupo5.utils.Constants;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * OtpActivity handles the verification of the One-Time Password (OTP) sent to the user.
 */
public class OtpActivity extends AppCompatActivity {

    private String email;
    private TextInputEditText otpEditText;
    private Button verifyButton;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Retrieve the email passed from LoginActivity
        email = getIntent().getStringExtra(Constants.EXTRA_EMAIL);

        otpEditText = findViewById(R.id.otpEditText);
        verifyButton = findViewById(R.id.verifyButton);
        TextView resendText = findViewById(R.id.resendText);

        // Set listener for the verification button
        verifyButton.setOnClickListener(v -> {
            if (otpEditText.getText() == null) return;
            String otp = otpEditText.getText().toString().trim();

            if (validarOtp(otp)) {
                verificarOtp(email, otp);
            }
        });

        // Set listener for resending the OTP code
        if (resendText != null) {
            resendText.setOnClickListener(v -> reenviarOtp());
        }
    }

    /**
     * Validates the OTP format before sending it to the server.
     */
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

    /**
     * Sends the OTP and email to the server for verification.
     */
    private void verificarOtp(String email, String otp) {
        OtpVerifyRequest request = new OtpVerifyRequest(email, otp);

        setLoading(true);

        apiService.verifyOtp(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(OtpActivity.this, R.string.welcome, Toast.LENGTH_SHORT).show();
                    
                    // Navigate to MainActivity and clear the activity stack
                    Intent intent = new Intent(OtpActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(OtpActivity.this, R.string.error_otp_invalid, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                String message = getString(R.string.error_connection_detail, t.getMessage());
                Toast.makeText(OtpActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Requests a new OTP code for the current email.
     */
    private void reenviarOtp() {
        OtpRequest request = new OtpRequest(email);

        apiService.resendOtp(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OtpActivity.this, R.string.otp_resent, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OtpActivity.this, R.string.error_resend_otp, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                Toast.makeText(OtpActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updates the UI state to reflect a loading process.
     */
    private void setLoading(boolean isLoading) {
        verifyButton.setEnabled(!isLoading);
        verifyButton.setText(isLoading ? R.string.verifying : R.string.verify);
        otpEditText.setEnabled(!isLoading);
    }
}
