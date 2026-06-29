package com.example.androidnativegrupo5.ui.reservations;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.local.db.Reserva;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;
import com.example.androidnativegrupo5.data.model.CreateReservationRequest;
import com.example.androidnativegrupo5.data.model.ErrorResponse;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentOrderSummaryBinding;
import com.example.androidnativegrupo5.utils.SyncManager;
import com.google.gson.Gson;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class OrderSummaryFragment extends Fragment {

    @Inject ApiService apiService;
    @Inject ReservaDao reservaDao;
    @Inject TokenManager tokenManager;

    private FragmentOrderSummaryBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private long activityId;
    private String activityName;
    private float activityPrice;
    private int slots;
    private String date;
    private String time;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            activityId = getArguments().getLong("activityId");
            activityName = getArguments().getString("activityName");
            activityPrice = getArguments().getFloat("activityPrice");
            slots = getArguments().getInt("slots");
            date = getArguments().getString("date");
            time = getArguments().getString("time");
        }

        binding.textActivityName.setText(activityName);
        binding.textDateTime.setText(String.format("Fecha: %s | Hora: %s", date, time));
        binding.textParticipants.setText(String.format(Locale.getDefault(), "Participantes: %d", slots));

        double total = activityPrice * slots;
        binding.textTotalPrice.setText(String.format(Locale.getDefault(), "Total: $%.2f", total));

        if (activityPrice == 0.0) {
            binding.layoutCardNumber.setVisibility(View.GONE);
            binding.layoutExpiryDate.setVisibility(View.GONE);
            binding.layoutCvv.setVisibility(View.GONE);
            binding.layoutCardHolder.setVisibility(View.GONE);
            binding.buttonConfirmPayment.setText("Confirmar Reserva Gratis");
        }

        binding.buttonConfirmPayment.setOnClickListener(v -> processPayment());
    }

    private void processPayment() {
        if (activityPrice > 0.0) {
            if (!validateCard()) return;
        }

        String tokenRaw = tokenManager.getToken();
        if (tokenRaw == null) {
            Toast.makeText(getContext(), "Error de sesión", Toast.LENGTH_SHORT).show();
            return;
        }
        String token = "Bearer " + tokenRaw;
        
        // Backend expects HH:mm:ss
        String formattedTime = time;
        if (time != null && time.length() == 5) {
            formattedTime = time + ":00";
        }
        
        CreateReservationRequest reservationRequest = new CreateReservationRequest(activityId, slots, date, formattedTime);

        if (activityPrice > 0.0) {
            String cardNumber = binding.editCardNumber.getText() != null ? binding.editCardNumber.getText().toString() : "";
            String cardHolder = binding.editCardHolder.getText() != null ? binding.editCardHolder.getText().toString() : "";
            String expiryDate = binding.editExpiryDate.getText() != null ? binding.editExpiryDate.getText().toString() : "";
            String cvv = binding.editCvv.getText() != null ? binding.editCvv.getText().toString() : "";

            CreateReservationRequest.CardDetails details = new CreateReservationRequest.CardDetails(
                    cardNumber,
                    cardHolder,
                    expiryDate,
                    cvv
            );
            reservationRequest.setCardDetails(details);
        }

        binding.buttonConfirmPayment.setEnabled(false);

        apiService.createReservation(token, reservationRequest).enqueue(new Callback<ReservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReservationResponse> call, @NonNull Response<ReservationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReservationResponse res = response.body();
                    if ("CONFIRMED".equalsIgnoreCase(res.getStatus())) {
                        executor.execute(() -> reservaDao.insert(Reserva.fromResponse(res)));
                        SyncManager.needsRefresh = true;
                        Toast.makeText(getContext(), "¡Reserva Confirmada con Éxito!", Toast.LENGTH_LONG).show();
                        Navigation.findNavController(requireView()).navigate(R.id.action_OrderSummaryFragment_to_MyReservationsFragment);
                    } else {
                        binding.buttonConfirmPayment.setEnabled(true);
                        Toast.makeText(getContext(), "Estado: " + res.getStatus(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    binding.buttonConfirmPayment.setEnabled(true);
                    String errorMsg = "Error: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            ErrorResponse error = new Gson().fromJson(response.errorBody().string(), ErrorResponse.class);
                            if (error != null && error.getMessage() != null) {
                                errorMsg = error.getMessage();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("OrderSummary", "Error parsing error body", e);
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReservationResponse> call, @NonNull Throwable t) {
                binding.buttonConfirmPayment.setEnabled(true);
                Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateCard() {
        String cardNumber = binding.editCardNumber.getText() != null ? binding.editCardNumber.getText().toString() : "";
        String cardHolder = binding.editCardHolder.getText() != null ? binding.editCardHolder.getText().toString() : "";

        if (cardNumber.length() != 16) {
            binding.layoutCardNumber.setError("16 dígitos");
            return false;
        }
        if (cardHolder.trim().isEmpty()) {
            binding.layoutCardHolder.setError("Campo requerido");
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}