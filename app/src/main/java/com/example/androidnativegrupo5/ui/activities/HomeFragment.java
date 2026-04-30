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
import com.example.androidnativegrupo5.utils.NetworkUtils;

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

    private static final String TAG = "HomeFragment";

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;

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
    private Integer filterMinPrice = null;
    private Integer filterMaxPrice = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = Navigation.findNavController(view);

        setupAdapters(navController);
        checkConnectionAndLoad(navController);
        setupListeners(navController);
    }

    private void setupAdapters(NavController navController) {
        adapter = new ActivityAdapter(activity -> {
            Bundle bundle = new Bundle();
            bundle.putLong("activityId", activity.getId());
            navController.navigate(R.id.action_FirstFragment_to_DetailFragment, bundle);
        });
        binding.recyclerActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerActivities.setAdapter(adapter);

        newsAdapter = new NewsAdapter(news -> {
            if (news.getLinkUrl() != null && !news.getLinkUrl().isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(news.getLinkUrl())));
            }
        });
        binding.recyclerNews.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerNews.setAdapter(newsAdapter);
    }

    private void setupListeners(NavController navController) {
        binding.editSearchActivities.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                searchText = s != null ? s.toString() : "";
                applyLocalFilters();
            }
        });

        binding.btnViewAllActivities.setOnClickListener(v -> navController.navigate(R.id.ExploreActivitiesFragment));
    }

    private void checkConnectionAndLoad(NavController navController) {
        if (!NetworkUtils.isOnline(requireContext())) {
            navController.navigate(R.id.action_FirstFragment_to_OfflineFragment);
        } else {
            loadNews();
            setupFeaturedCarousel(navController);
            loadAllActivities();
        }
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

    private void loadAllActivities() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);

        apiService.getActivities(0, 100, null, null, null, null, null)
                .enqueue(new Callback<PaginatedResponse<Activity>>() {
                    @Override
                    public void onResponse(@NonNull Call<PaginatedResponse<Activity>> call, @NonNull Response<PaginatedResponse<Activity>> response) {
                        if (!isAdded() || binding == null) return;
                        binding.progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            allActivities.clear();
                            allActivities.addAll(response.body().getContent());
                            applyLocalFilters();
                        } else {
                            // Si el back devuelve error aunque haya internet (back apagado)
                            Navigation.findNavController(requireView()).navigate(R.id.action_FirstFragment_to_OfflineFragment);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PaginatedResponse<Activity>> call, @NonNull Throwable t) {
                        if (!isAdded() || binding == null) return;
                        binding.progressBar.setVisibility(View.GONE);
                        // Falla total de conexión al backend
                        Navigation.findNavController(requireView()).navigate(R.id.action_FirstFragment_to_OfflineFragment);
                    }
                });
    }

    private void loadNews() {
        apiService.getNews().enqueue(new Callback<List<NewsItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<NewsItem>> call, @NonNull Response<List<NewsItem>> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    newsAdapter.setNewsList(response.body());
                    binding.layoutNews.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onFailure(@NonNull Call<List<NewsItem>> call, @NonNull Throwable t) {}
        });
    }

    private void loadFeaturedActivities() {
        apiService.getActivities(0, 5, null, null, null, null, null)
                .enqueue(new Callback<PaginatedResponse<Activity>>() {
                    @Override
                    public void onResponse(@NonNull Call<PaginatedResponse<Activity>> call, @NonNull Response<PaginatedResponse<Activity>> response) {
                        if (!isAdded() || binding == null) return;
                        if (response.isSuccessful() && response.body() != null) {
                            featuredAdapter.setActivities(response.body().getContent());
                        }
                    }
                    @Override public void onFailure(@NonNull Call<PaginatedResponse<Activity>> call, @NonNull Throwable t) {}
                });
    }

    private void applyLocalFilters() {
        List<Activity> filtered = new ArrayList<>();
        String search = searchText.toLowerCase(Locale.ROOT).trim();
        for (Activity a : allActivities) {
            if (a.getName().toLowerCase().contains(search)) filtered.add(a);
        }
        adapter.setActivities(filtered);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
