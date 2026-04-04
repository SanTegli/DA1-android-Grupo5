package com.example.androidnativegrupo5;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnativegrupo5.databinding.FragmentFirstBinding;
import com.example.androidnativegrupo5.model.Activity;
import com.example.androidnativegrupo5.model.PaginatedResponse;
import com.example.androidnativegrupo5.network.ApiService;
import com.example.androidnativegrupo5.network.RetrofitClient;

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

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ActivityAdapter(activity -> {
            Bundle bundle = new Bundle();
            bundle.putLong("activityId", activity.getId());
            Navigation.findNavController(view)
                    .navigate(R.id.action_FirstFragment_to_DetailFragment, bundle);
        });

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
            public void onResponse(@NonNull Call<PaginatedResponse<Activity>> call,
                                   @NonNull Response<PaginatedResponse<Activity>> response) {
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
                    Toast.makeText(getContext(), "No se encontraron actividades", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PaginatedResponse<Activity>> call, @NonNull Throwable t) {
                isLoading = false;
                binding.progressBar.setVisibility(View.GONE);
                Log.e("FirstFragment", "Error: " + t.getMessage());
                Toast.makeText(getContext(), "Error de conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}