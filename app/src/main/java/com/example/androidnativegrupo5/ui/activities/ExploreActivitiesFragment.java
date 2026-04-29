package com.example.androidnativegrupo5.ui.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.Activity;
import com.example.androidnativegrupo5.data.model.PaginatedResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentExploreActivitiesBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class ExploreActivitiesFragment extends Fragment {

    @Inject
    ApiService apiService;

    private FragmentExploreActivitiesBinding binding;
    private ActivityAdapter adapter;

    private final List<Activity> allActivities = new ArrayList<>();

    private final List<String> dynamicCategories = new ArrayList<>();
    private final List<String> dynamicDestinations = new ArrayList<>();

    private String filterCategory = null;
    private String filterDestination = null;
    private Integer filterMinPrice = null;
    private Integer filterMaxPrice = null;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentExploreActivitiesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ActivityAdapter(activity -> {
            Bundle bundle = new Bundle();
            bundle.putLong("activityId", activity.getId());

            Navigation.findNavController(view)
                    .navigate(R.id.action_ExploreActivitiesFragment_to_DetailFragment, bundle);
        });

        binding.recyclerExploreActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerExploreActivities.setAdapter(adapter);

        binding.btnFilterExplore.setOnClickListener(clickedView -> {
            FilterBottomSheetDialogFragment bottomSheet = new FilterBottomSheetDialogFragment();

            bottomSheet.setFilterOptions(dynamicCategories, dynamicDestinations);

            bottomSheet.setCurrentFilters(
                    filterCategory,
                    filterDestination,
                    filterMinPrice != null ? filterMinPrice.floatValue() : 0f,
                    filterMaxPrice != null ? filterMaxPrice.floatValue() : 100000f
            );

            bottomSheet.setOnFiltersAppliedListener((category, destination, minPrice, maxPrice) -> {
                filterCategory = category;
                filterDestination = destination;
                filterMinPrice = minPrice != null ? minPrice.intValue() : null;
                filterMaxPrice = maxPrice != null ? maxPrice.intValue() : null;

                applyLocalFilters();
            });

            bottomSheet.show(getParentFragmentManager(), "ExploreFilterBottomSheet");
        });

        loadAllActivities();
    }

    private void loadAllActivities() {
        binding.progressExplore.setVisibility(View.VISIBLE);

        apiService.getActivities(0, 100, null, null, null, null, null)
                .enqueue(new Callback<PaginatedResponse<Activity>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<PaginatedResponse<Activity>> call,
                            @NonNull Response<PaginatedResponse<Activity>> response
                    ) {
                        if (!isAdded() || binding == null) return;

                        binding.progressExplore.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Activity> activities = response.body().getContent();

                            allActivities.clear();

                            if (activities != null) {
                                allActivities.addAll(activities);
                            }

                            buildDynamicFilterOptions();
                            applyLocalFilters();

                        } else {
                            Toast.makeText(requireContext(), "Error al cargar actividades", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<PaginatedResponse<Activity>> call,
                            @NonNull Throwable t
                    ) {
                        if (!isAdded() || binding == null) return;

                        binding.progressExplore.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void buildDynamicFilterOptions() {
        Set<String> categoriesSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> destinationsSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (Activity activity : allActivities) {
            String category = safe(activity.getCategory()).trim();
            String destination = safe(activity.getDestination()).trim();

            if (!category.isEmpty()) {
                categoriesSet.add(category);
            }

            if (!destination.isEmpty()) {
                destinationsSet.add(destination);
            }
        }

        dynamicCategories.clear();
        dynamicCategories.addAll(categoriesSet);

        dynamicDestinations.clear();
        dynamicDestinations.addAll(destinationsSet);
    }

    private void applyLocalFilters() {
        List<Activity> filtered = new ArrayList<>();

        for (Activity activity : allActivities) {
            if (!matchesCategory(activity)) continue;
            if (!matchesDestination(activity)) continue;
            if (!matchesPrice(activity)) continue;

            filtered.add(activity);
        }

        adapter.setActivities(filtered);

        if (binding != null) {
            binding.textEmptyExplore.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            binding.textResultsCount.setText(filtered.size() + " actividades encontradas");
        }
    }

    private boolean matchesCategory(Activity activity) {
        if (filterCategory == null || filterCategory.trim().isEmpty()) return true;
        return safe(activity.getCategory()).equalsIgnoreCase(filterCategory);
    }

    private boolean matchesDestination(Activity activity) {
        if (filterDestination == null || filterDestination.trim().isEmpty()) return true;
        return safe(activity.getDestination()).equalsIgnoreCase(filterDestination);
    }

    private boolean matchesPrice(Activity activity) {
        double price = activity.getPrice();

        if (filterMinPrice != null && price < filterMinPrice) return false;
        if (filterMaxPrice != null && price > filterMaxPrice) return false;

        return true;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}