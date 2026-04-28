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
import com.example.androidnativegrupo5.data.local.db.FavoriteActivity;
import com.example.androidnativegrupo5.data.local.db.FavoriteDao;
import com.example.androidnativegrupo5.data.model.Activity;
import com.example.androidnativegrupo5.data.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.data.model.Rating;
import com.example.androidnativegrupo5.data.network.ApiService;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class DetailFragment extends Fragment implements OnMapReadyCallback {

    @Inject
    ApiService apiService;

    @Inject
    FavoriteDao favoriteDao;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private FragmentDetailBinding binding;
    private Activity activity;
    private CommentAdapter commentAdapter;
    private GoogleMap googleMap;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long activityId = getArguments() != null ? getArguments().getLong("activityId", -1) : -1;
        if (activityId == -1) {
            Toast.makeText(getContext(), "Error al cargar actividad", Toast.LENGTH_SHORT).show();
            return;
        }

        setupCommentsRecyclerView();
        try { MapsInitializer.initialize(requireContext()); } catch (Exception ignored) {}

        binding.mapView.onCreate(savedInstanceState);
        binding.mapView.onResume();
        binding.mapView.getMapAsync(this);

        loadActivityDetail(activityId);
        loadAvailability(activityId);
        loadComments(activityId);
        checkFavoriteStatus(activityId);
    }

    private void checkFavoriteStatus(long activityId) {
        executor.execute(() -> {
            boolean isFav = favoriteDao.isFavorite(activityId);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    binding.btnFavoriteDetail.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                });
            }
        });

        binding.btnFavoriteDetail.setOnClickListener(v -> {
            executor.execute(() -> {
                boolean isFav = favoriteDao.isFavorite(activityId);
                if (isFav) {
                    favoriteDao.delete(favoriteDao.getFavoriteById(activityId));
                } else if (activity != null) {
                    favoriteDao.insert(new FavoriteActivity(
                            activity.getId(), activity.getName(), activity.getDestination(),
                            activity.getPrice(), activity.getAvailableSlots(), activity.getImageUrl()
                    ));
                }
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.btnFavoriteDetail.setImageResource(!isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                        Toast.makeText(getContext(), !isFav ? "Añadido a favoritos" : "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                    });
                }
            });
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

                    Glide.with(requireContext()).load(activity.getImageUrl()).placeholder(android.R.drawable.ic_menu_gallery).into(binding.imageDetail);
                    setupMeetingPoint();

                    binding.btnReserve.setOnClickListener(v -> {
                        Bundle bundle = new Bundle();
                        bundle.putLong("activityId", activity.getId());
                        bundle.putString("activityName", activity.getName());
                        bundle.putFloat("activityPrice", (float) activity.getPrice());
                        Navigation.findNavController(v).navigate(R.id.action_DetailFragment_to_ReservationFragment, bundle);
                    });
                }
            }
            @Override public void onFailure(@NonNull Call<Activity> call, @NonNull Throwable t) {}
        });
    }

    private void setupMeetingPoint() {
        if (activity == null || binding == null) return;
        String address = activity.getMeetingPointAddress();
        Double lat = activity.getMeetingPointLat();
        Double lng = activity.getMeetingPointLng();

        binding.textMeetingPoint.setText(address == null || address.isEmpty() ? "No disponible" : address);
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
            googleMap.addMarker(new MarkerOptions().position(meetingPoint).title("Punto de encuentro"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(meetingPoint, 15f));
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
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng)));
        }
    }

    @Override public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        if (activity != null) setupMeetingPoint();
    }

    private void loadComments(Long activityId) {
        apiService.getRatingsByActivity(activityId).enqueue(new Callback<List<Rating>>() {
            @Override
            public void onResponse(@NonNull Call<List<Rating>> call, @NonNull Response<List<Rating>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentAdapter.setComments(response.body());
                    binding.textNoComments.setVisibility(response.body().isEmpty() ? View.VISIBLE : View.GONE);
                    binding.textCommentsHeader.setText("Comentarios (" + response.body().size() + ")");
                }
            }
            @Override public void onFailure(@NonNull Call<List<Rating>> call, @NonNull Throwable t) {}
        });
    }

    private void loadAvailability(Long activityId) {
        apiService.getAvailability(activityId).enqueue(new Callback<List<AvailabilitySlotResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AvailabilitySlotResponse>> call, @NonNull Response<List<AvailabilitySlotResponse>> response) {
                if (response.isSuccessful() && response.body() != null) renderAvailableDays(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<AvailabilitySlotResponse>> call, @NonNull Throwable t) {}
        });
    }

    private void renderAvailableDays(List<AvailabilitySlotResponse> availabilityList) {
        Set<DayOfWeek> availableDays = new HashSet<>();
        for (AvailabilitySlotResponse item : availabilityList) {
            try { availableDays.add(LocalDate.parse(item.getDate()).getDayOfWeek()); } catch (Exception ignored) {}
        }
        setDayStyle(binding.dayMon, availableDays.contains(DayOfWeek.MONDAY));
        setDayStyle(binding.dayTue, availableDays.contains(DayOfWeek.TUESDAY));
        setDayStyle(binding.dayWed, availableDays.contains(DayOfWeek.WEDNESDAY));
        setDayStyle(binding.dayThu, availableDays.contains(DayOfWeek.THURSDAY));
        setDayStyle(binding.dayFri, availableDays.contains(DayOfWeek.FRIDAY));
        setDayStyle(binding.daySat, availableDays.contains(DayOfWeek.SATURDAY));
        setDayStyle(binding.daySun, availableDays.contains(DayOfWeek.SUNDAY));
    }

    private void setDayStyle(TextView textView, boolean available) {
        textView.setTextColor(ContextCompat.getColor(requireContext(), available ? android.R.color.black : android.R.color.darker_gray));
        textView.setBackgroundResource(available ? R.drawable.bg_day_available : R.drawable.bg_day_unavailable);
    }

    private String safe(String value) { return value != null ? value : "-"; }
    @Override public void onResume() { super.onResume(); if (binding != null) binding.mapView.onResume(); }
    @Override public void onPause() { if (binding != null) binding.mapView.onPause(); super.onPause(); }
    @Override public void onDestroyView() { if (binding != null) binding.mapView.onDestroy(); super.onDestroyView(); binding = null; }
}
