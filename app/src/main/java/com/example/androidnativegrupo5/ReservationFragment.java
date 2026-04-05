package com.example.androidnativegrupo5;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.androidnativegrupo5.databinding.FragmentReservationBinding;
import com.example.androidnativegrupo5.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.model.CreateReservationRequest;
import com.example.androidnativegrupo5.model.ReservationResponse;
import com.example.androidnativegrupo5.network.ApiService;
import com.example.androidnativegrupo5.network.RetrofitClient;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReservationFragment extends Fragment {

    private FragmentReservationBinding binding;
    private String activityName;
    private double activityPrice;
    private Calendar calendar = Calendar.getInstance();
    private ApiService apiService;

    private int availableSlots = 0;
    private String selectedDateFormatted = "";
    private String selectedTime = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReservationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        if (getArguments() != null) {
            activityName = getArguments().getString("activityName");
            activityPrice = getArguments().getFloat("activityPrice");
        }

        binding.textActivityName.setText(activityName);

        if (activityPrice <= 0) {
            binding.textActivityPrice.setText(R.string.price_free);
        } else {
            binding.textActivityPrice.setText(String.format(Locale.getDefault(), "$%.2f", activityPrice));
        }

        updateTotalPrice(1);

        // Date picker
        binding.editDate.setOnClickListener(v -> showDatePicker());

        // Time picker
        binding.editTime.setOnClickListener(v -> showTimePicker());

        // Slots listener
        binding.editSlots.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int count = Integer.parseInt(s.toString());
                    updateTotalPrice(count);
                } catch (Exception e) {
                    updateTotalPrice(0);
                }
            }
        });

        // Confirm button
        binding.buttonConfirmReservation.setOnClickListener(v -> confirmReservation());
    }

    private void confirmReservation() {
        if (!validateFields()) return;

        long activityId = getArguments().getLong("activityId");
        int slotsRequested = Integer.parseInt(binding.editSlots.getText().toString());

        if (availableSlots == 0) {
            Toast.makeText(getContext(), "No hay disponibilidad", Toast.LENGTH_SHORT).show();
            return;
        }

        if (slotsRequested > availableSlots) {
            Toast.makeText(getContext(), "No hay suficientes cupos", Toast.LENGTH_SHORT).show();
            return;
        }

        CreateReservationRequest request = new CreateReservationRequest(
                activityId,
                slotsRequested,
                selectedDateFormatted,
                selectedTime
        );

        apiService.createReservation(request).enqueue(new Callback<ReservationResponse>() {
            @Override
            public void onResponse(Call<ReservationResponse> call, Response<ReservationResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "¡Reserva exitosa!", Toast.LENGTH_SHORT).show();
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e("RESERVA_ERROR", "Code: " + response.code() + " Body: " + errorBody);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ReservationResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {

                    calendar.set(year, month, day);

                    String displayDate = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                    binding.editDate.setText(displayDate);

                    selectedDateFormatted = year + "-" + String.format("%02d", (month + 1)) + "-" + String.format("%02d", day);

                    binding.layoutDate.setError(null);

                    tryLoadAvailability();

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(requireContext(),
                (view, hour, minute) -> {

                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    binding.editTime.setText(selectedTime);

                    binding.layoutTime.setError(null);

                    tryLoadAvailability();

                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );

        dialog.show();
    }

    private void tryLoadAvailability() {
        if (selectedDateFormatted.isEmpty() || selectedTime.isEmpty()) return;

        long activityId = getArguments().getLong("activityId");

        apiService.getAvailability(activityId).enqueue(new Callback<List<AvailabilitySlotResponse>>() {
            @Override
            public void onResponse(Call<List<AvailabilitySlotResponse>> call,
                                   Response<List<AvailabilitySlotResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    binding.textAvailableSlots.setText("Error al cargar cupos");
                    return;
                }

                AvailabilitySlotResponse selected = null;

                for (AvailabilitySlotResponse slot : response.body()) {
                    if (slot.getDate().equals(selectedDateFormatted)
                            && slot.getTime().substring(0, 5).equals(selectedTime)) {
                        selected = slot;
                        break;
                    }
                }

                if (selected != null) {
                    availableSlots = selected.getAvailableSlots();
                    binding.textAvailableSlots.setText("Cupos disponibles: " + availableSlots);
                } else {
                    availableSlots = 0;
                    binding.textAvailableSlots.setText("Sin disponibilidad");
                }
            }

            @Override
            public void onFailure(Call<List<AvailabilitySlotResponse>> call, Throwable t) {
                binding.textAvailableSlots.setText("Error al cargar cupos");
            }
        });
    }

    private boolean validateFields() {
        boolean valid = true;

        if (binding.editDate.getText().toString().isEmpty()) {
            binding.layoutDate.setError("Seleccione una fecha");
            valid = false;
        }

        if (binding.editTime.getText().toString().isEmpty()) {
            binding.layoutTime.setError("Seleccione una hora");
            valid = false;
        }

        String slotsStr = binding.editSlots.getText().toString();
        if (slotsStr.isEmpty() || Integer.parseInt(slotsStr) <= 0) {
            binding.layoutSlotsInput.setError("Ingrese cantidad válida");
            valid = false;
        } else {
            binding.layoutSlotsInput.setError(null);
        }

        return valid;
    }

    private void updateTotalPrice(int count) {
        double total = count * activityPrice;

        if (activityPrice <= 0) {
            binding.textTotalPrice.setText(R.string.price_free);
        } else {
            binding.textTotalPrice.setText(String.format(Locale.getDefault(), "$%.2f", total));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}