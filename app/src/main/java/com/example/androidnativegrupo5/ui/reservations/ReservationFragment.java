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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

    private static final String TAG = "ReservationFragment";

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    @Inject
    ReservaDao reservaDao;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private FragmentReservationBinding binding;
    private String activityName;
    private double activityPrice;

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
        Log.d(TAG, "onViewCreated: Iniciando pantalla de reserva");

        if (getArguments() != null) {
            activityName = getArguments().getString("activityName");
            activityPrice = getArguments().getFloat("activityPrice");
            long activityId = getArguments().getLong("activityId");
            Log.d(TAG, "Cargando datos para actividad: " + activityName + " (ID: " + activityId + ")");
            loadAvailabilities(activityId);
        }

        binding.textActivityName.setText(activityName);

        if (activityPrice <= 0) {
            binding.textActivityPrice.setText(R.string.price_free);
        } else {
            binding.textActivityPrice.setText(String.format(Locale.getDefault(), "$%.2f", activityPrice));
        }

        updateTotalPrice(1);

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

        binding.buttonConfirmReservation.setOnClickListener(v -> confirmReservation());
    }

    private void confirmReservation() {
        if (!validateFields()) return;

        long activityId = getArguments().getLong("activityId");
        int slotsRequested = Integer.parseInt(binding.editSlots.getText().toString());

        Log.d(TAG, "Intentando confirmar reserva: " + slotsRequested + " personas");

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
                    Log.d(TAG, "Reserva exitosa en server. Guardando local...");
                    Reserva reservaLocal = Reserva.fromResponse(response.body());
                    executor.execute(() -> reservaDao.insert(reservaLocal));
                    Toast.makeText(getContext(), "¡Reserva exitosa!", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) getActivity().getOnBackPressedDispatcher().onBackPressed();
                } else {
                    Log.e(TAG, "Error en reserva server. Código: " + response.code());
                    Toast.makeText(getContext(), "Error al realizar la reserva", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ReservationResponse> call, Throwable t) {
                Log.e(TAG, "Fallo de conexión al reservar: " + t.getMessage());
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateFields() {
        if (selectedDateFormatted.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(getContext(), "Seleccione fecha y hora", Toast.LENGTH_SHORT).show();
            return false;
        }
        String slotsStr = binding.editSlots.getText().toString();
        if (slotsStr.isEmpty() || Integer.parseInt(slotsStr) <= 0) {
            binding.layoutSlotsInput.setError("Ingrese cantidad válida");
            return false;
        }
        if (Integer.parseInt(slotsStr) > availableSlots) {
            Toast.makeText(getContext(), "No hay suficientes cupos (" + availableSlots + " disponibles)", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void updateTotalPrice(int count) {
        double total = count * activityPrice;
        binding.textTotalPrice.setText(String.format(Locale.getDefault(), "$%.2f", total));
    }

    private List<AvailabilitySlotResponse> allSlots = new ArrayList<>();

    private void loadAvailabilities(long activityId) {
        Log.d(TAG, "Cargando disponibilidad para ID: " + activityId);
        apiService.getAvailability(activityId).enqueue(new Callback<List<AvailabilitySlotResponse>>() {
            @Override
            public void onResponse(Call<List<AvailabilitySlotResponse>> call, Response<List<AvailabilitySlotResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AvailabilitySlotResponse> rawList = response.body();
                    Log.d(TAG, "Horarios recibidos del server: " + rawList.size());
                    
                    // Filtrar pasados y duplicados (Punto solicitado)
                    allSlots = filterAvailability(rawList);
                    Log.d(TAG, "Horarios después de filtrar (pasados/duplicados): " + allSlots.size());
                    
                    showDates();
                }
            }
            @Override
            public void onFailure(Call<List<AvailabilitySlotResponse>> call, Throwable t) {
                Log.e(TAG, "Error al cargar disponibilidad: " + t.getMessage());
            }
        });
    }

    private List<AvailabilitySlotResponse> filterAvailability(List<AvailabilitySlotResponse> list) {
        List<AvailabilitySlotResponse> filtered = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        String currentDateTimeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
        
        Set<String> seen = new LinkedHashSet<>();
        
        for (AvailabilitySlotResponse slot : list) {
            String key = slot.getDate() + " " + (slot.getTime().length() > 5 ? slot.getTime().substring(0, 5) : slot.getTime());
            
            // Filtro duplicados
            if (seen.contains(key)) continue;
            
            // Filtro pasados
            if (key.compareTo(currentDateTimeStr) >= 0) {
                filtered.add(slot);
                seen.add(key);
            }
        }
        return filtered;
    }

    private void showDates() {
        if (binding == null) return;
        binding.chipGroupDates.removeAllViews();
        Set<String> uniqueDates = new LinkedHashSet<>();
        for (AvailabilitySlotResponse slot : allSlots) uniqueDates.add(slot.getDate());

        for (String date : uniqueDates) {
            Chip chip = new Chip(requireContext());
            chip.setText(date);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false); // RESTORE: Sacar tilde (Punto solicitado)
            chip.setChipBackgroundColorResource(R.color.chip_selector_bg);
            chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_selector));
            chip.setOnClickListener(v -> {
                selectedDateFormatted = date;
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
        for (AvailabilitySlotResponse slot : allSlots) {
            if (slot.getDate().equals(date)) {
                Chip chip = new Chip(requireContext());
                String time = slot.getTime().length() >= 5 ? slot.getTime().substring(0, 5) : slot.getTime();
                chip.setText(time);
                chip.setCheckable(true);
                chip.setCheckedIconVisible(false); // RESTORE: Sacar tilde (Punto solicitado)
                chip.setChipBackgroundColorResource(R.color.chip_selector_bg);
                chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_selector));
                
                if (slot.getAvailableSlots() == 0) chip.setEnabled(false);
                
                chip.setOnClickListener(v -> {
                    selectedTime = time;
                    availableSlots = slot.getAvailableSlots();
                    binding.textAvailableSlots.setText("Cupos: " + availableSlots);
                });
                binding.chipGroupTimes.addView(chip);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
