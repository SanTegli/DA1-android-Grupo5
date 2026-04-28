package com.example.androidnativegrupo5.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.Activity;
import com.example.androidnativegrupo5.data.model.NewsItem;
import com.example.androidnativegrupo5.data.model.PaginatedResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentFirstBinding;
import com.google.gson.Gson;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private FragmentFirstBinding binding;
    private ActivityAdapter adapter;
    private FeaturedActivityAdapter featuredAdapter;
    private NewsAdapter newsAdapter;

    @Inject
    ApiService apiService;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    private String filterCategory = null;
    private String filterDestination = null;
    private Integer filterMinPrice = null;
    private Integer filterMaxPrice = null;
    private String filterSearch = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        
        // No reset filters here to keep user selection if they navigate back
        loadActivities();

        binding.btnViewAllActivities.setOnClickListener(v ->
                navController.navigate(R.id.ExploreActivitiesFragment)
        );

        binding.btnFilter.setOnClickListener(v -> {
            FilterBottomSheetDialogFragment bottomSheet = new FilterBottomSheetDialogFragment();

            bottomSheet.setOnFiltersAppliedListener((search, category, destination, minPrice, maxPrice) -> {
                filterSearch = (search != null && !search.isEmpty()) ? search : null;
                filterCategory = ("Todas".equalsIgnoreCase(category)) ? null : category;
                filterDestination = ("Todos".equalsIgnoreCase(destination)) ? null : destination;
                
                // If it was working before, maybe it needs 0 instead of null for minPrice?
                filterMinPrice = (minPrice != null && minPrice > 0) ? minPrice.intValue() : null;
                filterMaxPrice = (maxPrice != null && maxPrice < 100000) ? maxPrice.intValue() : null;

                currentPage = 0;
                isLastPage = false;
                adapter.clearActivities();
                loadActivities();

                if (filterCategory != null
                        || filterDestination != null
                        || filterMaxPrice != null
                        || (filterSearch != null && !filterSearch.isEmpty())) {
                    binding.layoutFeatured.setVisibility(View.GONE);
                    binding.layoutNews.setVisibility(View.GONE);
                } else {
                    binding.layoutFeatured.setVisibility(View.VISIBLE);
                    loadNews(); 
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
        binding.recyclerFeatured.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerFeatured.setAdapter(featuredAdapter);
        loadFeaturedActivities();
    }

    private void loadFeaturedActivities() {
        // Try calling the recommended endpoint if the standard one is empty
        apiService.getRecommendedActivities().enqueue(new Callback<PaginatedResponse<Activity>>() {
            @Override
            public void onResponse(@NonNull Call<PaginatedResponse<Activity>> call, @NonNull Response<PaginatedResponse<Activity>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getContent() != null) {
                    List<Activity> list = response.body().getContent();
                    if (!list.isEmpty()) {
                        featuredAdapter.setActivities(list);
                        if (binding != null) binding.layoutFeatured.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<PaginatedResponse<Activity>> call, @NonNull Throwable t) {}
        });
    }

    private void loadActivities() {
        if (isLoading) return;
        isLoading = true;
        if (binding != null) binding.progressBar.setVisibility(View.VISIBLE);

        // Very detailed log to see what exactly is happening
        Log.e("HOME_DEBUG", "FETCHING: Page=" + currentPage + ", Search=" + filterSearch + ", Cat=" + filterCategory);

        apiService.getActivities(
                currentPage,
                PAGE_SIZE,
                filterCategory,
                filterDestination,
                filterMinPrice,
                filterMaxPrice,
                filterSearch
        ).enqueue(new Callback<PaginatedResponse<Activity>>() {
            @Override
            public void onResponse(@NonNull Call<PaginatedResponse<Activity>> call, @NonNull Response<PaginatedResponse<Activity>> response) {
                isLoading = false;
                if (!isAdded() || binding == null) return;
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    PaginatedResponse<Activity> body = response.body();
                    List<Activity> activities = body.getContent();
                    
                    Log.e("HOME_DEBUG", "SUCCESS! Items Count: " + (activities != null ? activities.size() : "NULL"));
                    Log.e("HOME_DEBUG", "FULL JSON: " + new Gson().toJson(body));

                    if (activities != null && !activities.isEmpty()) {
                        adapter.addActivities(activities);
                        if (currentPage >= body.getTotalPages() - 1) isLastPage = true;
                        else currentPage++;
                    } else {
                        if (currentPage == 0) {
                            Toast.makeText(requireContext(), "No se encontraron actividades", Toast.LENGTH_SHORT).show();
                        }
                        isLastPage = true;
                    }
                } else {
                    Log.e("HOME_DEBUG", "SERVER ERROR: " + response.code() + " - " + response.message());
                    Toast.makeText(requireContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PaginatedResponse<Activity>> call, @NonNull Throwable t) {
                isLoading = false;
                if (!isAdded() || binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                Log.e("HOME_DEBUG", "NETWORK FAIL: " + t.getMessage());
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
