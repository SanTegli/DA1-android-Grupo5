package com.example.androidnativegrupo5.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.model.Activity;
import com.example.androidnativegrupo5.data.model.NewsItem;
import com.example.androidnativegrupo5.data.model.PaginatedResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentFirstBinding;
import com.google.gson.Gson;
import com.example.androidnativegrupo5.databinding.FragmentFirstBinding;

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
public class HomeFragment extends Fragment {

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    private FragmentFirstBinding binding;
    private ActivityAdapter adapter;
    private FeaturedActivityAdapter featuredAdapter;
    private NewsAdapter newsAdapter;

    private final List<Activity> allActivities = new ArrayList<>();

    private final List<String> dynamicCategories = new ArrayList<>();
    private final List<String> dynamicDestinations = new ArrayList<>();

    private String searchText = "";
    private String filterCategory = null;
    private String filterDestination = null;
    private Integer filterDuration = null;
    private Integer filterMinPrice = null;
    private Integer filterMaxPrice = null;
    private String filterSearch = null;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
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

        setupNewsSection();
        setupFeaturedCarousel(navController);

        binding.editSearchActivities.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                searchText = s != null ? s.toString() : "";
                applyLocalFilters();
            }
        });

        binding.btnViewAllActivities.setOnClickListener(v ->
                navController.navigate(R.id.ExploreActivitiesFragment)
        );

        binding.btnFilter.setOnClickListener(v -> {
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

                if (binding != null) {
                    binding.editSearchActivities.setText(searchText);
                    binding.editSearchActivities.setSelection(binding.editSearchActivities.getText().length());
                }

                applyLocalFilters();
                updateFeaturedVisibility();
            });

            bottomSheet.show(getParentFragmentManager(), "HomeFilterBottomSheet");
        });

        loadAllActivities();
    }

    private void setupNewsSection() {
        newsAdapter = new NewsAdapter(news -> {
            if (news.getLinkUrl() != null && !news.getLinkUrl().isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.getLinkUrl()));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.recyclerNews.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.recyclerNews.setAdapter(newsAdapter);
        loadNews();
    }

    private void loadNews() {
        apiService.getNews().enqueue(new Callback<List<NewsItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<NewsItem>> call, @NonNull Response<List<NewsItem>> response) {
                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    Log.d("API_NEWS", "Noticias recibidas: " + response.body().size());
                    if (!response.body().isEmpty()) {
                        newsAdapter.setNewsList(response.body());
                        binding.layoutNews.setVisibility(View.VISIBLE);
                    } else {
                        binding.layoutNews.setVisibility(View.GONE);
                    }
                } else {
                    Log.e("API_NEWS", "Error en respuesta: " + response.code());
                    binding.layoutNews.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<NewsItem>> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                Log.e("API_NEWS", "Fallo total: " + t.getMessage());
                binding.layoutNews.setVisibility(View.GONE);
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

    private void loadAllActivities() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }

        apiService.getActivities(0, 100, null, null, null, null, null)
                .enqueue(new Callback<PaginatedResponse<Activity>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<PaginatedResponse<Activity>> call,
                            @NonNull Response<PaginatedResponse<Activity>> response
                    ) {
                        if (!isAdded() || binding == null) return;

                        binding.progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Activity> activities = response.body().getContent();

                            allActivities.clear();

                            if (activities != null) {
                                allActivities.addAll(activities);
                            }

                            buildDynamicFilterOptions();
                            setupFeaturedFromAllActivities();
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

                        binding.progressBar.setVisibility(View.GONE);

                        Log.e("HomeFragment", "Error: " + t.getMessage());
                        Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupFeaturedFromAllActivities() {
        List<Activity> featured = new ArrayList<>();

        for (int i = 0; i < allActivities.size() && i < 5; i++) {
            featured.add(allActivities.get(i));
        }

        featuredAdapter.setActivities(featured);

        if (binding != null) {
            binding.layoutFeatured.setVisibility(featured.isEmpty() ? View.GONE : View.VISIBLE);
        }
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
            updateFeaturedVisibility();
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

    private boolean matchesPrice(Activity activity) {
        double price = activity.getPrice();

        if (filterMinPrice != null && price < filterMinPrice) return false;
        if (filterMaxPrice != null && price > filterMaxPrice) return false;

        return true;
    }

    private void updateFeaturedVisibility() {
        boolean hasActiveFilters =
                searchText != null && !searchText.trim().isEmpty()
                        || filterCategory != null && !filterCategory.trim().isEmpty()
                        || filterDestination != null && !filterDestination.trim().isEmpty()
                        || filterMinPrice != null
                        || filterMaxPrice != null;

        if (binding != null) {
            binding.layoutFeatured.setVisibility(hasActiveFilters ? View.GONE : View.VISIBLE);
        }
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