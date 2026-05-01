package com.example.androidnativegrupo5.ui.reservations;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.graphics.Color;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.local.db.AvailabilityDao;
import com.example.androidnativegrupo5.data.local.db.CachedAvailability;
import com.example.androidnativegrupo5.data.local.db.Reserva;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;
import com.example.androidnativegrupo5.data.model.Activity;
import com.example.androidnativegrupo5.data.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.data.model.RescheduleReservationRequest;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentManageReservationBinding;
import com.example.androidnativegrupo5.utils.NetworkUtils;
import com.example.androidnativegrupo5.utils.SyncManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
public class DetailReservationFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "DetailResFragment";

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;
    @Inject ReservaDao reservaDao;
    @Inject AvailabilityDao availabilityDao;

    private FragmentManageReservationBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Long reservationId;
    private Long activityId;
    private ReservationResponse currentReservation;
    private Activity activityDetail;
    private GoogleMap googleMap;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentManageReservationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            reservationId = getArguments().getLong("reservationId");
            activityId = getArguments().getLong("activityId", 0);
            Log.d(TAG, "onViewCreated: Cargando reserva " + reservationId + " para actividad " + activityId);
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_voucher);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadData();

        binding.btnCancelReservation.setOnClickListener(v -> confirmCancelReservation());

        binding.btnRescheduleReservation.setOnClickListener(v -> {
            if (currentReservation == null) return;
            showRescheduleFlow();
        });

        binding.btnRateReservation.setOnClickListener(v -> {
            if (currentReservation == null) return;

            Bundle bundle = new Bundle();
            bundle.putLong("activityId", currentReservation.getActivityId());
            bundle.putString("activityName", currentReservation.getActivityName());

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_ManageReservationFragment_to_RatingFragment, bundle);
        });

        binding.btnGetDirections.setOnClickListener(v -> openMapsNavigation());
    }

    private void loadData() {
        if (!NetworkUtils.isOnline(requireContext())) {
            loadReservationOffline();
        } else {
            loadReservation();
        }
    }

    private void loadReservation() {
        apiService.getMyReservations().enqueue(new Callback<List<ReservationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReservationResponse>> call,
                                   @NonNull Response<List<ReservationResponse>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    for (ReservationResponse r : response.body()) {
                        if (r.getId().equals(reservationId)) {
                            currentReservation = r;
                            Log.d(TAG, "Reserva encontrada: " + r.getActivityName() + " ID: " + r.getId());

                            fetchActivityCoords(r.getActivityId());
                            bindReservation(r);

                            executor.execute(() -> reservaDao.insert(Reserva.fromResponse(r)));
                            return;
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ReservationResponse>> call, @NonNull Throwable t) {
                loadReservationOffline();
            }
        });
    }

    private void fetchActivityCoords(Long id) {
        if (id == null || id == 0) return;

        apiService.getActivityById(id).enqueue(new Callback<Activity>() {
            @Override
            public void onResponse(Call<Activity> call, Response<Activity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    activityDetail = response.body();
                    updateMapMarker();
                }
            }

            @Override
            public void onFailure(Call<Activity> call, Throwable t) {
            }
        });
    }

    private void loadReservationOffline() {
        executor.execute(() -> {
            Reserva local = reservaDao.getReservaById(reservationId);

            if (local != null) {
                currentReservation = local.toResponse();

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.textMapOffline.setVisibility(View.VISIBLE);
                        bindReservation(currentReservation);
                    });
                }
            }
        });
    }

    private void bindReservation(ReservationResponse reservation) {
        if (reservation == null || binding == null) return;

        Glide.with(requireContext())
                .load(reservation.getImageUrl())
                .placeholder(R.drawable.common_illustration_welcome_placeholder)
                .into(binding.imageReservationDetail);

        binding.textManageName.setText(reservation.getActivityName());
        binding.textManageDateTime.setText(reservation.getDate() + " - " + reservation.getTime());
        binding.textManagePeople.setText("Personas: " + reservation.getParticipants());
        binding.textManageTotalPrice.setText("Total: $" + reservation.getTotalPrice());

        String address = reservation.getMeetingPointAddress() != null
                ? reservation.getMeetingPointAddress()
                : "Consultar al llegar";

        binding.textMeetingAddress.setText(address);

        String status = reservation.getStatus();
        binding.textManageStatus.setText(formatStatus(status));
        applyStatusStyle(status);

        String n = status != null ? status.trim().toUpperCase() : "";
        boolean isCancelled = n.contains("CANCEL");
        boolean isFinished = n.contains("FINISH");

        binding.btnCancelReservation.setVisibility((isCancelled || isFinished) ? View.GONE : View.VISIBLE);
        binding.btnRescheduleReservation.setVisibility((isCancelled || isFinished) ? View.GONE : View.VISIBLE);

        // 🔥 DEBUG (CLAVE)
        Log.d(TAG, "Rating DEBUG -> activityScore: "
                + reservation.getActivityScore()
                + " | guideScore: "
                + reservation.getGuideScore()
                + " | comment: "
                + reservation.getRatingComment());

        boolean alreadyRated = reservation.getActivityScore() != null
                && reservation.getActivityScore() > 0;

        if (isFinished) {
            binding.btnRateReservation.setVisibility(View.VISIBLE);

            if (alreadyRated) {

                binding.btnRateReservation.setText("Ver calificación");

                // 🔥 MOSTRAR BLOQUE
                binding.layoutRatingResult.setVisibility(View.VISIBLE);

                binding.textRatingActivity.setText(
                        "Actividad: " + getStars(reservation.getActivityScore())
                );

                binding.textRatingGuide.setText(
                        "Guía: " + getStars(reservation.getGuideScore())
                );

                if (reservation.getRatingComment() != null
                        && !reservation.getRatingComment().isEmpty()) {

                    binding.textRatingComment.setVisibility(View.VISIBLE);
                    binding.textRatingComment.setText(
                            "\"" + reservation.getRatingComment() + "\""
                    );

                } else {
                    binding.textRatingComment.setVisibility(View.GONE);
                }

            } else {
                binding.layoutRatingResult.setVisibility(View.GONE);
                binding.btnRateReservation.setText("Calificar experiencia");
            }

        } else {
            binding.btnRateReservation.setVisibility(View.GONE);
            binding.layoutRatingResult.setVisibility(View.GONE);
        }
    }

    private String getStars(Integer score) {
        if (score == null) return "";

        StringBuilder stars = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            stars.append(i < score ? "★" : "☆");
        }

        return stars.toString();
    }
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        updateMapMarker();
    }

    private void updateMapMarker() {
        if (googleMap == null || activityDetail == null || binding == null) return;

        Double lat = activityDetail.getMeetingPointLat();
        Double lng = activityDetail.getMeetingPointLng();

        if (lat != null && lng != null && lat != 0) {
            LatLng pos = new LatLng(lat, lng);

            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(pos).title("Punto de encuentro"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));

            binding.cardMeetingPoint.setVisibility(View.VISIBLE);
        } else {
            if (getChildFragmentManager().findFragmentById(R.id.map_voucher) != null) {
                View mapFragmentView =
                        getChildFragmentManager().findFragmentById(R.id.map_voucher).getView();

                if (mapFragmentView != null) {
                    mapFragmentView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void openMapsNavigation() {
        if (activityDetail == null || activityDetail.getMeetingPointLat() == null) {
            Toast.makeText(getContext(), "Ubicación no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        String uri = String.format(
                Locale.US,
                "google.navigation:q=%f,%f",
                activityDetail.getMeetingPointLat(),
                activityDetail.getMeetingPointLng()
        );

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(intent);
        } catch (Exception e) {
            Intent generic = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("geo:" + activityDetail.getMeetingPointLat() + "," + activityDetail.getMeetingPointLng())
            );
            startActivity(generic);
        }
    }

    private void showRescheduleFlow() {
        if (!NetworkUtils.isOnline(requireContext())) {
            loadCachedAvailabilityAndShow();
        } else {
            showRescheduleDialog(currentReservation);
        }
    }

    private void showRescheduleDialog(ReservationResponse reservation) {
        apiService.getAvailability(reservation.getActivityId()).enqueue(new Callback<List<AvailabilitySlotResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AvailabilitySlotResponse>> call,
                                   @NonNull Response<List<AvailabilitySlotResponse>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    List<AvailabilitySlotResponse> slots = filterAvailability(response.body());

                    cacheAvailability(reservation.getActivityId(), slots);
                    renderRescheduleDialog(reservation, slots, false);
                } else {
                    loadCachedAvailabilityAndShow();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AvailabilitySlotResponse>> call,
                                  @NonNull Throwable t) {
                loadCachedAvailabilityAndShow();
            }
        });
    }

    private void cacheAvailability(Long activityId, List<AvailabilitySlotResponse> slots) {
        executor.execute(() -> {
            availabilityDao.deleteByActivityId(activityId);

            List<CachedAvailability> cachedList = new ArrayList<>();

            for (AvailabilitySlotResponse s : slots) {
                CachedAvailability c = new CachedAvailability();
                c.setActivityId(activityId);
                c.setDate(s.getDate());
                c.setTime(s.getTime());
                c.setAvailableSlots(s.getAvailableSlots());
                cachedList.add(c);
            }

            availabilityDao.insertAll(cachedList);
        });
    }

    private void loadCachedAvailabilityAndShow() {
        executor.execute(() -> {
            if (currentReservation == null) return;

            List<CachedAvailability> cached =
                    availabilityDao.getAvailabilityByActivityId(currentReservation.getActivityId());

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (cached == null || cached.isEmpty()) {
                        Toast.makeText(
                                getContext(),
                                "No hay horarios cacheados. Conéctate para ver disponibilidad.",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        List<AvailabilitySlotResponse> slots = new ArrayList<>();

                        for (CachedAvailability c : cached) {
                            slots.add(new AvailabilitySlotResponse(
                                    c.getDate(),
                                    c.getTime(),
                                    c.getAvailableSlots()
                            ));
                        }

                        slots = filterAvailability(slots);
                        renderRescheduleDialog(currentReservation, slots, true);
                    }
                });
            }
        });
    }

    private List<AvailabilitySlotResponse> filterAvailability(List<AvailabilitySlotResponse> list) {
        List<AvailabilitySlotResponse> filtered = new ArrayList<>();

        String currentDateTimeStr =
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

        Set<String> seen = new LinkedHashSet<>();

        for (AvailabilitySlotResponse slot : list) {
            if (slot.getDate() == null || slot.getTime() == null) continue;

            String time = slot.getTime().length() > 5
                    ? slot.getTime().substring(0, 5)
                    : slot.getTime();

            String key = slot.getDate() + " " + time;

            if (seen.contains(key)) continue;

            if (key.compareTo(currentDateTimeStr) >= 0) {
                filtered.add(slot);
                seen.add(key);
            }
        }

        return filtered;
    }

    private void renderRescheduleDialog(ReservationResponse reservation,
                                        List<AvailabilitySlotResponse> slots,
                                        boolean isOffline) {

        if (slots == null || slots.isEmpty()) {
            Toast.makeText(getContext(), "No hay horarios disponibles.", Toast.LENGTH_LONG).show();
            return;
        }

        // Filtrar futuros
        slots = filterAvailability(slots);

        if (slots.isEmpty()) {
            Toast.makeText(getContext(), "No hay horarios futuros disponibles.", Toast.LENGTH_LONG).show();
            return;
        }

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_reschedule, null);

        ChipGroup cgDates = view.findViewById(R.id.chipGroupDatesReschedule);
        ChipGroup cgTimes = view.findViewById(R.id.chipGroupTimesReschedule);
        EditText editSlots = view.findViewById(R.id.editSlotsReschedule);

        if (editSlots != null) {
            editSlots.setText(String.valueOf(reservation.getParticipants()));
        }

        final String[] sDate = {""};
        final String[] sTime = {""};

        // ===== DATES =====
        Set<String> dates = new LinkedHashSet<>();

        for (AvailabilitySlotResponse s : slots) {
            if (s == null || s.getDate() == null) continue;
            dates.add(s.getDate());
        }

        for (String d : dates) {
            Chip chipDate = new Chip(requireContext());
            chipDate.setText(d);
            chipDate.setCheckable(true);
            chipDate.setCheckedIcon(null);
            chipDate.setCheckedIconVisible(false);

            chipDate.setChipBackgroundColorResource(R.color.chip_selector_bg);
            chipDate.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_selector));

            final List<AvailabilitySlotResponse> finalSlots = slots;

            chipDate.setOnClickListener(v -> {
                sDate[0] = d;
                sTime[0] = "";
                cgTimes.removeAllViews();

                // ===== TIMES =====
                for (AvailabilitySlotResponse s : finalSlots) {
                    if (s == null || s.getDate() == null) continue;

                    if (s.getDate().equals(d)) {
                        Chip chipTime = new Chip(requireContext());

                        String time = s.getTime() != null && s.getTime().length() >= 5
                                ? s.getTime().substring(0, 5)
                                : s.getTime();

                        chipTime.setText(time);
                        chipTime.setCheckable(true);
                        chipTime.setCheckedIcon(null);
                        chipTime.setCheckedIconVisible(false);

                        chipTime.setChipBackgroundColorResource(R.color.chip_selector_bg);
                        chipTime.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_selector));

                        if (s.getAvailableSlots() == 0) {
                            chipTime.setEnabled(false);
                        }

                        chipTime.setOnClickListener(vt -> {
                            sTime[0] = time;
                        });

                        cgTimes.addView(chipTime);
                    }
                }
            });

            cgDates.addView(chipDate);
        }

        String title = isOffline ? "Reprogramar (Modo Offline)" : "Reprogramar";

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Confirmar", null)
                .setNegativeButton("Cancelar", null)
                .show();

        Button btnConfirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (btnConfirm != null) {
            btnConfirm.setTextColor(Color.parseColor("#7F0303"));

            btnConfirm.setOnClickListener(v -> {
                String slotsStr = editSlots != null ? editSlots.getText().toString() : "";

                if (sDate[0].isEmpty() || sTime[0].isEmpty()) {
                    Toast.makeText(getContext(), "Seleccione fecha y hora", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (slotsStr.isEmpty() || Integer.parseInt(slotsStr) <= 0) {
                    Toast.makeText(getContext(), "Ingrese cantidad de participantes", Toast.LENGTH_SHORT).show();
                    return;
                }

                executeReschedule(
                        reservation.getId(),
                        sDate[0],
                        sTime[0],
                        Integer.parseInt(slotsStr)
                );

                dialog.dismiss();
            });
        }

        if (btnCancel != null) {
            btnCancel.setTextColor(Color.parseColor("#7F0303"));
        }
    }

    private void executeReschedule(Long id, String d, String time, int slotsVal) {
        if (!NetworkUtils.isOnline(requireContext())) {
            saveRescheduleOffline(id, d, time, slotsVal);
            return;
        }

        RescheduleReservationRequest req =
                new RescheduleReservationRequest(d, time, slotsVal);

        apiService.rescheduleReservation(id, req).enqueue(new Callback<ReservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReservationResponse> call,
                                   @NonNull Response<ReservationResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), "¡Reprogramación exitosa!", Toast.LENGTH_SHORT).show();

                    currentReservation = response.body();
                    bindReservation(currentReservation);

                    executor.execute(() -> reservaDao.insert(Reserva.fromResponse(response.body())));
                } else {
                    if (response.code() == 400) {
                        Toast.makeText(getContext(), "No hay cupos disponibles", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "No se pudo modificar la reserva", Toast.LENGTH_SHORT).show();
                    }

                    saveRescheduleOffline(id, d, time, slotsVal);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReservationResponse> call,
                                  @NonNull Throwable t) {
                saveRescheduleOffline(id, d, time, slotsVal);
            }
        });
    }

    private void confirmCancelReservation() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancelar Reserva")
                .setMessage("¿Estás seguro?")
                .setPositiveButton("Sí, cancelar", (d, w) -> cancelReservation())
                .setNegativeButton("No, volver", null)
                .show();

        Button btnPos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button btnNeg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (btnPos != null) {
            btnPos.setTextColor(Color.parseColor("#7F0303"));
        }

        if (btnNeg != null) {
            btnNeg.setTextColor(Color.parseColor("#7F0303"));
        }
    }

    private void cancelReservation() {
        if (!NetworkUtils.isOnline(requireContext())) {
            markCancellationOffline();
            return;
        }

        apiService.cancelReservation(reservationId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(getContext(), "Reserva cancelada", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(DetailReservationFragment.this).popBackStack();
                } else {
                    markCancellationOffline();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) {
                markCancellationOffline();
            }
        });
    }

    private void markCancellationOffline() {
        executor.execute(() -> {
            reservaDao.markPendingCancellation(reservationId);

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Cancelación guardada offline", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(DetailReservationFragment.this).popBackStack();
                });
            }
        });
    }

    private void saveRescheduleOffline(Long id, String d, String t, int slotsVal) {
        executor.execute(() -> {
            Reserva local = reservaDao.getReservaById(id);

            if (local != null) {
                local.setNewDate(d);
                local.setNewTime(t);
                local.setNewParticipants(slotsVal);
                local.setPendingSync(true);
                reservaDao.update(local);

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Reprogramación guardada offline", Toast.LENGTH_SHORT).show();

                        currentReservation = local.toResponse();
                        bindReservation(currentReservation);
                    });
                }
            }
        });
    }

    private String formatStatus(String s) {
        if (s == null) return "Confirmada";

        String n = s.trim().toUpperCase();

        if (n.contains("CONFIRM")) return "Confirmada";
        if (n.contains("FINISH")) return "Finalizada";
        if (n.contains("CANCEL")) return "Cancelada";

        return s;
    }

    private void applyStatusStyle(String s) {
        String f = formatStatus(s);

        if (f.equals("Confirmada")) {
            binding.textManageStatus.setBackgroundResource(R.drawable.common_bg_status_confirmed);
            binding.textManageStatus.setTextColor(Color.parseColor("#2F7A7E"));
        } else if (f.equals("Finalizada")) {
            binding.textManageStatus.setBackgroundResource(R.drawable.common_bg_status_finished);
            binding.textManageStatus.setTextColor(Color.parseColor("#1E4DB7"));
        } else if (f.equals("Cancelada")) {
            binding.textManageStatus.setBackgroundResource(R.drawable.common_bg_status_cancelled);
            binding.textManageStatus.setTextColor(Color.parseColor("#B3261E"));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (SyncManager.needsRefresh) {
            loadData();
            SyncManager.needsRefresh = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}