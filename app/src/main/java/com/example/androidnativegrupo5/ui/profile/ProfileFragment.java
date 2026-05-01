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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.UserPreferences;
import com.example.androidnativegrupo5.data.model.UserResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.utils.NetworkUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

    private static final String TAG = "ProfileFragment";

    @Inject
    ApiService apiService;

    @Inject
    TokenManager tokenManager;

    private TextInputEditText usernameEditText, emailEditText, phoneEditText;
    private AutoCompleteTextView categorySpinner, destinationSpinner, durationSpinner;
    private Slider budgetSlider;
    private Button saveButton, logoutButton;
    private ProgressBar progressBar;
    private ImageView profileImageView;
    private TextView userNameDisplay;
    private Uri selectedImageUri;

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
        Log.d(TAG, "onViewCreated: Iniciando pantalla de perfil");

        userNameDisplay = view.findViewById(R.id.user_name_display);
        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        profileImageView = view.findViewById(R.id.profileImage);
        
        Button btnMyReservations = view.findViewById(R.id.btnMyReservations);
        Button btnHistory = view.findViewById(R.id.btnMyHistory);
        Button btnFavorites = view.findViewById(R.id.btnFavorites);
        logoutButton = view.findViewById(R.id.logoutButton);
        saveButton = view.findViewById(R.id.saveButton);
        progressBar = view.findViewById(R.id.progressBar);

        categorySpinner = view.findViewById(R.id.categorySpinner);
        destinationSpinner = view.findViewById(R.id.destinationSpinner);
        budgetSlider = view.findViewById(R.id.budgetSlider);

        setupSpinners();
        
        // Estrategia: Cargar primero lo que hay en cache (Offline)
        loadOfflineProfile();
        
        // Intentar actualizar desde el servidor si hay internet
        if (NetworkUtils.isOnline(requireContext())) {
            loadProfile();
        } else {
            Toast.makeText(getContext(), "Modo offline: mostrando datos guardados", Toast.LENGTH_SHORT).show();
        }

        if (saveButton != null) saveButton.setOnClickListener(v -> {
            if (NetworkUtils.isOnline(requireContext())) {
                saveProfile();
            } else {
                Toast.makeText(getContext(), "No puedes editar el perfil sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
        
        if (logoutButton != null) logoutButton.setOnClickListener(v -> showLogoutConfirmation());

        btnMyReservations.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_ProfileFragment_to_MyReservationsFragment));

        btnHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_ProfileFragment_to_HistoryFragment));

        if (btnFavorites != null) {
            btnFavorites.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_ProfileFragment_to_FavoritesFragment));
        }
    }

    private void loadOfflineProfile() {
        UserResponse cachedUser = tokenManager.getUserProfile();
        if (cachedUser != null) {
            Log.d(TAG, "Cargando perfil desde caché local");
            populateFields(cachedUser);
        }
    }

    private void loadProfile() {
        Log.d(TAG, "Solicitando perfil al servidor...");
        setLoading(true);
        apiService.getMyProfile().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Perfil cargado con éxito. Guardando en caché.");
                    tokenManager.saveUserProfile(response.body());
                    populateFields(response.body());
                } else if (response.code() == 401) {
                    handleLogout();
                }
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Error de red: se mantendrán los datos offline.");
                setLoading(false);
            }
        });
    }

    private void populateFields(UserResponse user) {
        usernameEditText.setText(user.getUsername());
        if (userNameDisplay != null) userNameDisplay.setText(user.getUsername());
        emailEditText.setText(user.getEmail());
        phoneEditText.setText(user.getPhone());
        
        if (user.getProfileImageUrl() != null && isAdded()) {
            Glide.with(this).load(user.getProfileImageUrl()).circleCrop().into(profileImageView);
        }

        UserPreferences prefs = user.getPreferences();
        if (prefs != null) {
            if (prefs.getPreferredCategory() != null) categorySpinner.setText(prefs.getPreferredCategory(), false);
            if (prefs.getPreferredDestination() != null) destinationSpinner.setText(prefs.getPreferredDestination(), false);
            if (prefs.getMaxPrice() != null) budgetSlider.setValue(prefs.getMaxPrice().floatValue());
        }
    }

    private void saveProfile() {
        UserPreferences prefs = new UserPreferences(
                categorySpinner.getText().toString(),
                (int) budgetSlider.getValue(),
                destinationSpinner.getText().toString(),
                ""
        );

        UserResponse req = new UserResponse();
        req.setUsername(usernameEditText.getText().toString().trim());
        req.setPhone(phoneEditText.getText().toString().trim());
        req.setEmail(emailEditText.getText().toString().trim());
        req.setPreferences(prefs);
        
        setLoading(true);
        apiService.updateProfile(req).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveUserProfile(response.body());
                    Toast.makeText(getContext(), "Perfil actualizado y guardado", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<UserResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(getContext(), "Error al guardar en el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinners() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, categories);
        categorySpinner.setAdapter(catAdapter);
        ArrayAdapter<String> destAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, destinations);
        destinationSpinner.setAdapter(destAdapter);
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que quieres salir?")
                .setPositiveButton("Salir", (dialog, which) -> handleLogout())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void handleLogout() {
        tokenManager.clearToken();
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.WelcomeFragment);
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (saveButton != null) saveButton.setEnabled(!isLoading);
    }
}
