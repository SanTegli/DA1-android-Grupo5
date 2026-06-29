package com.example.androidnativegrupo5.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions = new ArrayList<>();
    private final OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public TransactionAdapter(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(transactions.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView textAmount;
        private final TextView textStatus;
        private final TextView textDate;
        private final TextView textCard;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            textAmount = itemView.findViewById(R.id.text_transaction_amount);
            textStatus = itemView.findViewById(R.id.text_transaction_status);
            textDate = itemView.findViewById(R.id.text_transaction_date);
            textCard = itemView.findViewById(R.id.text_transaction_card);
        }

        public void bind(Transaction transaction, OnTransactionClickListener listener) {
            if (transaction.getAmount() <= 0) {
                textAmount.setText("FREE");
            } else {
                textAmount.setText(String.format(Locale.getDefault(), "$%.2f", transaction.getAmount()));
            }
            textStatus.setText(transaction.getStatus());
            textDate.setText("Fecha: " + (transaction.getCreatedAt() != null ? transaction.getCreatedAt() : "-"));
            
            String card = transaction.getMaskedCard();
            if (card == null || card.isEmpty()) {
                textCard.setText("Tarjeta: N/A");
            } else {
                textCard.setText("Tarjeta: " + card);
            }

            if ("RECHAZADA".equalsIgnoreCase(transaction.getStatus())) {
                textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark));
            } else {
                textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_dark));
            }

            itemView.setOnClickListener(v -> listener.onTransactionClick(transaction));
        }
    }
}
