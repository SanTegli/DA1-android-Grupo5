package com.example.androidnativegrupo5.ui.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.androidnativegrupo5.databinding.BottomSheetFiltersBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilterBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetFiltersBinding binding;
    private OnFiltersAppliedListener listener;

    private List<String> categories = new ArrayList<>();
    private List<String> destinations = new ArrayList<>();

    private String currentSearch = "";
    private String currentCategory = null;
    private String currentDestination = null;
    private Float currentMinPrice = 0f;
    private Float currentMaxPrice = 100000f;

    public interface OnFiltersAppliedListener {
        void onFiltersApplied(String search, String category, String destination, Float minPrice, Float maxPrice);
    }

    public void setOnFiltersAppliedListener(OnFiltersAppliedListener listener) {
        this.listener = listener;
    }

    public void setFilterOptions(List<String> categories, List<String> destinations) {
        this.categories = categories != null ? categories : new ArrayList<>();
        this.destinations = destinations != null ? destinations : new ArrayList<>();
    }

    public void setCurrentFilters(String search, String category, String destination, Float minPrice, Float maxPrice) {
        this.currentSearch = search != null ? search : "";
        this.currentCategory = category;
        this.currentDestination = destination;
        this.currentMinPrice = minPrice != null ? minPrice : 0f;
        this.currentMaxPrice = maxPrice != null ? maxPrice : 100000f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetFiltersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupSpinners();
        setupInitialValues();

        binding.rangeSliderPrice.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            binding.textPriceRange.setText(
                    String.format(
                            Locale.getDefault(),
                            "Rango de precio ($%.0f - $%.0f)",
                            values.get(0),
                            values.get(1)
                    )
            );
        });

        binding.btnApplyFilters.setOnClickListener(v -> {
            if (listener != null) {
                String search = binding.editSearch.getText() != null
                        ? binding.editSearch.getText().toString().trim()
                        : "";

                String category = binding.spinnerCategory.getSelectedItem() != null
                        ? binding.spinnerCategory.getSelectedItem().toString()
                        : null;

                String destination = binding.spinnerDestination.getSelectedItem() != null
                        ? binding.spinnerDestination.getSelectedItem().toString()
                        : null;

                if ("Todas".equalsIgnoreCase(category)) category = null;
                if ("Todos".equalsIgnoreCase(destination)) destination = null;

                List<Float> priceValues = binding.rangeSliderPrice.getValues();
                Float minPrice = priceValues.get(0);
                Float maxPrice = priceValues.get(1);

                listener.onFiltersApplied(search, category, destination, minPrice, maxPrice);
            }

            dismiss();
        });

        binding.btnClearFilters.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFiltersApplied("", null, null, 0f, 100000f);
            }
            dismiss();
        });
    }

    private void setupSpinners() {
        List<String> categoryOptions = new ArrayList<>();
        categoryOptions.add("Todas");
        categoryOptions.addAll(categories);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categoryOptions
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(categoryAdapter);

        List<String> destinationOptions = new ArrayList<>();
        destinationOptions.add("Todos");
        destinationOptions.addAll(destinations);

        ArrayAdapter<String> destinationAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                destinationOptions
        );
        destinationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDestination.setAdapter(destinationAdapter);
    }

    private void setupInitialValues() {
        binding.editSearch.setText(currentSearch);

        setSpinnerSelection(binding.spinnerCategory, currentCategory, "Todas");
        setSpinnerSelection(binding.spinnerDestination, currentDestination, "Todos");

        float min = currentMinPrice != null ? currentMinPrice : 0f;
        float max = currentMaxPrice != null ? currentMaxPrice : 100000f;

        if (min < 0f) min = 0f;
        if (max > 100000f) max = 100000f;
        if (min > max) min = 0f;

        binding.rangeSliderPrice.setValues(min, max);

        binding.textPriceRange.setText(
                String.format(
                        Locale.getDefault(),
                        "Rango de precio ($%.0f - $%.0f)",
                        min,
                        max
                )
        );
    }

    private void setSpinnerSelection(android.widget.Spinner spinner, String value, String defaultValue) {
        if (spinner.getAdapter() == null) return;

        String target = value != null ? value : defaultValue;

        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            String item = spinner.getAdapter().getItem(i).toString();
            if (item.equalsIgnoreCase(target)) {
                spinner.setSelection(i);
                return;
            }
        }

        spinner.setSelection(0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}