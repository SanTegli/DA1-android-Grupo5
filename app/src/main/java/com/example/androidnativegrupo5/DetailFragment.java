package com.example.androidnativegrupo5;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.databinding.FragmentDetailBinding;
import com.example.androidnativegrupo5.model.Activity;
import com.example.androidnativegrupo5.network.ApiService;
import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class DetailFragment extends Fragment {

    @Inject
    ApiService apiService;

    private FragmentDetailBinding binding;
    private Activity activity;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long activityId = getArguments().getLong("activityId", -1);

        if (activityId == -1) {
            Toast.makeText(getContext(), "Error al cargar actividad", Toast.LENGTH_SHORT).show();
            return;
        }

        loadActivityDetail(activityId);
    }

    private void loadActivityDetail(Long id) {
        apiService.getActivityById(id).enqueue(new Callback<Activity>() {
            @Override
            public void onResponse(@NonNull Call<Activity> call, @NonNull Response<Activity> response) {

                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {

                    activity = response.body();

                    binding.textTitle.setText(activity.getName());
                    binding.textDestination.setText(activity.getDestination());
                    binding.textCategory.setText(activity.getCategory());
                    binding.textDuration.setText(activity.getDuration());
                    binding.textDescription.setText(activity.getDescription());

                    if (activity.getPrice() <= 0) {
                        binding.textPrice.setText("Gratis");
                    } else {
                        binding.textPrice.setText("$" + activity.getPrice());
                    }

                    Glide.with(requireContext())
                            .load(activity.getImageUrl())
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .into(binding.imageDetail);

                    binding.btnReserve.setOnClickListener(v -> {
                        Bundle bundle = new Bundle();
                        bundle.putLong("activityId", activity.getId());
                        bundle.putString("activityName", activity.getName());
                        bundle.putFloat("activityPrice", (float) activity.getPrice());

                        Navigation.findNavController(v)
                                .navigate(R.id.action_DetailFragment_to_ReservationFragment, bundle);
                    });

                } else {
                    Toast.makeText(getContext(), "Error al cargar detalle", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Activity> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
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