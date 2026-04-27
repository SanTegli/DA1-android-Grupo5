package com.example.androidnativegrupo5.ui.favorites;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.db.FavoriteActivity;
import com.example.androidnativegrupo5.data.local.db.FavoriteDao;
import com.example.androidnativegrupo5.data.model.Activity;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentFavoritesBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class FavoritesFragment extends Fragment {

    private FragmentFavoritesBinding binding;
    private FavoritesAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Inject
    FavoriteDao favoriteDao;

    @Inject
    ApiService apiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FavoritesAdapter(new ArrayList<>(), new FavoritesAdapter.OnFavoriteClickListener() {
            @Override
            public void onActivityClick(FavoriteActivity favorite) {
                Bundle bundle = new Bundle();
                bundle.putLong("activityId", favorite.getId());
                Navigation.findNavController(requireView()).navigate(R.id.action_FavoritesFragment_to_DetailFragment, bundle);
            }

            @Override
            public void onUnfavoriteClick(FavoriteActivity favorite) {
                executor.execute(() -> {
                    favoriteDao.delete(favorite);
                    loadFavorites();
                });
            }
        });
        binding.recyclerFavorites.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        executor.execute(() -> {
            List<FavoriteActivity> favorites = favoriteDao.getAllFavorites();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (favorites.isEmpty()) {
                        binding.textEmptyFavorites.setVisibility(View.VISIBLE);
                        binding.recyclerFavorites.setVisibility(View.GONE);
                    } else {
                        binding.textEmptyFavorites.setVisibility(View.GONE);
                        binding.recyclerFavorites.setVisibility(View.VISIBLE);
                        adapter.updateData(favorites);
                        checkForUpdates(favorites);
                    }
                });
            }
        });
    }

    private void checkForUpdates(List<FavoriteActivity> favorites) {
        for (FavoriteActivity fav : favorites) {
            apiService.getActivityById(fav.getId()).enqueue(new Callback<Activity>() {
                @Override
                public void onResponse(Call<Activity> call, Response<Activity> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Activity updated = response.body();
                        boolean changed = false;
                        if (updated.getPrice() != fav.getLastKnownPrice()) {
                            fav.setHasPriceChange(true);
                            fav.setLastKnownPrice(updated.getPrice());
                            changed = true;
                        }
                        if (updated.getAvailableSlots() != fav.getLastKnownSlots()) {
                            fav.setHasSlotChange(true);
                            fav.setLastKnownSlots(updated.getAvailableSlots());
                            changed = true;
                        }

                        if (changed) {
                            executor.execute(() -> {
                                favoriteDao.update(fav);
                                if (isAdded()) {
                                    requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
                                }
                            });
                        }
                    }
                }

                @Override
                public void onFailure(Call<Activity> call, Throwable t) {
                    // Ignorar errores de red para el indicador visual
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
