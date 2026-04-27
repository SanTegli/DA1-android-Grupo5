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
import com.example.androidnativegrupo5.data.local.db.Reserva;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentMyReservationsBinding;
import com.example.androidnativegrupo5.utils.NetworkUtils;

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

        if (!NetworkUtils.isOnline(requireContext())) {
            binding.layoutOfflineWarning.setVisibility(View.VISIBLE);
            binding.layoutOfflineWarning.setText("Estás sin conexión!");
            loadOfflineData();
        } else {
            binding.layoutOfflineWarning.setVisibility(View.GONE);
            loadReservations();
        }

        binding.btnGoHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_MyReservationsFragment_to_HistoryFragment)
        );
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
                    adapter.updateData(response.body());
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
                adapter.updateData(uiList);
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