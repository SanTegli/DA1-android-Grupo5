package com.example.androidnativegrupo5.ui.reservations;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.db.Reserva;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;
import com.example.androidnativegrupo5.data.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.data.model.CreateRatingRequest;
import com.example.androidnativegrupo5.data.model.Rating;
import com.example.androidnativegrupo5.data.model.RescheduleReservationRequest;
import com.example.androidnativegrupo5.databinding.FragmentMyReservationsBinding;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.utils.NetworkUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class MyReservationsFragment extends Fragment implements ReservationAdapter.OnReservationActionListener {

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    @Inject
    ReservaDao reservaDao;

    private FragmentMyReservationsBinding binding;
    private ReservationAdapter adapter;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentMyReservationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerReservations.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (adapter == null) {
            adapter = new ReservationAdapter(new ArrayList<>(), this);
        }
        binding.recyclerReservations.setAdapter(adapter);

        if (!NetworkUtils.isOnline(requireContext())) {
            binding.layoutOfflineWarning.setVisibility(View.VISIBLE);
            binding.layoutOfflineWarning.setText("Estás sin conexión!");
            loadOfflineData();
        } else {
            binding.layoutOfflineWarning.setVisibility(View.GONE);
            loadReservations();
        }

        binding.btnGoHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_MyReservationsFragment_to_HistoryFragment));
    }

    private void loadReservations() {
        String token = tokenManager.getToken();

        if (token == null) {
            Toast.makeText(getContext(), "Inicie sesión para ver sus reservas", Toast.LENGTH_SHORT).show();
            return;
        }

        syncPendingChanges();

        apiService.getMyReservations().enqueue(new Callback<List<ReservationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReservationResponse>> call,
                                   @NonNull Response<List<ReservationResponse>> response) {

                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    syncLocalDatabase(response.body());
                    adapter = new ReservationAdapter(response.body(), MyReservationsFragment.this);
                    binding.recyclerReservations.setAdapter(adapter);
                } else {
                    Toast.makeText(getContext(), "Error al cargar reservas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ReservationResponse>> call, @NonNull Throwable t) {
                if (isAdded() && binding != null) {
                    binding.layoutOfflineWarning.setVisibility(View.VISIBLE);
                    binding.layoutOfflineWarning.setText("Server fuera de línea. Cargando datos locales...");
                    loadOfflineData();
                }
                else {
                    Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onCancelClick(ReservationResponse reservation) {
        if (reservation.getId() == null) return;

        String token = tokenManager.getToken();

        if (token == null) {
            Toast.makeText(getContext(), "Error de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chequeo si está offline
        if (!NetworkUtils.isOnline(requireContext())) {
            executor.execute(() -> {
                reservaDao.markAsCancelledOffline(reservation.getId());
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Modo offline: La reserva se cancelará al recuperar conexión", Toast.LENGTH_LONG).show();
                    loadOfflineData();
                });
            });
            return;
        }

        apiService.cancelReservation(reservation.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Reserva cancelada con éxito", Toast.LENGTH_SHORT).show();
                    executor.execute(() -> reservaDao.markAsCancelledOffline(reservation.getId()));
                    loadReservations();
                } else {
                    Toast.makeText(getContext(), "Error al cancelar la reserva", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                executor.execute(() -> {
                    reservaDao.markAsCancelledOffline(reservation.getId());
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Server offline. Guardado localmente.", Toast.LENGTH_SHORT).show();
                        loadOfflineData();
                    });
                });
            }
        });
    }

    @Override
    public void onRateClick(ReservationResponse reservation) {
        showRatingDialog(reservation);
    }

    private void showRatingDialog(ReservationResponse reservation) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_rating, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        RatingBar ratingActivity = dialogView.findViewById(R.id.rating_activity);
        RatingBar ratingGuide = dialogView.findViewById(R.id.rating_guide);
        TextInputEditText etComment = dialogView.findViewById(R.id.et_comment);
        Button btnSubmit = dialogView.findViewById(R.id.btn_submit);

        btnSubmit.setOnClickListener(v -> {
            int activityScore = (int) ratingActivity.getRating();
            int guideScore = (int) ratingGuide.getRating();
            String comment = etComment.getText().toString();

            if (activityScore == 0 || guideScore == 0) {
                Toast.makeText(getContext(), "Por favor, califique ambos aspectos", Toast.LENGTH_SHORT).show();
                return;
            }

            submitRating(reservation.getActivityId(), activityScore, guideScore, comment, dialog);
        });

        dialog.show();
    }

    private void submitRating(Long activityId, int activityScore, int guideScore, String comment, AlertDialog dialog) {
        String token = tokenManager.getToken();

        if (token == null) return;

        CreateRatingRequest request =
                new CreateRatingRequest(activityScore, guideScore, comment);

        apiService.createRating(activityId, request).enqueue(new Callback<Rating>() {
            @Override
            public void onResponse(@NonNull Call<Rating> call,
                                   @NonNull Response<Rating> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "¡Gracias por tu calificación!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    // Optional: mark as rated in UI if needed
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Error al enviar calificación";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Rating> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadOfflineData() {
        executor.execute(() -> {
            List<Reserva> localData = reservaDao.getAllReservas();
            Log.d("OFFLINE_TEST", "Datos encontrados en Room: " + localData.size());

            java.util.List<ReservationResponse> uiList = new java.util.ArrayList<>();
            for (Reserva r : localData) {
                uiList.add(r.toResponse());
            }

            requireActivity().runOnUiThread(() -> {

                if (!isAdded() || binding == null) return;

                binding.layoutOfflineWarning.setVisibility(View.VISIBLE);

                if (adapter == null) {
                    adapter = new ReservationAdapter(uiList, MyReservationsFragment.this);
                    binding.recyclerReservations.setAdapter(adapter);
                } else {
                    adapter.updateData(uiList);
                }
            });
        });
    }

    @Override
    public void onDetailClick(ReservationResponse reservation) {
        if (reservation == null || reservation.getActivityId() == null) {
            Toast.makeText(getContext(), "No se pudo abrir el detalle", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putLong("activityId", reservation.getActivityId());
        bundle.putString("activityName", reservation.getActivityName());

        NavHostFragment.findNavController(this)
                .navigate(R.id.DetailFragment, bundle);
    }

    private void syncLocalDatabase(List<ReservationResponse> remoteData) {
        executor.execute(() -> {
            reservaDao.deleteAll();
            for (ReservationResponse dto : remoteData) {
                reservaDao.insert(Reserva.fromResponse(dto));
            }
        });
    }

    private void syncPendingChanges() {
        executor.execute(() -> {
            List<Reserva> pendings = reservaDao.getPendingCancellations();
            for (Reserva r : pendings) {
                apiService.cancelReservation(r.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            executor.execute(() -> {
                                reservaDao.clearPendingCancellation(r.getId());
                                Log.d("SYNC", "Reserva " + r.getId() + " sincronizada con el server.");
                            });
                        }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("SYNC", "Fallo al sincronizar reserva " + r.getId());
                    }
                });
            }
        });
    }

    @Override
    public void onRescheduleClick(ReservationResponse reservation) {
        Log.d("RESCHEDULE", "Iniciando para: " + reservation.getActivityName());
        showRescheduleDialog(reservation);
    }

    private void showRescheduleDialog(ReservationResponse reservation) {
        apiService.getAvailability(reservation.getActivityId()).enqueue(new Callback<List<AvailabilitySlotResponse>>() {
            @Override
            public void onResponse(Call<List<AvailabilitySlotResponse>> call, Response<List<AvailabilitySlotResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderRescheduleDialog(reservation, response.body());
                }
            }
            @Override
            public void onFailure(Call<List<AvailabilitySlotResponse>> call, Throwable t) {
                Toast.makeText(getContext(), "Error al cargar horarios", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderRescheduleDialog(ReservationResponse reservation, List<AvailabilitySlotResponse> slots) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_reschedule, null);
        ChipGroup cgDates = view.findViewById(R.id.chipGroupDatesReschedule);
        ChipGroup cgTimes = view.findViewById(R.id.chipGroupTimesReschedule);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Reprogramar Actividad");
        builder.setView(view);

        final String[] tempDate = {""};
        final String[] tempTime = {""};

        Set<String> uniqueDates = new LinkedHashSet<>();
        for (AvailabilitySlotResponse s : slots) uniqueDates.add(s.getDate());

        for (String date : uniqueDates) {
            Chip chip = new Chip(requireContext());
            chip.setText(date);
            chip.setCheckable(true);
            chip.setOnClickListener(v -> {
                tempDate[0] = date;
                cgTimes.removeAllViews();
                for (AvailabilitySlotResponse s : slots) {
                    if (s.getDate().equals(date)) {
                        Chip tChip = new Chip(requireContext());
                        tChip.setText(s.getTime());
                        tChip.setCheckable(true);
                        if (s.getAvailableSlots() < reservation.getParticipants()) tChip.setEnabled(false);
                        tChip.setOnClickListener(vt -> tempTime[0] = s.getTime());
                        cgTimes.addView(tChip);
                    }
                }
            });
            cgDates.addView(chip);
        }

        builder.setPositiveButton("Reprogramar", (dialog, which) -> {
            if (!tempDate[0].isEmpty() && !tempTime[0].isEmpty()) {
                executeReschedule(reservation.getId(), tempDate[0], tempTime[0], reservation.getParticipants());
            } else {
                Toast.makeText(getContext(), "Debe seleccionar fecha y hora", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void executeReschedule(Long id, String date, String time, int participants) {
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "Error: Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        RescheduleReservationRequest request = new RescheduleReservationRequest(date, time, participants);

        apiService.rescheduleReservation(id, request).enqueue(new Callback<ReservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReservationResponse> call, @NonNull Response<ReservationResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "¡Reserva reprogramada con éxito!", Toast.LENGTH_SHORT).show();

                    loadReservations();
                } else {
                    try {
                        String error = response.errorBody() != null ? response.errorBody().string() : "Error al reprogramar";
                        Log.e("RESCHEDULE_ERROR", error);
                        Toast.makeText(getContext(), "No se pudo reprogramar: " + error, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error de disponibilidad", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReservationResponse> call, @NonNull Throwable t) {
                Log.e("NETWORK_ERROR", t.getMessage());
                Toast.makeText(getContext(), "Fallo la conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
