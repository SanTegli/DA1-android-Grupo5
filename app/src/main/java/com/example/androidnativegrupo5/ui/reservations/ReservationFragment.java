package com.example.androidnativegrupo5.ui.reservations;

import android.content.Intent;
import android.net.Uri;
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

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.local.db.Reserva;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;
import com.example.androidnativegrupo5.data.model.Activity;
import com.example.androidnativegrupo5.data.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.data.model.CreateReservationRequest;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentReservationBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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
public class ReservationFragment extends Fragment implements OnMapReadyCallback {

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
    private final Calendar calendar = Calendar.getInstance();

    private int availableSlots = 0;
    private String selectedDateFormatted = "";
    private String selectedTime = "";

    private List<AvailabilitySlotResponse> allSlots;

    private GoogleMap googleMap;
    private Activity activity;

    private long activityId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReservationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            activityId = getArguments().getLong("activityId", -1);
            activityName = getArguments().getString("activityName");
            activityPrice = getArguments().getFloat("activityPrice");
        }

        if (activityId == -1) {
            Toast.makeText(getContext(), "Error al cargar actividad", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.mapView.onCreate(savedInstanceState);
        binding.mapView.getMapAsync(this);

        binding.textActivityName.setText(activityName);

        if (activityPrice <= 0) {
            binding.textActivityPrice.setText(R.string.price_free);
        } else {
            binding.textActivityPrice.setText(String.format(Locale.getDefault(), "$%.2f", activityPrice));
        }

        updateTotalPrice(1);

        loadActivityDetail();
        loadAvailabilities();

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

    private void loadActivityDetail() {
        apiService.getActivityById(activityId).enqueue(new Callback<Activity>() {
            @Override
            public void onResponse(@NonNull Call<Activity> call,
                                   @NonNull Response<Activity> response) {
                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    activity = response.body();
                    setupMeetingPoint();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Activity> call,
                                  @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                Toast.makeText(getContext(), "No se pudo cargar el punto de encuentro", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupMeetingPoint() {
        if (activity == null || binding == null) return;

        String address = activity.getMeetingPointAddress();
        Double lat = activity.getMeetingPointLat();
        Double lng = activity.getMeetingPointLng();

        if (address == null || address.trim().isEmpty()) {
            binding.textMeetingPoint.setText("Punto de encuentro no disponible");
        } else {
            binding.textMeetingPoint.setText(address);
        }

        if (lat == null || lng == null) {
            binding.mapView.setVisibility(View.GONE);
            binding.btnHowToGetThere.setVisibility(View.GONE);
            return;
        }

        binding.mapView.setVisibility(View.VISIBLE);
        binding.btnHowToGetThere.setVisibility(View.VISIBLE);

        LatLng meetingPoint = new LatLng(lat, lng);

        if (googleMap != null) {
            googleMap.clear();

            googleMap.addMarker(new MarkerOptions()
                    .position(meetingPoint)
                    .title("Punto de encuentro"));

            binding.mapView.post(() ->
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(meetingPoint, 15f))
            );
        }

        binding.btnHowToGetThere.setOnClickListener(v -> openNavigation(lat, lng));
    }

    private void openNavigation(Double lat, Double lng) {
        Uri navigationUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
        Intent navigationIntent = new Intent(Intent.ACTION_VIEW, navigationUri);
        navigationIntent.setPackage("com.google.android.apps.maps");

        if (navigationIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(navigationIntent);
        } else {
            Uri browserUri = Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng
            );
            startActivity(new Intent(Intent.ACTION_VIEW, browserUri));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (activity != null) {
            setupMeetingPoint();
        }
    }

    private void confirmReservation() {
        if (!validateFields()) return;

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
            public void onResponse(@NonNull Call<ReservationResponse> call,
                                   @NonNull Response<ReservationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Reserva reservaLocal = Reserva.fromResponse(response.body());

                    executor.execute(() -> reservaDao.insert(reservaLocal));

                    Toast.makeText(getContext(), "¡Reserva exitosa!", Toast.LENGTH_SHORT).show();

                    if (getActivity() != null) {
                        getActivity().getOnBackPressedDispatcher().onBackPressed();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null
                                ? response.errorBody().string()
                                : "Error desconocido";

                        Log.e("RESERVA_ERROR", "Code: " + response.code() + " Body: " + errorBody);
                        Toast.makeText(getContext(), "Error al realizar la reserva", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReservationResponse> call,
                                  @NonNull Throwable t) {
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

    private void loadAvailabilities() {
        apiService.getAvailability(activityId).enqueue(new Callback<List<AvailabilitySlotResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AvailabilitySlotResponse>> call,
                                   @NonNull Response<List<AvailabilitySlotResponse>> response) {

                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    allSlots = response.body();
                    showDates();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AvailabilitySlotResponse>> call,
                                  @NonNull Throwable t) {
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
            chip.setTextColor(getResources().getColorStateList(R.color.chip_selector));

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

        if (allSlots != null) {
            for (AvailabilitySlotResponse slot : allSlots) {
                if (slot.getDate().equals(date)) {
                    Chip chip = new Chip(requireContext());
                    String time = slot.getTime().length() >= 5
                            ? slot.getTime().substring(0, 5)
                            : slot.getTime();

                    chip.setText(time);
                    chip.setCheckable(true);

                    chip.setChipBackgroundColorResource(R.color.chip_selector_bg);
                    chip.setTextColor(getResources().getColorStateList(R.color.chip_selector));

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

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) binding.mapView.onResume();
    }

    @Override
    public void onPause() {
        if (binding != null) binding.mapView.onPause();
        super.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (binding != null) binding.mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        if (binding != null) binding.mapView.onDestroy();
        super.onDestroyView();
        binding = null;
    }
}