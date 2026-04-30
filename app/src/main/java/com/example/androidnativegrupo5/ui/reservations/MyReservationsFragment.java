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

    private static final String TAG = "MyReservationsFragment";

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;
    @Inject ReservaDao reservaDao;

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

        // Listener del botón de filtro (Punto solicitado)
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
        if (!NetworkUtils.isOnline(requireContext())) {
            Log.w(TAG, "Sin conexión. Cargando reservas locales.");
            binding.layoutOfflineWarning.setVisibility(View.VISIBLE);
            loadOfflineData();
        } else {
            binding.layoutOfflineWarning.setVisibility(View.GONE);
            loadReservations();
        }
    }

    private void loadReservations() {
        Log.d(TAG, "Solicitando reservas activas al servidor...");
        apiService.getMyReservations().enqueue(new Callback<List<ReservationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReservationResponse>> call, @NonNull Response<List<ReservationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Reservas recibidas: " + response.body().size());
                    allReservations.clear();
                    allReservations.addAll(response.body());
                    syncLocalDatabase(response.body());
                    applyStatusFilter();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<ReservationResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error al cargar reservas de la API: " + t.getMessage());
                loadOfflineData();
            }
        });
    }

    private void showStatusFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_reservation_filters, null);

        Spinner spinnerStatus = sheetView.findViewById(R.id.spinnerReservationStatus);
        View btnApplyFilters = sheetView.findViewById(R.id.btnApplyFilters);

        String[] options = {"Todas", "Confirmadas", "Finalizadas", "Canceladas"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(spinnerAdapter);

        btnApplyFilters.setOnClickListener(v -> {
            int pos = spinnerStatus.getSelectedItemPosition();
            switch (pos) {
                case 1: selectedStatusFilter = "CONFIRMED"; break;
                case 2: selectedStatusFilter = "FINISHED"; break;
                case 3: selectedStatusFilter = "CANCELLED"; break;
                default: selectedStatusFilter = null;
            }
            Log.d(TAG, "Filtro aplicado: " + (selectedStatusFilter != null ? selectedStatusFilter : "TODAS"));
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
        Log.d(TAG, "applyStatusFilter: Mostrando " + filtered.size() + " reservas filtradas");
        adapter.updateData(filtered);
    }

    private void loadOfflineData() {
        executor.execute(() -> {
            List<Reserva> localData = reservaDao.getAllReservas();
            Log.d(TAG, "loadOfflineData: Datos encontrados en Room: " + localData.size());
            List<ReservationResponse> uiList = new ArrayList<>();
            for (Reserva r : localData) uiList.add(r.toResponse());
            
            requireActivity().runOnUiThread(() -> {
                allReservations.clear();
                allReservations.addAll(uiList);
                applyStatusFilter();
            });
        });
    }

    @Override
    public void onDetailClick(ReservationResponse reservation) {
        Log.d(TAG, "Abriendo detalle para reserva: " + reservation.getId());
        Bundle bundle = new Bundle();
        bundle.putLong("reservationId", reservation.getId());
        bundle.putLong("activityId", reservation.getActivityId());
        NavHostFragment.findNavController(this).navigate(R.id.ManageReservationFragment, bundle);
    }

    private void syncLocalDatabase(List<ReservationResponse> remoteData) {
        executor.execute(() -> {
            reservaDao.deleteAll();
            for (ReservationResponse dto : remoteData) {
                reservaDao.insert(Reserva.fromResponse(dto));
            }
            Log.d(TAG, "syncLocalDatabase: Base de datos local actualizada.");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
