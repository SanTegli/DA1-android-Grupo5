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
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.data.model.RescheduleReservationRequest;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentManageReservationBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class DetailReservationFragment extends Fragment {

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    private FragmentManageReservationBinding binding;

    private Long reservationId;
    private Long activityId;
    private ReservationResponse currentReservation;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentManageReservationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            reservationId = args.getLong("reservationId");
            if (args.containsKey("activityId")) {
                activityId = args.getLong("activityId");
            }
        }

        if (reservationId == null || reservationId == 0) {
            Toast.makeText(getContext(), "No se pudo abrir la reserva", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        loadReservation();

        binding.btnCancelReservation.setOnClickListener(v -> confirmCancelReservation());

        binding.btnRescheduleReservation.setOnClickListener(v -> {
            if (currentReservation == null) return;
            showRescheduleDialog(currentReservation);
        });

        binding.btnRateReservation.setOnClickListener(v -> {
            if (currentReservation == null) return;

            Bundle bundle = new Bundle();
            bundle.putLong("activityId", currentReservation.getActivityId());
            bundle.putString("activityName", currentReservation.getActivityName());

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_ManageReservationFragment_to_RatingFragment, bundle);
        });
    }

    private void loadReservation() {
        apiService.getMyReservations().enqueue(new Callback<List<ReservationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReservationResponse>> call,
                                   @NonNull Response<List<ReservationResponse>> response) {

                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    for (ReservationResponse reservation : response.body()) {
                        if (reservation.getId() != null && reservation.getId().equals(reservationId)) {
                            currentReservation = reservation;
                            bindReservation(reservation);
                            return;
                        }
                    }

                    Toast.makeText(getContext(), "Reserva no encontrada", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(DetailReservationFragment.this).popBackStack();

                } else {
                    Toast.makeText(getContext(), "Error al cargar la reserva", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ReservationResponse>> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindReservation(ReservationResponse reservation) {
        binding.textManageName.setText(safeText(reservation.getActivityName(), "Actividad"));

        String date = getStringValue(reservation,
                "getDate",
                "getReservationDate",
                "getAvailabilityDate",
                "getActivityDate");

        String time = getStringValue(reservation,
                "getTime",
                "getReservationTime",
                "getAvailabilityTime",
                "getActivityTime");

        if (!date.isEmpty() && !time.isEmpty()) {
            binding.textManageDateTime.setText(date + " - " + time);
        } else if (!date.isEmpty()) {
            binding.textManageDateTime.setText(date);
        } else {
            binding.textManageDateTime.setText("Fecha y horario no disponible");
        }

        binding.textManagePeople.setText("Personas: " + reservation.getParticipants());

        String status = getStringValue(reservation,
                "getStatus",
                "getReservationStatus");

        if (status.isEmpty()) {
            status = "Confirmada";
        }

        binding.textManageStatus.setText(formatStatus(status));

        String totalPrice = getStringValue(reservation,
                "getTotalPrice",
                "getTotal",
                "getPrice");

        if (!totalPrice.isEmpty()) {
            binding.textManageTotalPrice.setText("Total: $" + totalPrice);
        } else {
            binding.textManageTotalPrice.setText("Total no disponible");
        }

        boolean isCancelled = status.equalsIgnoreCase("CANCELLED")
                || status.equalsIgnoreCase("CANCELADA");

        binding.btnCancelReservation.setVisibility(isCancelled ? View.GONE : View.VISIBLE);
        binding.btnRescheduleReservation.setVisibility(isCancelled ? View.GONE : View.VISIBLE);
    }

    private void confirmCancelReservation() {
        if (currentReservation == null || currentReservation.getId() == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancelar reserva")
                .setMessage("¿Seguro que querés cancelar esta reserva?")
                .setPositiveButton("Sí, cancelar", (dialog, which) -> cancelReservation())
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelReservation() {
        if (currentReservation == null || currentReservation.getId() == null) return;

        String token = tokenManager.getToken();

        if (token == null) {
            Toast.makeText(getContext(), "Error de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.cancelReservation(currentReservation.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {

                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Reserva cancelada con éxito", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(DetailReservationFragment.this).popBackStack();
                } else {
                    Toast.makeText(getContext(), "Error al cancelar la reserva", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Fallo la conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRescheduleDialog(ReservationResponse reservation) {
        if (reservation.getActivityId() == null) {
            Toast.makeText(getContext(), "No se pudo obtener la actividad", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getAvailability(reservation.getActivityId()).enqueue(new Callback<List<AvailabilitySlotResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AvailabilitySlotResponse>> call,
                                   @NonNull Response<List<AvailabilitySlotResponse>> response) {

                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    renderRescheduleDialog(reservation, response.body());
                } else {
                    Toast.makeText(getContext(), "Error al cargar horarios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AvailabilitySlotResponse>> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error al cargar horarios", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderRescheduleDialog(ReservationResponse reservation,
                                        List<AvailabilitySlotResponse> slots) {

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_reschedule, null);

        ChipGroup cgDates = dialogView.findViewById(R.id.chipGroupDatesReschedule);
        ChipGroup cgTimes = dialogView.findViewById(R.id.chipGroupTimesReschedule);

        final String[] selectedDate = {""};
        final String[] selectedTime = {""};

        Set<String> uniqueDates = new LinkedHashSet<>();

        for (AvailabilitySlotResponse slot : slots) {
            uniqueDates.add(slot.getDate());
        }

        for (String date : uniqueDates) {
            Chip chipDate = new Chip(requireContext());
            chipDate.setText(date);
            chipDate.setCheckable(true);

            chipDate.setOnClickListener(v -> {
                selectedDate[0] = date;
                selectedTime[0] = "";
                cgTimes.removeAllViews();

                for (AvailabilitySlotResponse slot : slots) {
                    if (slot.getDate().equals(date)) {
                        Chip chipTime = new Chip(requireContext());
                        chipTime.setText(slot.getTime());
                        chipTime.setCheckable(true);

                        if (slot.getAvailableSlots() < reservation.getParticipants()) {
                            chipTime.setEnabled(false);
                        }

                        chipTime.setOnClickListener(vt -> selectedTime[0] = slot.getTime());

                        cgTimes.addView(chipTime);
                    }
                }
            });

            cgDates.addView(chipDate);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reprogramar reserva")
                .setView(dialogView)
                .setPositiveButton("Reprogramar", (dialog, which) -> {
                    if (selectedDate[0].isEmpty() || selectedTime[0].isEmpty()) {
                        Toast.makeText(getContext(), "Debe seleccionar fecha y hora", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    executeReschedule(
                            reservation.getId(),
                            selectedDate[0],
                            selectedTime[0],
                            reservation.getParticipants()
                    );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void executeReschedule(Long id, String date, String time, int participants) {
        String token = tokenManager.getToken();

        if (token == null) {
            Toast.makeText(getContext(), "Error: Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        RescheduleReservationRequest request =
                new RescheduleReservationRequest(date, time, participants);

        apiService.rescheduleReservation(id, request).enqueue(new Callback<ReservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReservationResponse> call,
                                   @NonNull Response<ReservationResponse> response) {

                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Reserva reprogramada con éxito", Toast.LENGTH_SHORT).show();
                    loadReservation();
                } else {
                    try {
                        String error = response.errorBody() != null
                                ? response.errorBody().string()
                                : "Error al reprogramar";

                        Log.e("RESCHEDULE_ERROR", error);
                        Toast.makeText(getContext(), "No se pudo reprogramar", Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error de disponibilidad", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReservationResponse> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Fallo la conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringValue(Object object, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = object.getClass().getMethod(methodName);
                Object value = method.invoke(object);

                if (value != null) {
                    return String.valueOf(value);
                }

            } catch (Exception ignored) {
            }
        }

        return "";
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value;
    }

    private String formatStatus(String status) {
        if (status == null) return "Confirmada";

        switch (status.toUpperCase()) {
            case "CONFIRMED":
                return "Confirmada";
            case "CANCELLED":
                return "Cancelada";
            case "PENDING":
                return "Pendiente";
            default:
                return status;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
