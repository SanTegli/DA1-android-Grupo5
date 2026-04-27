package com.example.androidnativegrupo5.ui.favorites;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.db.FavoriteEntity;
import com.example.androidnativegrupo5.databinding.FragmentFavoritesBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FavoritesFragment extends Fragment implements FavoriteAdapter.OnFavoriteClickListener {

    private FragmentFavoritesBinding binding;
    private FavoriteViewModel viewModel;
    private FavoriteAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);

        setupRecyclerView();
        observeViewModel();
        
        viewModel.refreshFavorites();
    }

    private void setupRecyclerView() {
        adapter = new FavoriteAdapter(this);
        binding.rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvFavorites.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getFavorites().observe(getViewLifecycleOwner(), favorites -> {
            if (favorites != null && !favorites.isEmpty()) {
                adapter.setFavorites(favorites);
                binding.rvFavorites.setVisibility(View.VISIBLE);
                binding.tvEmptyMessage.setVisibility(View.GONE);
            } else {
                binding.rvFavorites.setVisibility(View.GONE);
                binding.tvEmptyMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onFavoriteClick(FavoriteEntity favorite) {
        // Clear indicators when clicking
        if (favorite.isHasPriceChanged() || favorite.isHasAvailabilityChanged()) {
            viewModel.clearIndicators(favorite.getId(), favorite.getActivityId());
        }
        
        Bundle bundle = new Bundle();
        bundle.putLong("activityId", favorite.getActivityId());
        Navigation.findNavController(requireView()).navigate(R.id.DetailFragment, bundle);
    }

    @Override
    public void onRemoveFavorite(FavoriteEntity favorite) {
        viewModel.toggleFavorite(favorite.getActivityId(), null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
