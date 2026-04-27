package com.example.androidnativegrupo5.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.databinding.FragmentFirstBinding;
import com.example.androidnativegrupo5.data.model.Activity;
import com.example.androidnativegrupo5.data.model.PaginatedResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.data.local.TokenManager;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    private FragmentFirstBinding binding;
    private ActivityAdapter adapter;
    private FeaturedActivityAdapter featuredAdapter;

    private int currentPage = 0;
    private static final int PAGE_SIZE = 3;

    private boolean isLoading = false;
    private boolean isLastPage = false;

    private String filterCategory = null;
    private String filterDestination = null;
    private Integer filterDuration = null;
    private Integer filterMinPrice = null;
    private Integer filterMaxPrice = null;
    private String filterSearch = null;

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

        setupFeaturedCarousel(navController);
        loadActivities();

        binding.btnViewAllActivities.setOnClickListener(v ->
                navController.navigate(R.id.ExploreActivitiesFragment)
        );

        binding.btnFilter.setOnClickListener(v -> {
            FilterBottomSheetDialogFragment bottomSheet = new FilterBottomSheetDialogFragment();

            bottomSheet.setOnFiltersAppliedListener((search, category, destination, minPrice, maxPrice) -> {

                filterSearch = search;
                filterCategory = category;
                filterDestination = destination;
                filterMinPrice = minPrice != null ? minPrice.intValue() : null;
                filterMaxPrice = maxPrice != null ? maxPrice.intValue() : null;

                currentPage = 0;
                isLastPage = false;
                adapter.clearActivities();
                loadActivities();

                if (filterCategory != null
                        || filterDestination != null
                        || filterMaxPrice != null
                        || (filterSearch != null && !filterSearch.isEmpty())) {
                    binding.layoutFeatured.setVisibility(View.GONE);
                } else {
                    binding.layoutFeatured.setVisibility(View.VISIBLE);
                }
            });

            bottomSheet.show(getParentFragmentManager(), "FilterBottomSheet");
        });

        binding.recyclerActivities.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) return;

                LinearLayoutManager layoutManager =
                        (LinearLayoutManager) recyclerView.getLayoutManager();

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
    }


    private void setupFeaturedCarousel(NavController navController) {
        featuredAdapter = new FeaturedActivityAdapter(activity -> {
            Bundle bundle = new Bundle();
            bundle.putLong("activityId", activity.getId());
            navController.navigate(R.id.action_FirstFragment_to_DetailFragment, bundle);
        });

        binding.recyclerFeatured.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        binding.recyclerFeatured.setAdapter(featuredAdapter);
        loadFeaturedActivities();
    }

    private void loadFeaturedActivities() {
        apiService.getActivities(0, 5, null, null, null, null, null)
                .enqueue(new Callback<PaginatedResponse<Activity>>() {
                    @Override
                    public void onResponse(@NonNull Call<PaginatedResponse<Activity>> call,
                                           @NonNull Response<PaginatedResponse<Activity>> response) {

                        if (!isAdded() || binding == null) return;

                        if (response.isSuccessful() && response.body() != null) {
                            List<Activity> activities = response.body().getContent();

                            if (activities != null && !activities.isEmpty()) {
                                featuredAdapter.setActivities(activities);
                                binding.layoutFeatured.setVisibility(View.VISIBLE);
                            } else {
                                binding.layoutFeatured.setVisibility(View.GONE);
                            }
                        } else {
                            binding.layoutFeatured.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PaginatedResponse<Activity>> call,
                                          @NonNull Throwable t) {
                        if (!isAdded() || binding == null) return;

                        Log.e("HomeFragment", "Error featured: " + t.getMessage());
                        binding.layoutFeatured.setVisibility(View.GONE);
                    }
                });
    }

    private void loadActivities() {
        isLoading = true;

        if (binding != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }

        apiService.getActivities(
                currentPage,
                PAGE_SIZE,
                filterCategory,
                filterDestination,
                filterMaxPrice,
                filterDuration,
                null
        ).enqueue(new Callback<PaginatedResponse<Activity>>() {

            @Override
            public void onResponse(@NonNull Call<PaginatedResponse<Activity>> call,
                                   @NonNull Response<PaginatedResponse<Activity>> response) {

                isLoading = false;

                if (!isAdded() || binding == null) return;

                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {

                    List<Activity> activities = response.body().getContent();

                    if (activities != null && !activities.isEmpty()) {

                        adapter.addActivities(activities);

                        if (currentPage >= response.body().getTotalPages() - 1) {
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

                Log.e("HomeFragment", "Error: " + t.getMessage());

                Toast.makeText(requireContext(),
                        "Error de conexión",
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