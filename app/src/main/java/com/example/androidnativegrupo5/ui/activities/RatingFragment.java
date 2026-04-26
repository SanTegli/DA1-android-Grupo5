package com.example.androidnativegrupo5.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.model.CreateRatingRequest;
import com.example.androidnativegrupo5.data.model.Rating;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.FragmentRatingBinding;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class RatingFragment extends Fragment {

    private FragmentRatingBinding binding;
    private Long activityId;
    private String activityName;

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            activityId = getArguments().getLong("activityId");
            activityName = getArguments().getString("activityName");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRatingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (activityName != null) {
            binding.textActivityName.setText(activityName);
        }

        binding.btnSubmitRating.setOnClickListener(v -> submitRating());
    }

    private void submitRating() {
        if (activityId == null) {
            Toast.makeText(getContext(), "Error: ID de actividad no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        int activityScore = (int) binding.ratingActivity.getRating();
        int guideScore = (int) binding.ratingGuide.getRating();
        String comment = binding.etComment.getText() != null ? binding.etComment.getText().toString() : "";

        if (activityScore == 0 || guideScore == 0) {
            Toast.makeText(getContext(), "Por favor, califica ambos aspectos", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "Sesión expirada", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSubmitRating.setEnabled(false);

        // Se envía la calificación REAL al backend, pero ignoramos el error 400 (que es el de la fecha)
        // para que la UI se comporte como si hubiera funcionado, permitiéndote testear el flujo.
        CreateRatingRequest request = new CreateRatingRequest(activityScore, guideScore, comment);

        apiService.createRating(activityId, request).enqueue(new Callback<Rating>() {
            @Override
            public void onResponse(@NonNull Call<Rating> call, @NonNull Response<Rating> response) {
                if (!isAdded()) return;
                
                // Si es exitoso (200) o si falla con 400 (error de fecha del backend),
                // lo tratamos como éxito en la UI para el testeo.
                if (response.isSuccessful() || response.code() == 400) {
                    String msg = response.isSuccessful() ? "¡Gracias por tu calificación!" : "Testeo: Calificación procesada (bypass de fecha)";
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                } else {
                    binding.btnSubmitRating.setEnabled(true);
                    Toast.makeText(getContext(), "Error al enviar: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Rating> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                binding.btnSubmitRating.setEnabled(true);
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
