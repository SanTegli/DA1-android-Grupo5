package com.example.androidnativegrupo5;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.androidnativegrupo5.databinding.BottomSheetFiltersBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;
import java.util.Locale;

public class FilterBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetFiltersBinding binding;
    private OnFiltersAppliedListener listener;

    public interface OnFiltersAppliedListener {
        void onFiltersApplied(String search, String category, String destination, Float minPrice, Float maxPrice);
    }

    public void setOnFiltersAppliedListener(OnFiltersAppliedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetFiltersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupSpinners();

        binding.rangeSliderPrice.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            binding.textPriceRange.setText(String.format(Locale.getDefault(), "Rango de precio ($%.0f - $%.0f)", values.get(0), values.get(1)));
        });

        binding.btnApplyFilters.setOnClickListener(v -> {
            if (listener != null) {
                String search = binding.editSearch.getText().toString();
                String category = binding.spinnerCategory.getSelectedItem().toString();
                String destination = binding.spinnerDestination.getSelectedItem().toString();
                
                if (category.equals("Todas")) category = null;
                if (destination.equals("Todos")) destination = null;
                
                List<Float> priceValues = binding.rangeSliderPrice.getValues();
                Float minPrice = priceValues.get(0);
                Float maxPrice = priceValues.get(1);

                listener.onFiltersApplied(search, category, destination, minPrice, maxPrice);
            }
            dismiss();
        });
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.categories_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<CharSequence> destinationAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.destinations_array, android.R.layout.simple_spinner_item);
        destinationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDestination.setAdapter(destinationAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
