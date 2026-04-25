package com.example.androidnativegrupo5.ui.reservations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.CreateRatingRequest;
import com.example.androidnativegrupo5.data.model.Rating;
import com.example.androidnativegrupo5.databinding.FragmentMyReservationsBinding;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.data.local.TokenManager;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class MyReservationsFragment extends Fragment implements ReservationAdapter.OnReservationActionListener {

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    private FragmentMyReservationsBinding binding;
    private ReservationAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentMyReservationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerReservations.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.btnGoHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.HistoryFragment)
        );

        loadReservations();
    }

    private void loadReservations() {
        String token = tokenManager.getToken();

        if (token == null) {
            Toast.makeText(getContext(), "Inicie sesión para ver sus reservas", Toast.LENGTH_SHORT).show();
            return;
        }

        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        apiService.getMyReservations(authHeader).enqueue(new Callback<List<ReservationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReservationResponse>> call,
                                   @NonNull Response<List<ReservationResponse>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    adapter = new ReservationAdapter(response.body(), MyReservationsFragment.this);
                    binding.recyclerReservations.setAdapter(adapter);
                } else {
                    Toast.makeText(getContext(), "Error al cargar reservas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ReservationResponse>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCancelClick(ReservationResponse reservation) {
        if (reservation.getId() == null) return;

        String token = tokenManager.getToken();

        if (token == null) {
            Toast.makeText(getContext(), "Error de autenticación", Toast.LENGTH_SHORT).show();
            return;
        }

        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        apiService.cancelReservation(authHeader, reservation.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Reserva cancelada con éxito", Toast.LENGTH_SHORT).show();
                    loadReservations(); // Refresh list to update status
                } else {
                    Toast.makeText(getContext(), "Error al cancelar la reserva", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRateClick(ReservationResponse reservation) {
        showRatingDialog(reservation);
    }

    private void showRatingDialog(ReservationResponse reservation) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_rating, null);
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        android.widget.RatingBar ratingActivity = dialogView.findViewById(R.id.rating_activity);
        android.widget.RatingBar ratingGuide = dialogView.findViewById(R.id.rating_guide);
        com.google.android.material.textfield.TextInputEditText etComment = dialogView.findViewById(R.id.et_comment);
        android.widget.Button btnSubmit = dialogView.findViewById(R.id.btn_submit);

        btnSubmit.setOnClickListener(v -> {
            int activityScore = (int) ratingActivity.getRating();
            int guideScore = (int) ratingGuide.getRating();
            String comment = etComment.getText().toString();

            if (activityScore == 0 || guideScore == 0) {
                Toast.makeText(getContext(), "Por favor, califique ambos aspectos", Toast.LENGTH_SHORT).show();
                return;
            }

            submitRating(reservation.getActivityId(), activityScore, guideScore, comment, dialog);
        });

        dialog.show();
    }

    private void submitRating(Long activityId, int activityScore, int guideScore, String comment, androidx.appcompat.app.AlertDialog dialog) {
        String token = tokenManager.getToken();

        if (token == null) return;

        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        CreateRatingRequest request =
                new CreateRatingRequest(activityScore, guideScore, comment);

        apiService.createRating(authHeader, activityId, request).enqueue(new Callback<Rating>() {
            @Override
            public void onResponse(@NonNull Call<Rating> call,
                                   @NonNull Response<Rating> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "¡Gracias por tu calificación!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    // Optional: mark as rated in UI if needed
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Error al enviar calificación";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Rating> call, @NonNull Throwable t) {
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
