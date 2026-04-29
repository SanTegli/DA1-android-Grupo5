package com.example.androidnativegrupo5.ui.reservations;

import android.app.AlertDialog;
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
import com.example.androidnativegrupo5.data.local.db.Reserva;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentMyReservationsBinding;
import com.example.androidnativegrupo5.utils.NetworkUtils;

import android.widget.ArrayAdapter;
import android.widget.Spinner;

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

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    @Inject
    ReservaDao reservaDao;

    private FragmentMyReservationsBinding binding;
    private ReservationAdapter adapter;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final List<ReservationResponse> allReservations = new ArrayList<>();
    private String selectedStatusFilter = null;

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

        adapter = new ReservationAdapter(new ArrayList<>(), this);
        binding.recyclerReservations.setAdapter(adapter);

        binding.btnFilterReservations.setOnClickListener(v -> showStatusFilterDialog());

        binding.btnGoHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_MyReservationsFragment_to_HistoryFragment)
        );

        if (!NetworkUtils.isOnline(requireContext())) {
            binding.layoutOfflineWarning.setVisibility(View.VISIBLE);
            binding.layoutOfflineWarning.setText("Estás sin conexión!");
            loadOfflineData();
        } else {
            binding.layoutOfflineWarning.setVisibility(View.GONE);
            loadReservations();
        }
    }

    private void applyStatusFilter() {
        if (adapter == null) return;

        // Si no hay filtro → mostrar todo
        if (selectedStatusFilter == null) {
            adapter.updateData(new ArrayList<>(allReservations));
            return;
        }

        List<ReservationResponse> filteredList = new ArrayList<>();

        for (ReservationResponse reservation : allReservations) {

            if (reservation == null) continue;

            // ⚠️ IMPORTANTE: este getter puede variar según tu modelo
            String rawStatus = reservation.getStatus(); // <-- SI ERROR, cambiar esto

            if (rawStatus == null) continue;

            String normalized = normalizeStatus(rawStatus);

            if (selectedStatusFilter.equals(normalized)) {
                filteredList.add(reservation);
            }
        }

        adapter.updateData(filteredList);
    }
    private void showStatusFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_reservation_filters, null);

        Spinner spinnerStatus = sheetView.findViewById(R.id.spinnerReservationStatus);
        View btnCloseFilters = sheetView.findViewById(R.id.btnCloseFilters);
        View btnClearFilters = sheetView.findViewById(R.id.btnClearFilters);
        View btnApplyFilters = sheetView.findViewById(R.id.btnApplyFilters);

        String[] options = {
                "Todas",
                "Confirmadas",
                "Finalizadas",
                "Canceladas"
        };

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                options
        );

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(spinnerAdapter);

        if ("CONFIRMED".equals(selectedStatusFilter)) {
            spinnerStatus.setSelection(1);
        } else if ("FINISHED".equals(selectedStatusFilter)) {
            spinnerStatus.setSelection(2);
        } else if ("CANCELLED".equals(selectedStatusFilter)) {
            spinnerStatus.setSelection(3);
        } else {
            spinnerStatus.setSelection(0);
        }

        btnCloseFilters.setOnClickListener(v -> dialog.dismiss());

        btnClearFilters.setOnClickListener(v -> {
            selectedStatusFilter = null;
            spinnerStatus.setSelection(0);
            applyStatusFilter();
            dialog.dismiss();
        });

        btnApplyFilters.setOnClickListener(v -> {
            int selectedPosition = spinnerStatus.getSelectedItemPosition();

            if (selectedPosition == 0) {
                selectedStatusFilter = null;
            } else if (selectedPosition == 1) {
                selectedStatusFilter = "CONFIRMED";
            } else if (selectedPosition == 2) {
                selectedStatusFilter = "FINISHED";
            } else if (selectedPosition == 3) {
                selectedStatusFilter = "CANCELLED";
            }

            applyStatusFilter();
            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private String normalizeStatus(String status) {
        if (status == null) return "";

        String value = status.trim().toUpperCase();

        if (value.equals("CONFIRMED") || value.equals("CONFIRMADO") || value.equals("CONFIRMADA")) {
            return "CONFIRMED";
        }

        if (value.equals("FINISHED") || value.equals("FINALIZADO") || value.equals("FINALIZADA")) {
            return "FINISHED";
        }

        if (value.equals("CANCELLED") || value.equals("CANCELED") || value.equals("CANCELADO") || value.equals("CANCELADA")) {
            return "CANCELLED";
        }

        return value;
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

                    allReservations.clear();
                    allReservations.addAll(response.body());

                    applyStatusFilter();
                } else {
                    Toast.makeText(getContext(), "Error al cargar reservas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ReservationResponse>> call,
                                  @NonNull Throwable t) {

                if (!isAdded() || binding == null) return;

                binding.layoutOfflineWarning.setVisibility(View.VISIBLE);
                binding.layoutOfflineWarning.setText("Server fuera de línea. Cargando datos locales...");
                loadOfflineData();
            }
        });
    }

    private void loadOfflineData() {
        executor.execute(() -> {
            List<Reserva> localData = reservaDao.getAllReservas();

            Log.d("OFFLINE_TEST", "Datos encontrados en Room: " + localData.size());

            List<ReservationResponse> uiList = new ArrayList<>();

            for (Reserva r : localData) {
                uiList.add(r.toResponse());
            }

            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || binding == null) return;

                binding.layoutOfflineWarning.setVisibility(View.VISIBLE);

                allReservations.clear();
                allReservations.addAll(uiList);

                applyStatusFilter();
            });
        });
    }

    @Override
    public void onDetailClick(ReservationResponse reservation) {
        if (reservation == null || reservation.getId() == null) {
            Toast.makeText(getContext(), "No se pudo abrir la reserva", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putLong("reservationId", reservation.getId());

        if (reservation.getActivityId() != null) {
            bundle.putLong("activityId", reservation.getActivityId());
        }

        bundle.putString("activityName", reservation.getActivityName());

        NavHostFragment.findNavController(this)
                .navigate(R.id.ManageReservationFragment, bundle);
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
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {

                        if (response.isSuccessful()) {
                            executor.execute(() -> {
                                reservaDao.clearPendingCancellation(r.getId());
                                Log.d("SYNC", "Reserva " + r.getId() + " sincronizada con el server.");
                            });
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call,
                                          @NonNull Throwable t) {
                        Log.e("SYNC", "Fallo al sincronizar reserva " + r.getId());
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}