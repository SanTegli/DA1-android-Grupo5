package com.example.androidnativegrupo5.ui.reservations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.ReservationResponse;

import java.util.List;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ViewHolder> {

    private List<ReservationResponse> list;
    private OnReservationActionListener actionListener;

    public interface OnReservationActionListener {
        void onCancelClick(ReservationResponse reservation);
        void onRateClick(ReservationResponse reservation);
    }

    public ReservationAdapter(List<ReservationResponse> list, OnReservationActionListener listener) {
        this.list = list;
        this.actionListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, date, time, slots, status, totalPrice;
        ImageView image;
        Button btnCancel, btnRate;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.text_name);
            date = view.findViewById(R.id.text_date);
            time = view.findViewById(R.id.text_time);
            slots = view.findViewById(R.id.text_slots);
            status = view.findViewById(R.id.text_status);
            totalPrice = view.findViewById(R.id.text_total_price);
            btnCancel = view.findViewById(R.id.btn_cancel_reservation);
            btnRate = view.findViewById(R.id.btn_rate_reservation);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReservationResponse r = list.get(position);

        holder.name.setText(r.getActivityName());
        holder.date.setText("Fecha: " + r.getDate());
        holder.time.setText("Hora: " + r.getTime());
        holder.slots.setText("Personas: " + (r.getParticipants() != null ? r.getParticipants() : 1));
        holder.status.setText("Estado: " + r.getStatus());
        holder.totalPrice.setText("$" + String.format("%.2f", r.getTotalPrice()));

        holder.btnCancel.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onCancelClick(r);
            }
        });

        holder.btnRate.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onRateClick(r);
            }
        });

        if ("CANCELLED".equalsIgnoreCase(r.getStatus())) {
            holder.btnCancel.setEnabled(false);
            holder.btnCancel.setAlpha(0.5f);
            holder.btnRate.setVisibility(View.GONE);
        } else if ("COMPLETED".equalsIgnoreCase(r.getStatus())) {
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnRate.setVisibility(View.VISIBLE);
        } else {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setEnabled(true);
            holder.btnCancel.setAlpha(1.0f);
            holder.btnRate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void removeReservationById(Long id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                list.remove(i);
                notifyItemRemoved(i);
                notifyItemRangeChanged(i, list.size());
                break;
            }
        }
    }

    public void updateData(List<ReservationResponse> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }
}