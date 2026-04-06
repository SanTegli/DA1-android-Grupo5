package com.example.androidnativegrupo5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnativegrupo5.model.ReservationResponse;

import java.util.List;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ViewHolder> {

    private List<ReservationResponse> list;
    private OnCancelClickListener cancelClickListener;

    public interface OnCancelClickListener {
        void onCancelClick(ReservationResponse reservation);
    }

    public ReservationAdapter(List<ReservationResponse> list, OnCancelClickListener listener) {
        this.list = list;
        this.cancelClickListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, date, time, slots, status, totalPrice;
        ImageView image;
        Button btnCancel;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.text_name);
            date = view.findViewById(R.id.text_date);
            time = view.findViewById(R.id.text_time);
            slots = view.findViewById(R.id.text_slots);
            status = view.findViewById(R.id.text_status);
            totalPrice = view.findViewById(R.id.text_total_price);
            btnCancel = view.findViewById(R.id.btn_cancel_reservation);
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
            if (cancelClickListener != null) {
                cancelClickListener.onCancelClick(r);
            }
        });

        if ("CANCELLED".equalsIgnoreCase(r.getStatus())) {
            holder.btnCancel.setEnabled(false);
            holder.btnCancel.setAlpha(0.5f);
        } else {
            holder.btnCancel.setEnabled(true);
            holder.btnCancel.setAlpha(1.0f);
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
}