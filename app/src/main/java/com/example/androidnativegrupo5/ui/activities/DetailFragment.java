package com.example.androidnativegrupo5.ui.activities;

import android.content.Intent;
import android.net.Uri;
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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.Activity;
import com.example.androidnativegrupo5.data.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.data.model.Rating;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.ui.favorites.FavoriteViewModel;
import androidx.lifecycle.ViewModelProvider;
import dagger.hilt.android.AndroidEntryPoint;
import com.example.androidnativegrupo5.databinding.FragmentDetailBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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
public class DetailFragment extends Fragment implements OnMapReadyCallback {

    @Inject
    ApiService apiService;

    private FavoriteViewModel favoriteViewModel;
    private FragmentDetailBinding binding;
    private Activity activity;
    private CommentAdapter commentAdapter;
    private GoogleMap googleMap;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentDetailBinding.inflate(inflater, container, false);
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long activityId = getArguments() != null
                ? getArguments().getLong("activityId", -1)
                : -1;

        if (activityId == -1) {
            Toast.makeText(getContext(), "Error al cargar actividad", Toast.LENGTH_SHORT).show();
            return;
        }

        setupCommentsRecyclerView();

        try {
            MapsInitializer.initialize(requireContext());
        } catch (Exception ignored) {
        }

        binding.mapView.onCreate(savedInstanceState);
        binding.mapView.onResume();
        binding.mapView.getMapAsync(this);

        loadActivityDetail(activityId);
        loadAvailability(activityId);
        loadComments(activityId);
        favoriteViewModel.checkFavoriteStatus(activityId);

        binding.btnFavorite.setOnClickListener(v -> {
            if (activity != null) {
                favoriteViewModel.toggleFavorite(activity.getId(), activity);
            }
        });

        favoriteViewModel.getIsFavorite().observe(getViewLifecycleOwner(), isFav -> {
            if (isFav) {
                binding.btnFavorite.setImageResource(com.example.androidnativegrupo5.R.drawable.ic_favorite_filled);
                binding.btnFavorite.setColorFilter(null);
            } else {
                binding.btnFavorite.setImageResource(com.example.androidnativegrupo5.R.drawable.ic_favorite_border);
                binding.btnFavorite.setColorFilter(getResources().getColor(android.R.color.white));
            }
        });
    }

    private void setupCommentsRecyclerView() {
        commentAdapter = new CommentAdapter();
        binding.recyclerComments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerComments.setAdapter(commentAdapter);
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
                    binding.textGuideName.setText(safe(activity.getGuideName()));                    binding.textCategory.setText("Categoría: " + safe(activity.getCategory()));
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

                    setupMeetingPoint();

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

    private void setupMeetingPoint() {
        if (activity == null || binding == null) return;

        String address = activity.getMeetingPointAddress();
        Double lat = activity.getMeetingPointLat();
        Double lng = activity.getMeetingPointLng();

        if (address == null || address.trim().isEmpty()) {
            binding.textMeetingPoint.setText("Punto de encuentro no disponible");
        } else {
            binding.textMeetingPoint.setText(address);
        }

        if (lat == null || lng == null) {
            binding.mapView.setVisibility(View.GONE);
            binding.btnHowToGetThere.setVisibility(View.GONE);
            return;
        }

        binding.mapView.setVisibility(View.VISIBLE);
        binding.btnHowToGetThere.setVisibility(View.VISIBLE);

        LatLng meetingPoint = new LatLng(lat, lng);

        if (googleMap != null) {
            googleMap.clear();

            googleMap.addMarker(new MarkerOptions()
                    .position(meetingPoint)
                    .title("Punto de encuentro"));

            binding.mapView.post(() ->
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(meetingPoint, 15f))
            );
        }

        binding.btnHowToGetThere.setOnClickListener(v -> openNavigation(lat, lng));
    }

    private void openNavigation(Double lat, Double lng) {
        Uri navigationUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
        Intent navigationIntent = new Intent(Intent.ACTION_VIEW, navigationUri);
        navigationIntent.setPackage("com.google.android.apps.maps");

        if (navigationIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(navigationIntent);
        } else {
            Uri browserUri = Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng
            );
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
            startActivity(browserIntent);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (activity != null) {
            setupMeetingPoint();
        }
    }

    private void loadComments(Long activityId) {
        apiService.getRatingsByActivity(activityId).enqueue(new Callback<List<Rating>>() {
            @Override
            public void onResponse(@NonNull Call<List<Rating>> call, @NonNull Response<List<Rating>> response) {
                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Rating> comments = response.body();
                    commentAdapter.setComments(comments);

                    if (comments.isEmpty()) {
                        binding.textNoComments.setVisibility(View.VISIBLE);
                        binding.recyclerComments.setVisibility(View.GONE);
                    } else {
                        binding.textNoComments.setVisibility(View.GONE);
                        binding.recyclerComments.setVisibility(View.VISIBLE);
                        binding.textCommentsHeader.setText("Comentarios (" + comments.size() + ")");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Rating>> call, @NonNull Throwable t) {
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
    public void onResume() {
        super.onResume();
        if (binding != null) {
            binding.mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (binding != null) {
            binding.mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (binding != null) {
            binding.mapView.onLowMemory();
        }
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.mapView.onDestroy();
        }
        super.onDestroyView();
        binding = null;
    }
}