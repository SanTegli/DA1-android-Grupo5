package com.example.androidnativegrupo5;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.androidnativegrupo5.model.Activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    public interface OnActivityClickListener {
        void onActivityClick(Activity activity);
    }

    private final List<Activity> activities = new ArrayList<>();
    private final OnActivityClickListener listener;

    public ActivityAdapter(OnActivityClickListener listener) {
        this.listener = listener;
    }

    public void addActivities(List<Activity> newActivities) {
        int startPos = activities.size();
        activities.addAll(newActivities);
        notifyItemRangeInserted(startPos, newActivities.size());
    }

    public void clearActivities() {
        activities.clear();
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
        holder.bind(activities.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageActivity;
        private final TextView textName;
        private final TextView textDescription;
        private final TextView textDestination;
        private final TextView textDuration;
        private final TextView textSlots;
        private final TextView textPrice;
        private final Button btnDetail;
        private final RatingBar ratingBar;
        private final TextView textRatingValue;
        private final TextView textCommentsCount;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            imageActivity = itemView.findViewById(R.id.image_activity);
            textName = itemView.findViewById(R.id.text_name);
            textDescription = itemView.findViewById(R.id.text_description);
            textDestination = itemView.findViewById(R.id.text_destination);
            textDuration = itemView.findViewById(R.id.text_duration);
            textSlots = itemView.findViewById(R.id.text_slots);
            textPrice = itemView.findViewById(R.id.text_price);
            btnDetail = itemView.findViewById(R.id.btn_detail);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            textRatingValue = itemView.findViewById(R.id.text_rating_value);
            textCommentsCount = itemView.findViewById(R.id.text_comments_count);
        }

        public void bind(Activity activity, OnActivityClickListener listener) {
            Context context = itemView.getContext();

            textName.setText(activity.getName());
            textDescription.setText(activity.getDescription() != null ? activity.getDescription() : "");
            textDestination.setText(activity.getDestination() != null ? activity.getDestination() : "");
            textDuration.setText(activity.getDuration() != null ? activity.getDuration() : "");
            textSlots.setText(String.format(Locale.getDefault(), "%d cupos", activity.getAvailableSlots()));

            if (activity.getPrice() <= 0) {
                textPrice.setText("Gratis");
                textPrice.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            } else {
                textPrice.setText(String.format(Locale.getDefault(), "$%.2f", activity.getPrice()));
                textPrice.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            }

            // Bind Rating and Comments
            if (activity.getAverageRating() != null) {
                ratingBar.setVisibility(View.VISIBLE);
                textRatingValue.setVisibility(View.VISIBLE);
                ratingBar.setRating(activity.getAverageRating().floatValue());
                textRatingValue.setText(String.format(Locale.getDefault(), "%.1f", activity.getAverageRating()));
            } else {
                // Mock data for visibility if backend is empty (change to GONE in production if preferred)
                ratingBar.setVisibility(View.VISIBLE);
                textRatingValue.setVisibility(View.VISIBLE);
                ratingBar.setRating(0f);
                textRatingValue.setText("0.0");
            }

            if (activity.getRatingCount() != null) {
                textCommentsCount.setVisibility(View.VISIBLE);
                textCommentsCount.setText(String.format(Locale.getDefault(), "(%d comentarios)", activity.getRatingCount()));
            } else {
                textCommentsCount.setVisibility(View.VISIBLE);
                textCommentsCount.setText("(0 comentarios)");
            }

            Glide.with(context)
                    .load(activity.getImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageActivity);

            itemView.setOnClickListener(v -> listener.onActivityClick(activity));
            btnDetail.setOnClickListener(v -> listener.onActivityClick(activity));
        }
    }
}
