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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.local.db.AvailabilityDao;
import com.example.androidnativegrupo5.data.local.db.CachedAvailability;
import com.example.androidnativegrupo5.data.local.db.Reserva;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;
import com.example.androidnativegrupo5.data.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.data.model.RescheduleReservationRequest;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentMyReservationsBinding;
import com.example.androidnativegrupo5.utils.NetworkUtils;

import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.androidnativegrupo5.utils.SyncManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class MyReservationsFragment extends Fragment implements ReservationAdapter.OnReservationActionListener {

    private static final String TAG = "MyReservationsFragment";

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;
    @Inject ReservaDao reservaDao;
    @Inject AvailabilityDao availabilityDao;

    private FragmentMyReservationsBinding binding;
    private ReservationAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final List<ReservationResponse> allReservations = new ArrayList<>();
    private String selectedStatusFilter = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMyReservationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Cargando pantalla de mis reservas");

        binding.recyclerReservations.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReservationAdapter(new ArrayList<>(), this);
        binding.recyclerReservations.setAdapter(adapter);

        binding.btnFilterReservations.setOnClickListener(v -> {
            Log.d(TAG, "Abriendo diálogo de filtros de reservas");
            showStatusFilterDialog();
        });

        binding.btnGoHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_MyReservationsFragment_to_HistoryFragment)
        );

        checkStatusAndLoad();
    }

    private void checkStatusAndLoad() {
        // Estrategia "Cache First": Cargamos siempre lo local primero
        loadOfflineData();

        if (!NetworkUtils.isOnline(requireContext())) {
            Log.w(TAG, "Sin conexión detectada. Mostrando aviso.");
            binding.layoutOfflineWarning.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "Conexión detectada. Intentando sincronizar.");
            binding.layoutOfflineWarning.setVisibility(View.GONE);
            loadReservations();
        }
    }

    private void loadReservations() {
        apiService.getMyReservations().enqueue(new Callback<List<ReservationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReservationResponse>> call, @NonNull Response<List<ReservationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Datos recibidos de API: " + response.body().size());
                    syncLocalDatabase(response.body());
                    prefetchAvailabilityForAll(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<ReservationResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Fallo de API, se mantiene lo cargado de Room: " + t.getMessage());
            }
        });
    }

    private void prefetchAvailabilityForAll(List<ReservationResponse> reservations) {
        for (ReservationResponse res : reservations) {
            if (res.getActivityId() != null) {
                apiService.getAvailability(res.getActivityId()).enqueue(new Callback<List<AvailabilitySlotResponse>>() {
                    @Override
                    public void onResponse(Call<List<AvailabilitySlotResponse>> call, Response<List<AvailabilitySlotResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            cacheAvailability(res.getActivityId(), response.body());
                        }
                    }
                    @Override public void onFailure(Call<List<AvailabilitySlotResponse>> call, Throwable t) {}
                });
            }
        }
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

    private void showStatusFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_reservation_filters, null);

        Spinner spinnerStatus = sheetView.findViewById(R.id.spinnerReservationStatus);
        View btnApplyFilters = sheetView.findViewById(R.id.btnApplyFilters);
        View btnCloseFilters = sheetView.findViewById(R.id.btnCloseFilters);
        View btnClearFilters = sheetView.findViewById(R.id.btnClearFilters);

        String[] options = {"Todas", "Confirmadas", "Finalizadas", "Canceladas"};

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                options
        );

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(spinnerAdapter);

        if (selectedStatusFilter == null) {
            spinnerStatus.setSelection(0);
        } else if (selectedStatusFilter.equals("CONFIRMED")) {
            spinnerStatus.setSelection(1);
        } else if (selectedStatusFilter.equals("FINISHED")) {
            spinnerStatus.setSelection(2);
        } else if (selectedStatusFilter.equals("CANCELLED")) {
            spinnerStatus.setSelection(3);
        }

        btnCloseFilters.setOnClickListener(v -> dialog.dismiss());

        btnClearFilters.setOnClickListener(v -> {
            selectedStatusFilter = null;
            spinnerStatus.setSelection(0);
            applyStatusFilter();
            dialog.dismiss();
        });

        btnApplyFilters.setOnClickListener(v -> {
            int pos = spinnerStatus.getSelectedItemPosition();

            switch (pos) {
                case 1:
                    selectedStatusFilter = "CONFIRMED";
                    break;
                case 2:
                    selectedStatusFilter = "FINISHED";
                    break;
                case 3:
                    selectedStatusFilter = "CANCELLED";
                    break;
                default:
                    selectedStatusFilter = null;
                    break;
            }

            applyStatusFilter();
            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void applyStatusFilter() {
        List<ReservationResponse> filtered = new ArrayList<>();
        for (ReservationResponse res : allReservations) {
            if (selectedStatusFilter == null) {
                filtered.add(res);
            } else {
                String status = res.getStatus() != null ? res.getStatus().toUpperCase() : "";
                if (status.contains(selectedStatusFilter)) filtered.add(res);
            }
        }
        adapter.updateData(filtered);
    }

    private void loadOfflineData() {
        executor.execute(() -> {
            List<Reserva> localData = reservaDao.getAllReservas();
            List<ReservationResponse> uiList = new ArrayList<>();
            for (Reserva r : localData) uiList.add(r.toResponse());
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    allReservations.clear();
                    allReservations.addAll(uiList);
                    applyStatusFilter();
                });
            }
        });
    }

    @Override
    public void onDetailClick(ReservationResponse reservation) {
        Bundle bundle = new Bundle();
        long resId = (reservation.getId() != null) ? reservation.getId() : 0L;
        long actId = (reservation.getActivityId() != null) ? reservation.getActivityId() : 0L;
        bundle.putLong("reservationId", resId);
        bundle.putLong("activityId", actId);
        NavHostFragment.findNavController(this).navigate(R.id.ManageReservationFragment, bundle);
    }

    private void syncLocalDatabase(List<ReservationResponse> remoteData) {
        executor.execute(() -> {
            for (ReservationResponse dto : remoteData) {
                Reserva local = reservaDao.getReservaById(dto.getId());
                if (local != null) {
                    if (local.isPendingSync()) {
                        Log.d(TAG, "Reenviando cambio local al backend: " + local.getId());

                        try {
                            apiService.rescheduleReservation(
                                    local.getId(),
                                    new RescheduleReservationRequest(
                                            local.getNewDate(),
                                            local.getNewTime(),
                                            local.getNewParticipants()
                                    )
                            ).execute();
                            local.setPendingSync(false);
                            reservaDao.update(local);
                        } catch (Exception e) {
                            Log.e(TAG, "Error reenviando cambios", e);
                        }
                        continue;
                    }

                    if (local.isPendingCancellation()) {
                        Log.d(TAG, "Reenviando cancelación: " + local.getId());
                        try {
                            apiService.cancelReservation(local.getId()).execute();
                            reservaDao.clearPendingCancellation(local.getId());
                        } catch (Exception e) {
                            Log.e(TAG, "Error cancelando", e);
                        }
                        continue;
                    }
                }
                reservaDao.insert(Reserva.fromResponse(dto));
            }

            Log.d(TAG, "Sync terminado");
            loadOfflineData();
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        checkStatusAndLoad();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
