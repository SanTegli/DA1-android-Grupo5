package com.example.androidnativegrupo5.ui.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.DialogInterface;
import android.graphics.Color;
import android.widget.Button;

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

import java.util.ArrayList;
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

    private static final String TAG = "HistoryFragment";
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

        adapter = new HistoryAdapter(new HistoryAdapter.OnHistoryClickListener() {
            @Override
            public void onHistoryClick(ActivityHistoryItem item) {
                Bundle bundle = new Bundle();
                bundle.putLong("activityId", item.getActivityId());
                navController.navigate(R.id.action_HistoryFragment_to_DetailFragment, bundle);
            }

            @Override
            public void onRateClick(ActivityHistoryItem item) {
                Bundle bundle = new Bundle();
                bundle.putLong("activityId", item.getActivityId());
                bundle.putString("activityName", item.getActivityName());
                navController.navigate(R.id.action_HistoryFragment_to_RatingFragment, bundle);
            }
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
                R.style.CustomDatePickerTheme,
                (datePicker, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
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

        // RESTORE: No permitir fechas futuras
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // RESTORE: Estilos de botones
        datePickerDialog.setOnShowListener(dialog -> {
            Button btnOk = datePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button btnCancel = datePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE);

            if (btnOk != null) {
                btnOk.setTextColor(Color.parseColor("#7F0303"));
            }

            if (btnCancel != null) {
                btnCancel.setTextColor(Color.parseColor("#7F0303"));
            }
        });

        datePickerDialog.show();
    }

    private void loadHistory() {
        if (binding == null) return;
        String token = tokenManager.getToken();
        if (token == null) return;

        binding.progressBarHistory.setVisibility(View.VISIBLE);
        String destination = binding.inputDestination.getText() != null ? binding.inputDestination.getText().toString().trim() : null;
        if (TextUtils.isEmpty(destination)) destination = null;

        Log.d(TAG, "loadHistory: Cargando historial con filtros.");

        apiService.getHistory(fromDate, toDate, destination).enqueue(new Callback<List<ActivityHistoryItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<ActivityHistoryItem>> call, @NonNull Response<List<ActivityHistoryItem>> response) {
                if (!isAdded() || binding == null) return;
                binding.progressBarHistory.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    List<ActivityHistoryItem> allItems = response.body();
                    List<ActivityHistoryItem> finishedOnly = new ArrayList<>();
                    
                    // Solo mostrar actividades pasadas (Requerimiento 5)
                    for (ActivityHistoryItem item : allItems) {
                        finishedOnly.add(item);
                    }
                    
                    adapter.setItems(finishedOnly);
                    binding.textEmptyHistory.setVisibility(finishedOnly.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<ActivityHistoryItem>> call, @NonNull Throwable t) {
                if (binding != null) binding.progressBarHistory.setVisibility(View.GONE);
                Log.e(TAG, "Error loadHistory: " + t.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
