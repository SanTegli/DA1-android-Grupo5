package com.example.androidnativegrupo5.ui.activities;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.db.FavoriteActivity;
import com.example.androidnativegrupo5.data.local.db.FavoriteDao;
import com.example.androidnativegrupo5.data.model.Activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private static final String TAG = "ActivityAdapter";
    private List<Activity> activityList;
    private final OnActivityClickListener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @EntryPoint
    @InstallIn(SingletonComponent.class)
    public interface AdapterEntryPoint {
        FavoriteDao favoriteDao();
    }

    public interface OnActivityClickListener {
        void onActivityClick(Activity activity);
    }

    public ActivityAdapter(OnActivityClickListener listener) {
        this.activityList = new ArrayList<>();
        this.listener = listener;
    }

    public void setActivities(List<Activity> activities) {
        Log.d(TAG, "setActivities: Cargando " + (activities != null ? activities.size() : 0) + " actividades");
        this.activityList = new ArrayList<>(activities);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        holder.bind(activityList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    public class ActivityViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageActivity;
        private final TextView textName;
        private final TextView textPrice;
        private final Button btnDetail;
        private final ImageButton btnFavorite;
        private final RatingBar ratingBar;
        private final TextView textRatingValue;
        private final TextView textCommentsCount;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            imageActivity = itemView.findViewById(R.id.image_activity);
            textName = itemView.findViewById(R.id.text_name);
            textPrice = itemView.findViewById(R.id.text_price);
            btnDetail = itemView.findViewById(R.id.btn_detail);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            textRatingValue = itemView.findViewById(R.id.text_rating_value);
            textCommentsCount = itemView.findViewById(R.id.text_comments_count);
        }

        public void bind(Activity activity, OnActivityClickListener listener) {
            Context context = itemView.getContext();
            Log.d(TAG, "Binding actividad: " + activity.getName() + " (Rating: " + activity.getAverageRating() + ")");

            textName.setText(activity.getName());
            
            if (activity.getPrice() <= 0) {
                textPrice.setText("Gratis");
                textPrice.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            } else {
                textPrice.setText(String.format(Locale.getDefault(), "$%.2f", activity.getPrice()));
                textPrice.setTextColor(ContextCompat.getColor(context, R.color.app_primary));
            }

            if (activity.getAverageRating() != null) {
                ratingBar.setRating(activity.getAverageRating().floatValue());
                textRatingValue.setText(String.format(Locale.getDefault(), "%.1f", activity.getAverageRating()));
            }
            
            if (activity.getRatingCount() != null) {
                textCommentsCount.setText("• " + activity.getRatingCount() + " Reseñas");
            }

            Glide.with(context)
                    .load(activity.getImageUrl())
                    .placeholder(R.drawable.common_illustration_welcome_placeholder)
                    .error(R.drawable.common_illustration_welcome_placeholder)
                    .into(imageActivity);

            AdapterEntryPoint entryPoint = EntryPointAccessors.fromApplication(context.getApplicationContext(), AdapterEntryPoint.class);
            FavoriteDao favoriteDao = entryPoint.favoriteDao();

            executor.execute(() -> {
                boolean isFav = favoriteDao.isFavorite(activity.getId());
                btnFavorite.post(() -> {
                    btnFavorite.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                });
            });

            btnFavorite.setOnClickListener(v -> {
                Log.d(TAG, "Toggle favorito para: " + activity.getName());
                executor.execute(() -> {
                    boolean isFav = favoriteDao.isFavorite(activity.getId());
                    if (isFav) {
                        favoriteDao.delete(favoriteDao.getFavoriteById(activity.getId()));
                    } else {
                        favoriteDao.insert(new FavoriteActivity(
                                activity.getId(), activity.getName(), activity.getDestination(),
                                activity.getPrice(), activity.getAvailableSlots(), activity.getImageUrl()
                        ));
                    }
                    btnFavorite.post(() -> {
                        btnFavorite.setImageResource(!isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                        Toast.makeText(context, !isFav ? "Añadido a favoritos" : "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                    });
                });
            });

            itemView.setOnClickListener(v -> listener.onActivityClick(activity));
            btnDetail.setOnClickListener(v -> listener.onActivityClick(activity));
        }
    }
}
