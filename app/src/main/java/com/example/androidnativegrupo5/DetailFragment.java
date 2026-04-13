package com.example.androidnativegrupo5;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.databinding.FragmentDetailBinding;
import com.example.androidnativegrupo5.model.Activity;
import com.example.androidnativegrupo5.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.network.ApiService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        loadAvailability(activityId);
    }

    private void loadActivityDetail(Long id) {
        apiService.getActivityById(id).enqueue(new Callback<Activity>() {
            @Override
            public void onResponse(@NonNull Call<Activity> call, @NonNull Response<Activity> response) {

                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {

                    activity = response.body();

                    binding.textTitle.setText(safe(activity.getName()));
                    binding.textDestination.setText("Destino: " + safe(activity.getDestination()));
                    binding.textGuide.setText("Guía: " + safe(activity.getGuideName()));
                    binding.textCategory.setText("Categoría: " + safe(activity.getCategory()));
                    binding.textDuration.setText("Duración: " + safe(activity.getDuration()));
                    binding.textSlots.setText("Cupos generales: " + activity.getAvailableSlots());
                    binding.textDescription.setText(safe(activity.getDescription()));

                    if (activity.getPrice() <= 0) {
                        binding.textPrice.setText("Gratis");
                    } else {
                        binding.textPrice.setText(String.format(Locale.getDefault(), "$%.2f", activity.getPrice()));
                    }

                    Glide.with(requireContext())
                            .load(activity.getImageUrl())
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
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

    private void loadAvailability(Long activityId) {
        apiService.getAvailability(activityId).enqueue(new Callback<List<AvailabilitySlotResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AvailabilitySlotResponse>> call,
                                   @NonNull Response<List<AvailabilitySlotResponse>> response) {

                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<AvailabilitySlotResponse> availabilityList = response.body();
                    renderAvailableDays(availabilityList);
                    renderAvailableSchedules(availabilityList);
                } else {
                    binding.textSchedules.setText("No hay horarios disponibles.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AvailabilitySlotResponse>> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                binding.textSchedules.setText("No se pudo cargar la disponibilidad.");
            }
        });
    }

    private void renderAvailableDays(List<AvailabilitySlotResponse> availabilityList) {
        Set<DayOfWeek> availableDays = new HashSet<>();

        for (AvailabilitySlotResponse item : availabilityList) {
            try {
                LocalDate date = LocalDate.parse(item.getDate());
                availableDays.add(date.getDayOfWeek());
            } catch (Exception ignored) {
            }
        }

        setDayStyle(binding.dayMon, availableDays.contains(DayOfWeek.MONDAY));
        setDayStyle(binding.dayTue, availableDays.contains(DayOfWeek.TUESDAY));
        setDayStyle(binding.dayWed, availableDays.contains(DayOfWeek.WEDNESDAY));
        setDayStyle(binding.dayThu, availableDays.contains(DayOfWeek.THURSDAY));
        setDayStyle(binding.dayFri, availableDays.contains(DayOfWeek.FRIDAY));
        setDayStyle(binding.daySat, availableDays.contains(DayOfWeek.SATURDAY));
        setDayStyle(binding.daySun, availableDays.contains(DayOfWeek.SUNDAY));
    }

    private void renderAvailableSchedules(List<AvailabilitySlotResponse> availabilityList) {
        if (availabilityList.isEmpty()) {
            binding.textSchedules.setText("No hay horarios disponibles.");
            return;
        }

        StringBuilder builder = new StringBuilder();

        int limit = Math.min(availabilityList.size(), 6);
        for (int i = 0; i < limit; i++) {
            AvailabilitySlotResponse item = availabilityList.get(i);
            builder.append("• ")
                    .append(item.getDate())
                    .append(" - ")
                    .append(item.getTime())
                    .append(" | Cupos: ")
                    .append(item.getAvailableSlots());

            if (i < limit - 1) {
                builder.append("\n");
            }
        }

        binding.textSchedules.setText(builder.toString());
    }

    private void setDayStyle(TextView textView, boolean available) {
        if (available) {
            textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
            textView.setBackgroundResource(R.drawable.bg_day_available);
        } else {
            textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            textView.setBackgroundResource(R.drawable.bg_day_unavailable);
        }
    }

    private String safe(String value) {
        return value != null ? value : "-";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}