package com.example.androidnativegrupo5;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

    private final List<Activity> activities = new ArrayList<>();

    public void addActivities(List<Activity> newActivities) {
        int startPos = activities.size();
        activities.addAll(newActivities);
        notifyItemRangeInserted(startPos, newActivities.size());
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        Activity activity = activities.get(position);
        holder.bind(activity);
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageActivity;
        private final TextView textName;
        private final TextView textDestination;
        private final TextView textCategory;
        private final TextView textDuration;
        private final TextView textPrice;
        private final TextView textSlots;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            imageActivity = itemView.findViewById(R.id.image_activity);
            textName = itemView.findViewById(R.id.text_name);
            textDestination = itemView.findViewById(R.id.text_destination);
            textCategory = itemView.findViewById(R.id.text_category);
            textDuration = itemView.findViewById(R.id.text_duration);
            textPrice = itemView.findViewById(R.id.text_price);
            textSlots = itemView.findViewById(R.id.text_slots);
        }

        public void bind(Activity activity) {
            Context context = itemView.getContext();
            textName.setText(activity.getName());
            textDestination.setText(activity.getDestination());
            textCategory.setText(activity.getCategory().toUpperCase());
            
            textDuration.setText(context.getString(R.string.duration_label, activity.getDuration()));
            textSlots.setText(context.getString(R.string.slots_label, activity.getAvailableSlots()));
            
            if (activity.getPrice() <= 0) {
                textPrice.setText(R.string.price_free);
                textPrice.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            } else {
                textPrice.setText(String.format(Locale.getDefault(), "$%.2f", activity.getPrice()));
                // Usamos el color definido en nuestro colors.xml para evitar errores
                textPrice.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            }

            Glide.with(context)
                    .load(activity.getImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageActivity);
        }
    }
}
