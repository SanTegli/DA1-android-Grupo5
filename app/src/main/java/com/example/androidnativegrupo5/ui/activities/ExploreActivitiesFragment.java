package com.example.androidnativegrupo5.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.Locale;
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

    private String searchText = "";
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

        binding.editSearchActivities.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                searchText = s != null ? s.toString() : "";
                applyLocalFilters();
            }
        });

        binding.btnFilterExplore.setOnClickListener(clickedView -> {
            FilterBottomSheetDialogFragment bottomSheet = new FilterBottomSheetDialogFragment();

            bottomSheet.setFilterOptions(dynamicCategories, dynamicDestinations);

            bottomSheet.setCurrentFilters(
                    searchText,
                    filterCategory,
                    filterDestination,
                    filterMinPrice != null ? filterMinPrice.floatValue() : 0f,
                    filterMaxPrice != null ? filterMaxPrice.floatValue() : 100000f
            );

            bottomSheet.setOnFiltersAppliedListener((search, category, destination, minPrice, maxPrice) -> {
                filterCategory = category;
                filterDestination = destination;
                filterMinPrice = minPrice != null ? minPrice.intValue() : null;
                filterMaxPrice = maxPrice != null ? maxPrice.intValue() : null;

                searchText = search != null ? search : "";

                if (binding != null) {
                    binding.editSearchActivities.setText(searchText);
                    binding.editSearchActivities.setSelection(binding.editSearchActivities.getText().length());
                }

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
                    public void onResponse(@NonNull Call<PaginatedResponse<Activity>> call,
                                           @NonNull Response<PaginatedResponse<Activity>> response) {

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
                    public void onFailure(@NonNull Call<PaginatedResponse<Activity>> call,
                                          @NonNull Throwable t) {

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

        String search = searchText != null
                ? searchText.toLowerCase(Locale.ROOT).trim()
                : "";

        for (Activity activity : allActivities) {
            if (!matchesSearch(activity, search)) continue;
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

    private boolean matchesSearch(Activity activity, String search) {
        if (search == null || search.isEmpty()) return true;

        String name = safe(activity.getName()).toLowerCase(Locale.ROOT);
        String description = safe(activity.getDescription()).toLowerCase(Locale.ROOT);
        String destination = safe(activity.getDestination()).toLowerCase(Locale.ROOT);
        String category = safe(activity.getCategory()).toLowerCase(Locale.ROOT);
        String guide = safe(activity.getGuideName()).toLowerCase(Locale.ROOT);

        return name.contains(search)
                || description.contains(search)
                || destination.contains(search)
                || category.contains(search)
                || guide.contains(search);
    }

    private boolean matchesCategory(Activity activity) {
        if (filterCategory == null || filterCategory.trim().isEmpty()) return true;
        return safe(activity.getCategory()).equalsIgnoreCase(filterCategory);
    }

    private boolean matchesDestination(Activity activity) {
        if (filterDestination == null || filterDestination.trim().isEmpty()) return true;
        return safe(activity.getDestination()).equalsIgnoreCase(filterDestination);
    }

    private String getCity(Activity activity) {
        String destination = safe(activity.getDestination());

        if (destination.contains("-")) {
            return destination.split("-")[0].trim();
        }

        return destination.trim();
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