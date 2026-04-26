package com.example.androidnativegrupo5.ui.reservations;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.db.Reserva;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;
import com.example.androidnativegrupo5.databinding.FragmentReservationBinding;
import com.example.androidnativegrupo5.data.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.data.model.CreateReservationRequest;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.google.android.material.chip.Chip;

import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class ReservationFragment extends Fragment {

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    @Inject
    ReservaDao reservaDao;

    // Room no permite operaciones en el main thread
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private FragmentReservationBinding binding;
    private String activityName;
    private double activityPrice;
    private Calendar calendar = Calendar.getInstance();

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

        loadAvailabilities();

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

        String token = tokenManager.getToken();

        if (token == null) {
            Toast.makeText(getContext(), "Inicie sesión para reservar", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.createReservation(request).enqueue(new Callback<ReservationResponse>() {
            @Override
            public void onResponse(Call<ReservationResponse> call, Response<ReservationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Reserva reservaLocal = Reserva.fromResponse(response.body());
                    executor.execute(() -> {
                        reservaDao.insert(reservaLocal);
                    });
                    Toast.makeText(getContext(), "¡Reserva exitosa!", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        getActivity().getOnBackPressedDispatcher().onBackPressed();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
                        Log.e("RESERVA_ERROR", "Code: " + response.code() + " Body: " + errorBody);
                        Toast.makeText(getContext(), "Error al realizar la reserva", Toast.LENGTH_SHORT).show();
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

    private boolean validateFields() {
        boolean valid = true;

        if (selectedDateFormatted == null || selectedDateFormatted.isEmpty()) {
            Toast.makeText(getContext(), "Seleccione una fecha", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (selectedTime == null || selectedTime.isEmpty()) {
            Toast.makeText(getContext(), "Seleccione una hora", Toast.LENGTH_SHORT).show();
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

    private List<AvailabilitySlotResponse> allSlots;

    private void loadAvailabilities() {
        long activityId = getArguments().getLong("activityId");

        apiService.getAvailability(activityId).enqueue(new Callback<List<AvailabilitySlotResponse>>() {
            @Override
            public void onResponse(Call<List<AvailabilitySlotResponse>> call,
                                   Response<List<AvailabilitySlotResponse>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    allSlots = response.body();
                    showDates();
                }
            }

            @Override
            public void onFailure(Call<List<AvailabilitySlotResponse>> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar disponibilidad", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showDates() {
        if (binding == null) return;
        binding.chipGroupDates.removeAllViews();

        Set<String> uniqueDates = new LinkedHashSet<>();

        if (allSlots != null) {
            for (AvailabilitySlotResponse slot : allSlots) {
                uniqueDates.add(slot.getDate());
            }
        }

        for (String date : uniqueDates) {

            Chip chip = new Chip(requireContext());
            chip.setText(date);
            chip.setCheckable(true);

            chip.setChipBackgroundColorResource(R.color.chip_selector_bg);
            chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_selector));

            chip.setOnClickListener(v -> {
                selectedDateFormatted = date;

                // RESET
                selectedTime = "";
                availableSlots = 0;
                binding.textAvailableSlots.setText("Cupos disponibles: -");

                showTimes(date);
            });

            binding.chipGroupDates.addView(chip);
        }
    }

    private void showTimes(String date) {
        if (binding == null) return;
        binding.chipGroupTimes.removeAllViews();

        if (allSlots != null) {
            for (AvailabilitySlotResponse slot : allSlots) {

                if (slot.getDate().equals(date)) {

                    Chip chip = new Chip(requireContext());
                    String time = slot.getTime().length() >= 5 ? slot.getTime().substring(0, 5) : slot.getTime();

                    chip.setText(time);
                    chip.setCheckable(true);

                    chip.setChipBackgroundColorResource(R.color.chip_selector_bg);
                    chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_selector));

                    if (slot.getAvailableSlots() == 0) {
                        chip.setEnabled(false);
                    }

                    chip.setOnClickListener(v -> {
                        selectedTime = time;
                        availableSlots = slot.getAvailableSlots();

                        binding.textAvailableSlots.setText("Cupos: " + availableSlots);
                    });

                    binding.chipGroupTimes.addView(chip);
                }
            }
        }
    }
}
