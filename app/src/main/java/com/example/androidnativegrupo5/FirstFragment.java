package com.example.androidnativegrupo5;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnativegrupo5.databinding.FragmentFirstBinding;
import com.example.androidnativegrupo5.model.Activity;
import com.example.androidnativegrupo5.model.PaginatedResponse;
import com.example.androidnativegrupo5.network.ApiService;
import com.example.androidnativegrupo5.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private ActivityAdapter adapter;
    private int currentPage = 0;
    private final int pageSize = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ActivityAdapter();
        binding.recyclerActivities.setAdapter(adapter);
        binding.recyclerActivities.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.recyclerActivities.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadActivities();
                    }
                }
            }
        });

        loadActivities();
    }

    private void loadActivities() {
        isLoading = true;
        binding.progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getActivities(currentPage, pageSize).enqueue(new Callback<PaginatedResponse<Activity>>() {
            @Override
            public void onResponse(@NonNull Call<PaginatedResponse<Activity>> call, @NonNull Response<PaginatedResponse<Activity>> response) {
                isLoading = false;
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && !response.body().getContent().isEmpty()) {
                    PaginatedResponse<Activity> paginatedResponse = response.body();
                    adapter.addActivities(paginatedResponse.getContent());
                    
                    if (currentPage >= paginatedResponse.getTotalPages() - 1) {
                        isLastPage = true;
                    } else {
                        currentPage++;
                    }
                } else {
                    // Si no hay datos en el servidor, cargamos datos de prueba para que veas el resultado
                    if (currentPage == 0) {
                        loadMockData();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<PaginatedResponse<Activity>> call, @NonNull Throwable t) {
                isLoading = false;
                binding.progressBar.setVisibility(View.GONE);
                Log.e("FirstFragment", "Error: " + t.getMessage());
                // Fallback a datos de prueba en caso de error de red para propósitos de demostración
                if (currentPage == 0) {
                    loadMockData();
                    Toast.makeText(getContext(), "Mostrando datos de prueba (Error de red)", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadMockData() {
        List<Activity> mockList = new ArrayList<>();
        
        Activity a1 = new Activity();
        a1.setName("Tour Histórico por el Centro");
        a1.setDestination("Cuzco, Perú");
        a1.setCategory("Cultura");
        a1.setDuration("3h");
        a1.setPrice(45.00);
        a1.setAvailableSlots(12);
        a1.setImageUrl("https://images.unsplash.com/photo-1526392060635-9d6019884377");
        
        Activity a2 = new Activity();
        a2.setName("Excursión a Machu Picchu");
        a2.setDestination("Aguas Calientes");
        a2.setCategory("Aventura");
        a2.setDuration("Full Day");
        a2.setPrice(120.00);
        a2.setAvailableSlots(5);
        a2.setImageUrl("https://images.unsplash.com/photo-1587593810167-a84920ea0781");

        Activity a3 = new Activity();
        a3.setName("Free Tour Nocturno");
        a3.setDestination("Lima, Perú");
        a3.setCategory("Free Tour");
        a3.setDuration("2h");
        a3.setPrice(0.00);
        a3.setAvailableSlots(20);
        a3.setImageUrl("https://images.unsplash.com/photo-1590050751217-23bb0f50bd52");

        mockList.add(a1);
        mockList.add(a2);
        mockList.add(a3);
        
        adapter.addActivities(mockList);
        isLastPage = true; // Detener paginación en mock data
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
