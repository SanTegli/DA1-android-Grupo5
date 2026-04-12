package com.example.androidnativegrupo5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnativegrupo5.model.ActivityHistoryItem;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface OnHistoryClickListener {
        void onHistoryClick(ActivityHistoryItem item);
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

        private final CardView cardHistory;
        private final TextView textDate;
        private final TextView textName;
        private final TextView textDestination;
        private final TextView textGuide;
        private final TextView textDuration;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardHistory = itemView.findViewById(R.id.card_history);
            textDate = itemView.findViewById(R.id.text_history_date);
            textName = itemView.findViewById(R.id.text_history_name);
            textDestination = itemView.findViewById(R.id.text_history_destination);
            textGuide = itemView.findViewById(R.id.text_history_guide);
            textDuration = itemView.findViewById(R.id.text_history_duration);
        }

        public void bind(ActivityHistoryItem item, OnHistoryClickListener listener) {
            textDate.setText(item.getDate());
            textName.setText(item.getActivityName());
            textDestination.setText("Destino: " + safe(item.getDestination()));
            textGuide.setText("Guía: " + safe(item.getGuideName()));
            textDuration.setText("Duración: " + safe(item.getDuration()));

            cardHistory.setOnClickListener(v -> listener.onHistoryClick(item));
        }

        private String safe(String value) {
            return value != null ? value : "-";
        }
    }
}