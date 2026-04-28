package com.example.androidnativegrupo5.ui.reservations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.ReservationResponse;

import java.util.List;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ViewHolder> {

    private List<ReservationResponse> list;
    private final OnReservationActionListener actionListener;

    public interface OnReservationActionListener {
        void onDetailClick(ReservationResponse reservation);
    }

    public ReservationAdapter(List<ReservationResponse> list, OnReservationActionListener listener) {
        this.list = list;
        this.actionListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, date, time, slots, status, totalPrice;
        Button btnManageReservation;

        public ViewHolder(@NonNull View view) {
            super(view);

            name = view.findViewById(R.id.text_name);
            date = view.findViewById(R.id.text_date);
            time = view.findViewById(R.id.text_time);
            slots = view.findViewById(R.id.text_slots);
            status = view.findViewById(R.id.text_status);
            totalPrice = view.findViewById(R.id.text_total_price);

            btnManageReservation = view.findViewById(R.id.btn_manage_reservation);
        }
    }

    @NonNull
    @Override
    public ReservationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationAdapter.ViewHolder holder, int position) {
        ReservationResponse r = list.get(position);

        holder.name.setText(r.getActivityName());
        holder.date.setText("Fecha: " + r.getDate());
        holder.time.setText("Hora: " + r.getTime());
        holder.slots.setText("Personas: " + (r.getParticipants() != null ? r.getParticipants() : 1));
        holder.status.setText("Estado: " + r.getStatus());

        holder.totalPrice.setText("$" + String.format("%.2f", r.getTotalPrice()));

        View.OnClickListener openDetailListener = v -> {
            if (actionListener != null) {
                actionListener.onDetailClick(r);
            }
        };

        holder.itemView.setOnClickListener(openDetailListener);
        holder.btnManageReservation.setOnClickListener(openDetailListener);
        holder.btnManageReservation.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void updateData(List<ReservationResponse> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }
}