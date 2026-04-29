package com.example.androidnativegrupo5.ui.favorites;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.db.FavoriteActivity;
import com.example.androidnativegrupo5.databinding.ItemActivityBinding;

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private List<FavoriteActivity> favorites;
    private final OnFavoriteClickListener listener;

    public interface OnFavoriteClickListener {
        void onActivityClick(FavoriteActivity favorite);
        void onUnfavoriteClick(FavoriteActivity favorite);
    }

    public FavoritesAdapter(List<FavoriteActivity> favorites, OnFavoriteClickListener listener) {
        this.favorites = favorites;
        this.listener = listener;
    }

    public void updateData(List<FavoriteActivity> newFavorites) {
        this.favorites = newFavorites;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemActivityBinding binding = ItemActivityBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FavoriteActivity favorite = favorites.get(position);
        holder.bind(favorite);
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemActivityBinding binding;

        ViewHolder(ItemActivityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FavoriteActivity favorite) {
            binding.textName.setText(favorite.getName());
            binding.textPrice.setText(String.format("$%.2f", favorite.getLastKnownPrice()));
            
            // Icono de corazón lleno porque es favorito
            binding.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            
            // Cargar imagen con Glide
            Glide.with(itemView.getContext())
                    .load(favorite.getImageUrl())
                    .placeholder(R.drawable.common_illustration_welcome_placeholder)
                    .into(binding.imageActivity);

            // Indicadores de cambios
            if (favorite.isHasPriceChange() || favorite.isHasSlotChange()) {
                binding.indicatorUpdate.setVisibility(View.VISIBLE);
                if (favorite.isHasPriceChange() && favorite.isHasSlotChange()) {
                    binding.indicatorUpdate.setText("¡Precio y cupos modificados!");
                } else if (favorite.isHasPriceChange()) {
                    binding.indicatorUpdate.setText("¡Precio modificado!");
                } else {
                    binding.indicatorUpdate.setText("¡Nuevos cupos!");
                }
            } else {
                binding.indicatorUpdate.setVisibility(View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> listener.onActivityClick(favorite));
            binding.btnDetail.setOnClickListener(v -> listener.onActivityClick(favorite));
            binding.btnFavorite.setOnClickListener(v -> listener.onUnfavoriteClick(favorite));
            
            // Valores por defecto para campos que no vienen en el objeto local simplificado
            binding.ratingBar.setRating(5f);
            binding.textRatingValue.setText("5.0");
            binding.textCommentsCount.setText("• Nuevo");
        }
    }
}
