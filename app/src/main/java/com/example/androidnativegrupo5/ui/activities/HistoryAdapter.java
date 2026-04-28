package com.example.androidnativegrupo5.ui.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.ActivityHistoryItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface OnHistoryClickListener {
        void onHistoryClick(ActivityHistoryItem item);
        void onRateClick(ActivityHistoryItem item);
    }

    private final List<ActivityHistoryItem> items = new ArrayList<>();
    private final OnHistoryClickListener listener;

    public HistoryAdapter(OnHistoryClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ActivityHistoryItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_activity, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardHistory;
        private final TextView textDate;
        private final TextView textName;
        private final TextView textDestination;
        private final TextView textGuide;
        private final View layoutRatingInfo;
        private final TextView textRatingActivity;
        private final TextView textRatingGuide;
        private final TextView textRatingComment;
        private final MaterialButton btnRate;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardHistory = itemView.findViewById(R.id.card_history);
            textDate = itemView.findViewById(R.id.text_history_date);
            textName = itemView.findViewById(R.id.text_history_name);
            textDestination = itemView.findViewById(R.id.text_history_destination);
            textGuide = itemView.findViewById(R.id.text_history_guide);

            layoutRatingInfo = itemView.findViewById(R.id.layout_rating_info);
            textRatingActivity = itemView.findViewById(R.id.text_rating_activity);
            textRatingGuide = itemView.findViewById(R.id.text_rating_guide);
            textRatingComment = itemView.findViewById(R.id.text_rating_comment);
            btnRate = itemView.findViewById(R.id.btn_rate_experience);
        }

        public void bind(ActivityHistoryItem item, OnHistoryClickListener listener) {
            textDate.setText(item.getDate());
            textName.setText(item.getActivityName());
            textDestination.setText(safe(item.getDestination()));
            textGuide.setText("Guía: " + safe(item.getGuideName()));

            if (item.getActivityScore() != null && item.getActivityScore() > 0) {
                layoutRatingInfo.setVisibility(View.VISIBLE);
                btnRate.setVisibility(View.GONE);

                textRatingActivity.setText("Actividad: " + getStars(item.getActivityScore()));
                textRatingGuide.setText("Guía: " + getStars(item.getGuideScore()));

                if (item.getComment() != null && !item.getComment().isEmpty()) {
                    textRatingComment.setVisibility(View.VISIBLE);
                    textRatingComment.setText("\"" + item.getComment() + "\"");
                } else {
                    textRatingComment.setVisibility(View.GONE);
                }
            } else {
                layoutRatingInfo.setVisibility(View.GONE);

                //if (isWithinRatingWindow(item.getDate())) {
                if (true) {
                    btnRate.setVisibility(View.VISIBLE);
                    btnRate.setOnClickListener(v -> listener.onRateClick(item));
                } else {
                    btnRate.setVisibility(View.GONE);
                }
            }

            cardHistory.setOnClickListener(v -> listener.onHistoryClick(item));
        }

        private boolean isWithinRatingWindow(String dateStr) {
            try {
                // El historial suele venir en yyyy-MM-dd
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date activityDate = sdf.parse(dateStr);
                if (activityDate == null) return false;

                long currentTime = System.currentTimeMillis();
                long activityTimeMillis = activityDate.getTime();
                
                // 48 horas + margen de seguridad (por ejemplo 24h más para cubrir el día completo)
                // Total 72h desde el inicio del día de la actividad
                long windowMillis = 72 * 60 * 60 * 1000L; 
                
                return currentTime > activityTimeMillis && currentTime <= (activityTimeMillis + windowMillis);
            } catch (Exception e) {
                return false;
            }
        }

        private String getStars(Integer score) {
            if (score == null) return "";
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                stars.append(i < score ? "★" : "☆");
            }
            return stars.toString();
        }

        private String safe(String value) {
            return value != null ? value : "-";
        }
    }
}
