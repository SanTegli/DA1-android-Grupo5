package com.example.androidnativegrupo5;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnativegrupo5.databinding.FragmentFirstBinding;
import com.example.androidnativegrupo5.model.Activity;
import com.example.androidnativegrupo5.model.PaginatedResponse;
import com.example.androidnativegrupo5.model.ReservationResponse;
import com.example.androidnativegrupo5.network.ApiService;
import com.example.androidnativegrupo5.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private ActivityAdapter adapter;

    private int currentPage = 0;
    private static final int PAGE_SIZE = 10;

    private boolean isLoading = false;
    private boolean isLastPage = false;

    private String filterCategory = null;
    private String filterDestination = null;
    private String filterDuration = null;
    private Double filterMaxPrice = null;
    private boolean isInitialSelect = true;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = Navigation.findNavController(view);

        adapter = new ActivityAdapter(activity -> {
            Bundle bundle = new Bundle();
            bundle.putLong("activityId", activity.getId());
            navController.navigate(R.id.action_FirstFragment_to_DetailFragment, bundle);
        });

        binding.recyclerActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerActivities.setAdapter(adapter);

        setupFilters();

        // Scroll infinito
        binding.recyclerActivities.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) return;

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null || isLoading || isLastPage) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= PAGE_SIZE) {
                    loadActivities();
                }
            }
        });

        binding.btnMyReservations.setOnClickListener(v -> {
            Navigation.findNavController(v)
                    .navigate(R.id.action_FirstFragment_to_MyReservationsFragment);
        });
    }

    private void applyFilters() {
        if (binding == null) return;

        filterCategory = binding.spinnerCategory.getSelectedItem().toString();
        if (filterCategory.equals("Categorías")) filterCategory = null;

        filterDestination = binding.spinnerDestination.getSelectedItem().toString();
        if (filterDestination.equals("Destinos")) filterDestination = null;

        filterDuration = binding.spinnerDuration.getSelectedItem().toString();
        if (filterDuration.equals("Duración")) filterDuration = null;

        String selectedPrice = binding.spinnerPrice.getSelectedItem().toString();
        filterMaxPrice = mapPriceRange(selectedPrice);

        currentPage = 0;
        isLastPage = false;
        adapter.clearActivities();
        loadActivities();
    }

    private void setupFilters() {
            String[] categories = {"Categorías", "Aventura", "Cultura", "Gastronomía", "Naturaleza"};
            String[] destinations = {"Destinos", "Mendoza", "Bariloche", "Buenos Aires", "Iguazú"};
            String[] durations = {"Duración", "1-2 horas", "Media jornada", "Día completo"};
            String[] prices = {"Precio", "Hasta $5000", "Hasta $10000", "Hasta $20000"};

            setupSpinner(binding.spinnerCategory, categories);
            setupSpinner(binding.spinnerDestination, destinations);
            setupSpinner(binding.spinnerDuration, durations);
            setupSpinner(binding.spinnerPrice, prices);

            AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isInitialSelect) {
                        return;
                    }
                    applyFilters();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };

            binding.spinnerCategory.setOnItemSelectedListener(itemSelectedListener);
            binding.spinnerDestination.setOnItemSelectedListener(itemSelectedListener);
            binding.spinnerDuration.setOnItemSelectedListener(itemSelectedListener);
            binding.spinnerPrice.setOnItemSelectedListener(itemSelectedListener);

            isInitialSelect = false;
            loadActivities();
        }

        private void setupSpinner(Spinner spinner, String[] items) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }

        private Double mapPriceRange(String selectedPrice) {
            switch (selectedPrice) {
                case "Hasta $5000": return 5000.0;
                case "Hasta $10000": return 10000.0;
                case "Hasta $20000": return 20000.0;
                default: return null;
            }
        }

    private void loadActivities() {
        isLoading = true;
        binding.progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        apiService.getActivities(currentPage, PAGE_SIZE, filterCategory, filterDestination, filterDuration, null, filterMaxPrice)
                .enqueue(new Callback<PaginatedResponse<Activity>>() {

                    @Override
                    public void onResponse(@NonNull Call<PaginatedResponse<Activity>> call,
                                           @NonNull Response<PaginatedResponse<Activity>> response) {

                        isLoading = false;
                        binding.progressBar.setVisibility(View.GONE);

                        if (!isAdded() || binding == null) return;

                        if (response.isSuccessful() && response.body() != null) {

                            PaginatedResponse<Activity> paginatedResponse = response.body();
                            List<Activity> activities = paginatedResponse.getContent();

                            if (activities != null && !activities.isEmpty()) {

                                adapter.addActivities(activities);

                                if (currentPage >= paginatedResponse.getTotalPages() - 1) {
                                    isLastPage = true;
                                } else {
                                    currentPage++;
                                }

                            } else {
                                if (currentPage == 0) {
                                    Toast.makeText(requireContext(),
                                            "No se encontraron actividades",
                                            Toast.LENGTH_SHORT).show();
                                }
                                isLastPage = true;
                            }

                        } else {
                            Log.e("FirstFragment", "Error code: " + response.code());
                            Toast.makeText(requireContext(),
                                    "Error al obtener actividades",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PaginatedResponse<Activity>> call,
                                          @NonNull Throwable t) {

                        isLoading = false;

                        if (!isAdded() || binding == null) return;

                        binding.progressBar.setVisibility(View.GONE);

                        Log.e("FirstFragment", "Error: " + t.getMessage(), t);

                        Toast.makeText(requireContext(),
                                "Error de conexión con el servidor",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}