package com.example.androidnativegrupo5;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.databinding.FragmentDetailBinding;
import com.example.androidnativegrupo5.model.Activity;
import com.example.androidnativegrupo5.network.ApiService;
import com.example.androidnativegrupo5.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailFragment extends Fragment {

    private FragmentDetailBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long activityId = getArguments() != null ? getArguments().getLong("activityId", -1) : -1;

        if (activityId == -1) {
            Toast.makeText(getContext(), "Actividad inválida", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getActivityById(activityId).enqueue(new Callback<Activity>() {
            @Override
            public void onResponse(@NonNull Call<Activity> call, @NonNull Response<Activity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Activity activity = response.body();

                    binding.detailName.setText(activity.getName());
                    binding.detailDescription.setText(activity.getDescription());
                    binding.detailDestination.setText(activity.getDestination());
                    binding.detailCategory.setText(activity.getCategory());
                    binding.detailDuration.setText(activity.getDuration());
                    binding.detailSlots.setText("Cupos disponibles: " + activity.getAvailableSlots());

                    if (activity.getPrice() <= 0) {
                        binding.detailPrice.setText("Gratis");
                    } else {
                        binding.detailPrice.setText("$" + activity.getPrice());
                    }

                    Glide.with(requireContext())
                            .load(activity.getImageUrl())
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
                            .into(binding.detailImage);
                } else {
                    Toast.makeText(getContext(), "No se pudo cargar el detalle", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Activity> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}