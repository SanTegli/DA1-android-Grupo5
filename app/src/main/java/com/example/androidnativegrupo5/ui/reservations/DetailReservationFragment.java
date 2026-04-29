package com.example.androidnativegrupo5.ui.reservations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
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

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;

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
            activityId = args.getLong("activityId", 0);
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

    // =========================
    // CARGAR RESERVA
    // =========================

    private void loadReservation() {
        apiService.getMyReservations().enqueue(new Callback<List<ReservationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReservationResponse>> call,
                                   @NonNull Response<List<ReservationResponse>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    for (ReservationResponse r : response.body()) {
                        if (r.getId().equals(reservationId)) {
                            currentReservation = r;
                            bindReservation(r);
                            return;
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ReservationResponse>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================
    // BIND UI
    // =========================

    private void bindReservation(ReservationResponse reservation) {

        Glide.with(requireContext())
                .load(reservation.getImageUrl())
                .placeholder(R.drawable.common_illustration_welcome_placeholder)
                .error(R.drawable.common_illustration_welcome_placeholder)
                .into(binding.imageReservationDetail);

        binding.textManageName.setText(reservation.getActivityName());
        binding.textManageDateTime.setText(reservation.getDate() + " - " + reservation.getTime());
        binding.textManagePeople.setText("Personas: " + reservation.getParticipants());

        String status = reservation.getStatus();
        String normalized = status != null ? status.trim().toUpperCase() : "";

        binding.textManageStatus.setText(formatStatus(status));
        applyStatusStyle(status);
        binding.textManageTotalPrice.setText("Total: $" + reservation.getTotalPrice());

        boolean isCancelled = normalized.equals("CANCELLED") ||
                normalized.equals("CANCELED") ||
                normalized.equals("CANCELADO") ||
                normalized.equals("CANCELADA");

        boolean isFinished = normalized.equals("FINISHED") ||
                normalized.equals("FINALIZADO") ||
                normalized.equals("FINALIZADA");

        binding.btnCancelReservation.setVisibility((isCancelled || isFinished) ? View.GONE : View.VISIBLE);
        binding.btnRescheduleReservation.setVisibility((isCancelled || isFinished) ? View.GONE : View.VISIBLE);
        binding.btnRateReservation.setVisibility(isFinished ? View.VISIBLE : View.GONE);
    }

    // =========================
    // RESCHEDULE
    // =========================

    private void showRescheduleDialog(ReservationResponse reservation) {

        apiService.getAvailability(reservation.getActivityId())
                .enqueue(new Callback<List<AvailabilitySlotResponse>>() {

                    @Override
                    public void onResponse(@NonNull Call<List<AvailabilitySlotResponse>> call,
                                           @NonNull Response<List<AvailabilitySlotResponse>> response) {

                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(getContext(), "Error al cargar horarios", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        renderRescheduleDialog(reservation, response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<AvailabilitySlotResponse>> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
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
            chipDate.setCheckedIconVisible(false);

            chipDate.setOnClickListener(v -> {
                selectedDate[0] = date;
                cgTimes.removeAllViews();

                for (AvailabilitySlotResponse slot : slots) {
                    if (slot.getDate().equals(date)) {
                        Chip chipTime = new Chip(requireContext());
                        chipTime.setText(slot.getTime());
                        chipTime.setCheckable(true);
                        chipTime.setCheckedIconVisible(false);

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
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    if (selectedDate[0].isEmpty() || selectedTime[0].isEmpty()) {
                        Toast.makeText(getContext(), "Seleccioná fecha y hora", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    executeReschedule(reservation.getId(), selectedDate[0], selectedTime[0]);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void executeReschedule(Long id, String date, String time) {
        RescheduleReservationRequest request =
                new RescheduleReservationRequest(date, time, currentReservation.getParticipants());

        apiService.rescheduleReservation(id, request)
                .enqueue(new Callback<ReservationResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ReservationResponse> call,
                                           @NonNull Response<ReservationResponse> response) {

                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Reserva reprogramada", Toast.LENGTH_SHORT).show();
                            loadReservation();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ReservationResponse> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =========================
    // FORMAT STATUS
    // =========================

    private String formatStatus(String status) {
        if (status == null) return "Confirmada";

        switch (status.trim().toUpperCase()) {
            case "CONFIRMED":
            case "CONFIRMADED":
            case "CONFIRMADO":
            case "CONFIRMADA":
                return "Confirmada";

            case "FINISHED":
            case "FINALIZADO":
            case "FINALIZADA":
                return "Finalizada";

            case "CANCELLED":
            case "CANCELED":
            case "CANCELADO":
            case "CANCELADA":
                return "Cancelada";

            case "PENDING":
            case "PENDIENTE":
                return "Pendiente";

            default:
                return status;
        }
    }
    private void applyStatusStyle(String status) {
        String formattedStatus = formatStatus(status);

        switch (formattedStatus) {
            case "Confirmada":
                binding.textManageStatus.setBackgroundResource(R.drawable.common_bg_status_confirmed);
                binding.textManageStatus.setTextColor(Color.parseColor("#2F7A7E"));
                break;

            case "Finalizada":
                binding.textManageStatus.setBackgroundResource(R.drawable.common_bg_status_finished);
                binding.textManageStatus.setTextColor(Color.parseColor("#1E4DB7"));
                break;

            case "Cancelada":
                binding.textManageStatus.setBackgroundResource(R.drawable.common_bg_status_cancelled);
                binding.textManageStatus.setTextColor(Color.parseColor("#B3261E"));
                break;

            default:
                binding.textManageStatus.setBackgroundResource(R.drawable.common_bg_status_confirmed);
                binding.textManageStatus.setTextColor(Color.parseColor("#2F7A7E"));
                break;
        }
    }

    // =========================
    // CANCEL
    // =========================

    private void confirmCancelReservation() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancelar reserva")
                .setMessage("¿Seguro?")
                .setPositiveButton("Sí", (d, w) -> cancelReservation())
                .setNegativeButton("No", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(android.graphics.Color.parseColor("#7F0303"));

            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(android.graphics.Color.parseColor("#7F0303"));
        });

        dialog.show();
    }

    private void cancelReservation() {
        apiService.cancelReservation(currentReservation.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {

                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Cancelada", Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(DetailReservationFragment.this).popBackStack();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}