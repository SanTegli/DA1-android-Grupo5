package com.example.androidnativegrupo5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.androidnativegrupo5.model.Activity;

import java.util.ArrayList;
import java.util.List;

public class FeaturedActivityAdapter extends RecyclerView.Adapter<FeaturedActivityAdapter.FeaturedViewHolder> {

    public interface OnActivityClickListener {
        void onActivityClick(Activity activity);
    }

    private final List<Activity> activities = new ArrayList<>();
    private final OnActivityClickListener listener;

    public FeaturedActivityAdapter(OnActivityClickListener listener) {
        this.listener = listener;
    }

    public void setActivities(List<Activity> newActivities) {
        activities.clear();
        activities.addAll(newActivities);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeaturedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured_activity, parent, false);
        return new FeaturedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeaturedViewHolder holder, int position) {
        holder.bind(activities.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class FeaturedViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageFeatured;
        private final TextView textName;
        private final TextView textDestination;

        public FeaturedViewHolder(@NonNull View itemView) {
            super(itemView);
            imageFeatured = itemView.findViewById(R.id.image_featured);
            textName = itemView.findViewById(R.id.text_featured_name);
            textDestination = itemView.findViewById(R.id.text_featured_destination);
        }

        public void bind(Activity activity, OnActivityClickListener listener) {
            textName.setText(activity.getName());
            textDestination.setText(activity.getDestination());

            Glide.with(itemView.getContext())
                    .load(activity.getImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageFeatured);

            itemView.setOnClickListener(v -> listener.onActivityClick(activity));
        }
    }
}