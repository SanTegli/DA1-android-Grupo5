package com.example.androidnativegrupo5.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.UserPreferences;
import com.example.androidnativegrupo5.data.model.UserResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.data.local.TokenManager;
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

    @Inject
    TokenManager tokenManager;

    private TextInputEditText usernameEditText, emailEditText, phoneEditText;
    private AutoCompleteTextView categorySpinner, destinationSpinner, durationSpinner;
    private Slider budgetSlider;
    private Button saveButton;
    private ProgressBar progressBar;
    private ImageView profileImageView;
    private Uri selectedImageUri;

    private final List<String> categories = Arrays.asList("Aventura", "Cultura", "Gastronomía", "Bienestar", "Naturaleza");
    private final List<String> destinations = Arrays.asList("Bariloche", "Buenos Aires", "Mendoza", "Salta", "Iguazú");
    private final List<String> durations = Arrays.asList("1-2 horas", "Media jornada", "Día completo");

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).circleCrop().into(profileImageView);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        profileImageView = view.findViewById(R.id.profileImage);
        Button btnMyReservations = view.findViewById(R.id.btnMyReservations);
        Button btnHistory = view.findViewById(R.id.btnMyHistory);

        // Bloquear edición de email
        if (emailEditText != null) {
            emailEditText.setFocusable(false);
            emailEditText.setClickable(false);
            emailEditText.setEnabled(false);
        }

        categorySpinner = view.findViewById(R.id.categorySpinner);
        destinationSpinner = view.findViewById(R.id.destinationSpinner);
        durationSpinner = view.findViewById(R.id.durationSpinner);
        budgetSlider = view.findViewById(R.id.budgetSlider);

        saveButton = view.findViewById(R.id.saveButton);
        progressBar = view.findViewById(R.id.progressBar);

        setupSpinners();

        loadProfile();

        saveButton.setOnClickListener(v -> saveProfile());

        profileImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        btnMyReservations.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_ProfileFragment_to_MyReservationsFragment));

        btnHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_ProfileFragment_to_HistoryFragment));
    }

    private void setupSpinners() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, categories);
        categorySpinner.setAdapter(catAdapter);

        ArrayAdapter<String> destAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, destinations);
        destinationSpinner.setAdapter(destAdapter);

        ArrayAdapter<String> durAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, durations);
        durationSpinner.setAdapter(durAdapter);
    }

    private void loadProfile() {
        setLoading(true);
        apiService.getMyProfile().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    populateFields(response.body());
                } else {
                    Log.e("ProfileFragment", "Error loading profile: " + response.code());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error al cargar perfil", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                setLoading(false);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void populateFields(UserResponse user) {
        if (user == null) return;
        usernameEditText.setText(user.getUsername());
        emailEditText.setText(user.getEmail());
        phoneEditText.setText(user.getPhone());

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .circleCrop()
                    .into(profileImageView);
        }

        UserPreferences prefs = user.getPreferences();
        if (prefs != null) {
            if (prefs.getPreferredCategory() != null) {
                categorySpinner.setText(prefs.getPreferredCategory(), false);
            }
            if (prefs.getPreferredDestination() != null) {
                destinationSpinner.setText(prefs.getPreferredDestination(), false);
            }
            if (prefs.getActivityDuration() != null) {
                durationSpinner.setText(prefs.getActivityDuration(), false);
            }
            if (prefs.getMaxPrice() != null) {
                budgetSlider.setValue(prefs.getMaxPrice().floatValue());
            }
        }
    }

    private void saveProfile() {
        String username = usernameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String imageUrl = selectedImageUri != null ? selectedImageUri.toString() : null;

        UserPreferences prefs = new UserPreferences(
                categorySpinner.getText().toString(),
                (int) budgetSlider.getValue(),
                destinationSpinner.getText().toString(),
                durationSpinner.getText().toString()
        );

        UserResponse updateRequest = new UserResponse();
        updateRequest.setUsername(username);
        updateRequest.setPhone(phone);
        updateRequest.setEmail(email);
        updateRequest.setPreferences(prefs);
        if (imageUrl != null) {
            updateRequest.setProfileImageUrl(imageUrl);
        }

        setLoading(true);
        apiService.updateProfile(updateRequest).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "✓ Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("ProfileFragment", "Update fail: " + response.code());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error al actualizar perfil", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                setLoading(false);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (saveButton != null) saveButton.setEnabled(!isLoading);
    }
}
