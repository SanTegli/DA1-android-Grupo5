package com.example.androidnativegrupo5.ui.favorites;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.data.local.db.FavoriteEntity;
import com.example.androidnativegrupo5.databinding.ItemFavoriteCardBinding;

import java.util.ArrayList;
import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {

    private List<FavoriteEntity> favorites = new ArrayList<>();
    private final OnFavoriteClickListener listener;

    public interface OnFavoriteClickListener {
        void onFavoriteClick(FavoriteEntity favorite);
        void onRemoveFavorite(FavoriteEntity favorite);
    }

    public FavoriteAdapter(OnFavoriteClickListener listener) {
        this.listener = listener;
    }

    public void setFavorites(List<FavoriteEntity> favorites) {
        this.favorites = favorites;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFavoriteCardBinding binding = ItemFavoriteCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FavoriteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        holder.bind(favorites.get(position));
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    class FavoriteViewHolder extends RecyclerView.ViewHolder {
        private final ItemFavoriteCardBinding binding;

        public FavoriteViewHolder(ItemFavoriteCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FavoriteEntity favorite) {
            binding.tvActivityName.setText(favorite.getActivityName());
            binding.tvDestination.setText(favorite.getDestination());
            binding.tvPrice.setText("$" + favorite.getPrice());

            binding.tvPriceChange.setVisibility(favorite.isHasPriceChanged() ? View.VISIBLE : View.GONE);
            binding.tvAvailabilityChange.setVisibility(favorite.isHasAvailabilityChanged() ? View.VISIBLE : View.GONE);

            Glide.with(binding.ivActivityImage.getContext())
                    .load(favorite.getImageUrl())
                    .into(binding.ivActivityImage);

            itemView.setOnClickListener(v -> listener.onFavoriteClick(favorite));
            binding.btnRemoveFavorite.setOnClickListener(v -> listener.onRemoveFavorite(favorite));
        }
    }
}
