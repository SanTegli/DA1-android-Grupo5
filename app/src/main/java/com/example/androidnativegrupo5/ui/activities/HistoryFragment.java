package com.example.androidnativegrupo5.ui.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.databinding.FragmentHistoryBinding;
import com.example.androidnativegrupo5.data.model.ActivityHistoryItem;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.data.local.TokenManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HistoryAdapter adapter;

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    private String fromDate = null;
    private String toDate = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = Navigation.findNavController(view);

        adapter = new HistoryAdapter(item -> {
            Bundle bundle = new Bundle();
            bundle.putLong("activityId", item.getActivityId());
            navController.navigate(R.id.action_HistoryFragment_to_DetailFragment, bundle);
        });

        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistory.setAdapter(adapter);

        binding.inputFromDate.setOnClickListener(v -> showDatePicker(true));
        binding.inputToDate.setOnClickListener(v -> showDatePicker(false));

        binding.btnApplyFilters.setOnClickListener(v -> loadHistory());

        binding.btnClearFilters.setOnClickListener(v -> {
            fromDate = null;
            toDate = null;
            binding.inputFromDate.setText("");
            binding.inputToDate.setText("");
            binding.inputDestination.setText("");
            loadHistory();
        });

        loadHistory();
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                    );

                    if (isFromDate) {
                        fromDate = selectedDate;
                        binding.inputFromDate.setText(selectedDate);
                    } else {
                        toDate = selectedDate;
                        binding.inputToDate.setText(selectedDate);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void loadHistory() {
        if (binding == null) return;

        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "Inicie sesión para ver su historial", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBarHistory.setVisibility(View.VISIBLE);
        binding.textEmptyHistory.setVisibility(View.GONE);

        String destination = binding.inputDestination.getText() != null
                ? binding.inputDestination.getText().toString().trim()
                : null;

        if (TextUtils.isEmpty(destination)) {
            destination = null;
        }

        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        apiService.getHistory(fromDate, toDate, destination).enqueue(new Callback<List<ActivityHistoryItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<ActivityHistoryItem>> call,
                                   @NonNull Response<List<ActivityHistoryItem>> response) {

                if (!isAdded() || binding == null) return;

                binding.progressBarHistory.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<ActivityHistoryItem> items = response.body();
                    adapter.setItems(items);

                    if (items.isEmpty()) {
                        binding.textEmptyHistory.setVisibility(View.VISIBLE);
                        binding.recyclerHistory.setVisibility(View.GONE);
                    } else {
                        binding.textEmptyHistory.setVisibility(View.GONE);
                        binding.recyclerHistory.setVisibility(View.VISIBLE);
                    }
                } else {
                    binding.textEmptyHistory.setVisibility(View.VISIBLE);
                    binding.recyclerHistory.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error al cargar historial", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ActivityHistoryItem>> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;

                binding.progressBarHistory.setVisibility(View.GONE);
                binding.textEmptyHistory.setVisibility(View.VISIBLE);
                binding.recyclerHistory.setVisibility(View.GONE);

                Toast.makeText(requireContext(), "Error de conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
