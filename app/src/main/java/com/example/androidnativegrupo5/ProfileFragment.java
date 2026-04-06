package com.example.androidnativegrupo5;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.androidnativegrupo5.model.UserResponse;
import com.example.androidnativegrupo5.network.ApiService;
import com.example.androidnativegrupo5.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextInputEditText usernameEditText, emailEditText, phoneEditText, imageUrlEditText;
    private CheckBox cbAventura, cbCultura, cbGastronomia, cbNaturaleza, cbRelax;
    private Button saveButton;
    private ProgressBar progressBar;
    private ApiService apiService;
    private String token;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        token = getAuthToken();

        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        imageUrlEditText = view.findViewById(R.id.imageUrlEditText);

        cbAventura = view.findViewById(R.id.cbAventura);
        cbCultura = view.findViewById(R.id.cbCultura);
        cbGastronomia = view.findViewById(R.id.cbGastronomia);
        cbNaturaleza = view.findViewById(R.id.cbNaturaleza);
        cbRelax = view.findViewById(R.id.cbRelax);

        saveButton = view.findViewById(R.id.saveButton);
        progressBar = view.findViewById(R.id.progressBar);

        if (token == null) {
            Toast.makeText(getContext(), "Error: Token no encontrado. Inicie sesión.", Toast.LENGTH_LONG).show();
            return;
        }

        loadProfile();

        saveButton.setOnClickListener(v -> saveProfile());
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
                    Toast.makeText(getContext(), "Error al cargar perfil", Toast.LENGTH_SHORT).show();
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

        String prefs = user.getTravelPreferences();
        if (prefs != null) {
            List<String> prefList = Arrays.asList(prefs.split(","));
            cbAventura.setChecked(prefList.contains("aventura"));
            cbCultura.setChecked(prefList.contains("cultura"));
            cbGastronomia.setChecked(prefList.contains("gastronomia"));
            cbNaturaleza.setChecked(prefList.contains("naturaleza"));
            cbRelax.setChecked(prefList.contains("relax"));
        }
    }

    private void saveProfile() {
        String username = usernameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String imageUrl = imageUrlEditText.getText().toString().trim();
        String prefs = getSelectedPreferences();

        UserResponse updateRequest = new UserResponse(username, phone, imageUrl, prefs);

        setLoading(true);
        apiService.updateProfile(token, updateRequest).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
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

    private String getSelectedPreferences() {
        List<String> selected = new ArrayList<>();
        if (cbAventura.isChecked()) selected.add("aventura");
        if (cbCultura.isChecked()) selected.add("cultura");
        if (cbGastronomia.isChecked()) selected.add("gastronomia");
        if (cbNaturaleza.isChecked()) selected.add("naturaleza");
        if (cbRelax.isChecked()) selected.add("relax");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selected.size(); i++) {
            sb.append(selected.get(i));
            if (i < selected.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!isLoading);
    }
}
