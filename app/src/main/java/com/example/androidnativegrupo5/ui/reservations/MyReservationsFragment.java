package com.example.androidnativegrupo5.ui.reservations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentMyReservationsBinding;
import com.example.androidnativegrupo5.utils.NetworkUtils;
import com.example.androidnativegrupo5.utils.SyncManager;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class MyReservationsFragment extends Fragment implements ReservationAdapter.OnReservationActionListener {

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;

    private FragmentMyReservationsBinding binding;
    private ReservationAdapter adapter;
    private final List<ReservationResponse> allReservations = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMyReservationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.recyclerReservations.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReservationAdapter(new ArrayList<>(), this);
        binding.recyclerReservations.setAdapter(adapter);

        if (NetworkUtils.isOnline(requireContext())) {
            loadReservations();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (SyncManager.needsRefresh) {
            loadReservations();
            SyncManager.needsRefresh = false;
        }
    }

    @Override
    public void onDetailClick(ReservationResponse reservation) {
        Bundle bundle = new Bundle();
        bundle.putLong("reservationId", reservation.getId());
        bundle.putLong("activityId", reservation.getActivityId());
        // Asegúrate de que este ID existe en tu nav_graph.xml
        NavHostFragment.findNavController(this).navigate(R.id.action_MyReservationsFragment_to_ManageReservationFragment, bundle);    }

    private void loadReservations() {
        String token = "Bearer " + tokenManager.getToken();
        apiService.getMyReservations(token).enqueue(new Callback<List<ReservationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReservationResponse>> call, @NonNull Response<List<ReservationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allReservations.clear();
                    allReservations.addAll(response.body());
                    adapter.updateData(allReservations);
                }
            }
            @Override public void onFailure(@NonNull Call<List<ReservationResponse>> call, @NonNull Throwable t) {}
        });
    }
}