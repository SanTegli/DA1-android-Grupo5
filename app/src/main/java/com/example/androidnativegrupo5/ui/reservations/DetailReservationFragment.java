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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.local.db.Reserva;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;
import com.example.androidnativegrupo5.data.model.Activity;
import com.example.androidnativegrupo5.data.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.data.model.RescheduleReservationRequest;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentManageReservationBinding;
import com.example.androidnativegrupo5.utils.NetworkUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
        }

        // Setup Mapa (Punto 10)
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_voucher);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadData();

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
            NavHostFragment.findNavController(this).navigate(R.id.action_ManageReservationFragment_to_RatingFragment, bundle);
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
            public void onResponse(@NonNull Call<List<ReservationResponse>> call, @NonNull Response<List<ReservationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (ReservationResponse r : response.body()) {
                        if (r.getId().equals(reservationId)) {
                            currentReservation = r;
                            fetchActivityCoords(r.getActivityId());
                            bindReservation(r);
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
        apiService.getActivityById(id).enqueue(new Callback<Activity>() {
            @Override
            public void onResponse(Call<Activity> call, Response<Activity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    activityDetail = response.body();
                    updateMapMarker();
                }
            }
            @Override
            public void onFailure(Call<Activity> call, Throwable t) {}
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
        Glide.with(requireContext())
                .load(reservation.getImageUrl())
                .placeholder(R.drawable.common_illustration_welcome_placeholder)
                .into(binding.imageReservationDetail);

        binding.textManageName.setText(reservation.getActivityName());
        binding.textManageDateTime.setText(reservation.getDate() + " - " + reservation.getTime());
        binding.textManagePeople.setText("Personas: " + reservation.getParticipants());
        binding.textManageTotalPrice.setText("Total: $" + reservation.getTotalPrice());
        
        String address = reservation.getMeetingPointAddress() != null ? reservation.getMeetingPointAddress() : "Consultar al llegar";
        binding.textMeetingAddress.setText(address);

        String status = reservation.getStatus();
        binding.textManageStatus.setText(formatStatus(status));
        applyStatusStyle(status);

        String n = status != null ? status.trim().toUpperCase() : "";
        boolean isCancelled = n.contains("CANCEL");
        boolean isFinished = n.contains("FINISH");
        boolean isOffline = !NetworkUtils.isOnline(requireContext());

        binding.btnCancelReservation.setVisibility((isCancelled || isFinished || isOffline) ? View.GONE : View.VISIBLE);
        binding.btnRescheduleReservation.setVisibility((isCancelled || isFinished || isOffline) ? View.GONE : View.VISIBLE);
        binding.btnRateReservation.setVisibility(isFinished ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        updateMapMarker();
    }

    private void updateMapMarker() {
        if (googleMap == null || activityDetail == null) return;
        Double lat = activityDetail.getMeetingPointLat();
        Double lng = activityDetail.getMeetingPointLng();
        if (lat != null && lng != null && lat != 0) {
            LatLng pos = new LatLng(lat, lng);
            googleMap.addMarker(new MarkerOptions().position(pos).title("Punto de encuentro"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
            binding.cardMeetingPoint.setVisibility(View.VISIBLE);
        } else {
            // Arreglo aquí: binding.mapVoucher ya no es un fragment directo en el binding si se usa <fragment> tag de forma estándar con ViewBinding
            // pero en el layout está como <fragment android:id="@+id/map_voucher" ... />
            // El error decía: cannot find symbol variable mapVoucher
            // Sin embargo, vi el layout y sí tiene el id.
            // Es probable que ViewBinding no genere el campo para <fragment> o lo genere diferente.
            // Si el error persiste, podemos intentar ocultar el contenedor o usar FragmentContainerView.
            View mapFragmentView = getChildFragmentManager().findFragmentById(R.id.map_voucher).getView();
            if (mapFragmentView != null) {
                mapFragmentView.setVisibility(View.GONE);
            }
        }
    }

    private void openMapsNavigation() {
        if (activityDetail == null || activityDetail.getMeetingPointLat() == null) {
            Toast.makeText(getContext(), "Ubicación no disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        String uri = String.format(Locale.US, "google.navigation:q=%f,%f", activityDetail.getMeetingPointLat(), activityDetail.getMeetingPointLng());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(intent);
        } catch (Exception e) {
            Intent generic = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + activityDetail.getMeetingPointLat() + "," + activityDetail.getMeetingPointLng()));
            startActivity(generic);
        }
    }

    private void showRescheduleDialog(ReservationResponse reservation) {
        apiService.getAvailability(reservation.getActivityId()).enqueue(new Callback<List<AvailabilitySlotResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AvailabilitySlotResponse>> call, @NonNull Response<List<AvailabilitySlotResponse>> response) {
                if (response.isSuccessful() && response.body() != null) renderRescheduleDialog(reservation, response.body());
            }
            @Override
            public void onFailure(@NonNull Call<List<AvailabilitySlotResponse>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error al cargar horarios", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderRescheduleDialog(ReservationResponse reservation, List<AvailabilitySlotResponse> slots) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_reschedule, null);
        ChipGroup cgDates = view.findViewById(R.id.chipGroupDatesReschedule);
        ChipGroup cgTimes = view.findViewById(R.id.chipGroupTimesReschedule);
        final String[] sDate = {""}; final String[] sTime = {""};
        Set<String> dates = new LinkedHashSet<>();
        for (AvailabilitySlotResponse s : slots) dates.add(s.getDate());
        for (String d : dates) {
            Chip c = new Chip(requireContext()); c.setText(d); c.setCheckable(true);
            c.setOnClickListener(v -> {
                sDate[0] = d; cgTimes.removeAllViews();
                for (AvailabilitySlotResponse s : slots) {
                    if (s.getDate().equals(d)) {
                        Chip ct = new Chip(requireContext()); ct.setText(s.getTime()); ct.setCheckable(true);
                        ct.setOnClickListener(vt -> sTime[0] = s.getTime());
                        cgTimes.addView(ct);
                    }
                }
            });
            cgDates.addView(c);
        }
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Reprogramar").setView(view)
                .setPositiveButton("Confirmar", (d, w) -> {
                    if (!sDate[0].isEmpty() && !sTime[0].isEmpty()) executeReschedule(reservation.getId(), sDate[0], sTime[0]);
                }).setNegativeButton("Cancelar", null).show();
    }

    private void executeReschedule(Long id, String d, String t) {
        RescheduleReservationRequest req = new RescheduleReservationRequest(d, t, currentReservation.getParticipants());
        apiService.rescheduleReservation(id, req).enqueue(new Callback<ReservationResponse>() {
            @Override public void onResponse(@NonNull Call<ReservationResponse> c, @NonNull Response<ReservationResponse> r) {
                if (r.isSuccessful()) { Toast.makeText(getContext(), "Reprogramada", Toast.LENGTH_SHORT).show(); loadReservation(); }
            }
            @Override public void onFailure(@NonNull Call<ReservationResponse> c, @NonNull Throwable t) { Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show(); }
        });
    }

    private void confirmCancelReservation() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancelar Reserva")
                .setMessage("¿Estás seguro de que deseas cancelar esta reserva? Esta acción no se puede deshacer.")
                .setPositiveButton("Sí, cancelar", (d, w) -> cancelReservation())
                .setNegativeButton("No, volver", null)
                .show();

        Button btnPos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (btnPos != null) btnPos.setTextColor(Color.parseColor("#7F0303"));
    }

    private void cancelReservation() {
        apiService.cancelReservation(reservationId).enqueue(new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {
                if (r.isSuccessful()) { 
                    Toast.makeText(getContext(), "Reserva cancelada", Toast.LENGTH_SHORT).show(); 
                    NavHostFragment.findNavController(DetailReservationFragment.this).popBackStack(); 
                }
            }
            @Override public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) { Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show(); }
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

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
