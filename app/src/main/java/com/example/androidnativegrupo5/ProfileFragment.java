package com.example.androidnativegrupo5;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.androidnativegrupo5.model.UserPreferences;
import com.example.androidnativegrupo5.model.UserResponse;
import com.example.androidnativegrupo5.network.ApiService;
import com.example.androidnativegrupo5.network.RetrofitClient;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    @Inject
    ApiService apiService;

    private TextInputEditText usernameEditText, emailEditText, phoneEditText, imageUrlEditText;
    private Spinner categorySpinner, destinationSpinner, durationSpinner;
    private Slider budgetSlider;
    private Button saveButton;
    private ProgressBar progressBar;
    private String token;

    private final List<String> categories = Arrays.asList("Aventura", "Cultura", "Gastronomía", "Bienestar", "Naturaleza");
    private final List<String> destinations = Arrays.asList("Bariloche", "Buenos Aires", "Mendoza", "Salta", "Iguazú");
    private final List<String> durations = Arrays.asList("1-2 horas", "Media jornada", "Día completo");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        token = getAuthToken();

        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        imageUrlEditText = view.findViewById(R.id.imageUrlEditText);

        categorySpinner = view.findViewById(R.id.categorySpinner);
        destinationSpinner = view.findViewById(R.id.destinationSpinner);
        durationSpinner = view.findViewById(R.id.durationSpinner);
        budgetSlider = view.findViewById(R.id.budgetSlider);

        saveButton = view.findViewById(R.id.saveButton);
        progressBar = view.findViewById(R.id.progressBar);

        setupSpinners();

        if (token == null) {
            Toast.makeText(getContext(), "Error: Token no encontrado. Inicie sesión.", Toast.LENGTH_LONG).show();
            return;
        }

        loadProfile();

        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void setupSpinners() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(catAdapter);

        ArrayAdapter<String> destAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, destinations);
        destAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        destinationSpinner.setAdapter(destAdapter);

        ArrayAdapter<String> durAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, durations);
        durAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(durAdapter);
    }

    private String getAuthToken() {
        if (getContext() == null) return null;
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String savedToken = prefs.getString("auth_token", null);
        if (savedToken != null && !savedToken.startsWith("Bearer ")) {
            return "Bearer " + savedToken;
        }
        return savedToken;
    }

    private void loadProfile() {
        setLoading(true);
        apiService.getMyProfile(token).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    populateFields(response.body());
                } else {
                    String errorMsg = "Error al cargar perfil";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.e("ProfileFragment", "Error loading profile: " + response.code() + " " + errorMsg);
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateFields(UserResponse user) {
        usernameEditText.setText(user.getUsername());
        emailEditText.setText(user.getEmail());
        phoneEditText.setText(user.getPhone());
        imageUrlEditText.setText(user.getProfileImageUrl());

        UserPreferences prefs = user.getPreferences();
        if (prefs != null) {
            if (prefs.getPreferredCategory() != null) {
                categorySpinner.setSelection(categories.indexOf(prefs.getPreferredCategory()));
            }
            if (prefs.getPreferredDestination() != null) {
                destinationSpinner.setSelection(destinations.indexOf(prefs.getPreferredDestination()));
            }
            if (prefs.getActivityDuration() != null) {
                durationSpinner.setSelection(durations.indexOf(prefs.getActivityDuration()));
            }
            if (prefs.getMaxPrice() != null) {
                budgetSlider.setValue(prefs.getMaxPrice().floatValue());
            }
        }
    }

    private void saveProfile() {
        String username = usernameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String imageUrl = imageUrlEditText.getText().toString().trim();

        UserPreferences prefs = new UserPreferences(
                categorySpinner.getSelectedItem().toString(),
                (int) budgetSlider.getValue(),
                destinationSpinner.getSelectedItem().toString(),
                durationSpinner.getSelectedItem().toString()
        );

        UserResponse updateRequest = new UserResponse(username, phone, imageUrl, prefs);

        setLoading(true);
        apiService.updateProfile(token, updateRequest).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "✓ Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Error al actualizar perfil", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (saveButton != null) saveButton.setEnabled(!isLoading);
    }
}
