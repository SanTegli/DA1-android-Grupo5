package com.example.androidnativegrupo5.ui.reservations;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.ReservationResponse;

import java.lang.reflect.Method;
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
        ImageView imageReservation;
        Button btnManageReservation;

        public ViewHolder(@NonNull View view) {
            super(view);

            imageReservation = view.findViewById(R.id.image_reservation);
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

        holder.name.setText(safeText(String.valueOf(r.getActivityName()), "Actividad"));
        holder.date.setText("Fecha: " + safeText(String.valueOf(r.getDate()), "-"));
        holder.time.setText("Hora: " + safeText(String.valueOf(r.getTime()), "-"));
        holder.slots.setText("Personas: " + r.getParticipants());

        holder.status.setText(formatStatus(String.valueOf(r.getStatus())));
        applyStatusStyle(holder.status, String.valueOf(r.getStatus()));
        holder.totalPrice.setText("$" + String.format("%.2f", r.getTotalPrice()));

        String imageUrl = getStringValue(
                r,
                "getImageUrl",
                "getActivityImageUrl",
                "getImage",
                "getActivityImage"
        );

        Log.d("RESERVATION_IMAGE", "URL imagen reserva: " + imageUrl);

        Glide.with(holder.itemView.getContext())
                .load(imageUrl == null || imageUrl.trim().isEmpty() ? null : imageUrl)
                .placeholder(R.drawable.common_illustration_welcome_placeholder)
                .error(R.drawable.common_illustration_welcome_placeholder)
                .into(holder.imageReservation);

        View.OnClickListener openDetailListener = v -> {
            if (actionListener != null) {
                actionListener.onDetailClick(r);
            }
        };

        holder.itemView.setOnClickListener(openDetailListener);
        holder.btnManageReservation.setOnClickListener(openDetailListener);
        holder.btnManageReservation.setVisibility(View.VISIBLE);
    }

    private String getStringValue(Object object, String... methodNames) {
        if (object == null) return "";

        for (String methodName : methodNames) {
            try {
                Method method = object.getClass().getMethod(methodName);
                Object value = method.invoke(object);

                if (value != null) {
                    return String.valueOf(value);
                }
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.equals("null") || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }

    private String formatStatus(String status) {
        if (status == null) return "Confirmada";

        switch (status.trim().toUpperCase()) {
            case "CONFIRMED":
            case "CONFIRMADED":
            case "CONFIRMADO":
            case "CONFIRMADA":
                return "Confirmada";

            case "FINISHED":
            case "FINALIZADO":
            case "FINALIZADA":
                return "Finalizada";

            case "CANCELLED":
            case "CANCELED":
            case "CANCELADO":
            case "CANCELADA":
                return "Cancelada";

            case "PENDING":
            case "PENDIENTE":
                return "Pendiente";

            default:
                return status;
        }
    }
    private void applyStatusStyle(TextView statusView, String status) {
        String formattedStatus = formatStatus(status);

        switch (formattedStatus) {
            case "Confirmada":
                statusView.setBackgroundResource(R.drawable.common_bg_status_confirmed);
                statusView.setTextColor(Color.parseColor("#2F7A7E"));
                break;

            case "Finalizada":
                statusView.setBackgroundResource(R.drawable.common_bg_status_finished);
                statusView.setTextColor(Color.parseColor("#1E4DB7"));
                break;

            case "Cancelada":
                statusView.setBackgroundResource(R.drawable.common_bg_status_cancelled);
                statusView.setTextColor(Color.parseColor("#B3261E"));
                break;

            default:
                statusView.setBackgroundResource(R.drawable.common_bg_status_confirmed);
                statusView.setTextColor(Color.parseColor("#2F7A7E"));
                break;
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