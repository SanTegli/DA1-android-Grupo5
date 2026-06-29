package com.example.androidnativegrupo5.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.model.Transaction;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentTransactionsBinding;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class TransactionsFragment extends Fragment {

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;

    private FragmentTransactionsBinding binding;
    private TransactionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        loadTransactions();
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(transaction -> {
            if (transaction.getReservationId() != null) {
                Bundle bundle = new Bundle();
                bundle.putLong("reservationId", transaction.getReservationId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_TransactionsFragment_to_ManageReservationFragment, bundle);
            }
        });
        binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerTransactions.setAdapter(adapter);
    }

    private void loadTransactions() {
        String token = "Bearer " + tokenManager.getToken();
        apiService.getMyTransactions(token).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Transaction> transactions = response.body();
                    adapter.setTransactions(transactions);
                    binding.textNoTransactions.setVisibility(transactions.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                Toast.makeText(getContext(), "Error al cargar transacciones", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
