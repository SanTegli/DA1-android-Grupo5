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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ViewHolder> {

    private List<ReservationResponse> list;
    private final OnReservationActionListener actionListener;

    public interface OnReservationActionListener {
        void onCancelClick(ReservationResponse reservation);
        void onRateClick(ReservationResponse reservation);
        void onDetailClick(ReservationResponse reservation);
        void onRescheduleClick(ReservationResponse reservation);
    }

    public ReservationAdapter(List<ReservationResponse> list, OnReservationActionListener listener) {
        this.list = list;
        this.actionListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, date, time, slots, status, totalPrice;
        Button btnCancel, btnRate, btnReschedule;

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
            btnReschedule = view.findViewById(R.id.btn_reschedule_reservation);
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

        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDetailClick(r);
            }
        });

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

        holder.btnReschedule.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onRescheduleClick(r);
            }
        });

        if ("CANCELLED".equalsIgnoreCase(r.getStatus())) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setEnabled(false);
            holder.btnCancel.setAlpha(0.5f);
            holder.btnRate.setVisibility(View.GONE);
            holder.btnReschedule.setVisibility(View.GONE);
        } else if ("COMPLETED".equalsIgnoreCase(r.getStatus())) {
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnReschedule.setVisibility(View.GONE);
            
            if (isWithinRatingWindow(r.getDate(), r.getTime())) {
                holder.btnRate.setVisibility(View.VISIBLE);
            } else {
                holder.btnRate.setVisibility(View.GONE);
            }
        } else {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setEnabled(true);
            holder.btnCancel.setAlpha(1.0f);
            holder.btnRate.setVisibility(View.GONE);
            holder.btnReschedule.setVisibility(View.VISIBLE);
        }
    }

    private boolean isWithinRatingWindow(String dateStr, String timeStr) {
        try {
            // Se asume formato yyyy-MM-dd y HH:mm (ajustar si es necesario)
            // Si el tiempo viene como "10:00 AM", el parseo debe ser diferente.
            // Según item_reservation.xml tools: "2023-10-25" y "10:00 AM"
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US);
            Date activityDate = sdf.parse(dateStr + " " + timeStr);
            
            if (activityDate == null) return false;

            long currentTime = System.currentTimeMillis();
            long activityTimeMillis = activityDate.getTime();
            
            // 48 horas en milisegundos
            long fortyEightHoursInMillis = 48 * 60 * 60 * 1000L;
            
            // La opción aparece DESPUÉS de finalizar y hasta 48 horas después
            return currentTime > activityTimeMillis && currentTime <= (activityTimeMillis + fortyEightHoursInMillis);
            
        } catch (Exception e) {
            e.printStackTrace();
            // Por defecto, si hay error de parseo, permitimos calificar si está COMPLETED 
            // para no bloquear al usuario, o retornamos false según política.
            return true; 
        }
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
