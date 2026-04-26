package com.example.androidnativegrupo5.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;
import com.example.androidnativegrupo5.databinding.ActivityMainBinding;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject TokenManager tokenManager;
    @Inject ReservaDao reservaDao;

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navController = Navigation.findNavController(
                this,
                R.id.nav_host_fragment_content_main
        );

        if (savedInstanceState == null && tokenManager.getToken() != null) {
            navController.navigate(R.id.MyReservationsFragment);
        }

        binding.btnBack.setOnClickListener(v -> navController.navigateUp());

        binding.navDiscover.setOnClickListener(v ->
                navController.navigate(R.id.FirstFragment)
        );

        binding.btnMyReservations.setOnClickListener(v ->
                navController.navigate(R.id.MyReservationsFragment)
        );

        binding.navProfile.setOnClickListener(v ->
                navController.navigate(R.id.ProfileFragment)
        );

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean hideBars =
                    destination.getId() == R.id.LoginFragment ||
                            destination.getId() == R.id.RegisterFragment ||
                            destination.getId() == R.id.WelcomeFragment;

            binding.header.setVisibility(hideBars ? View.GONE : View.VISIBLE);
            binding.footer.setVisibility(hideBars ? View.GONE : View.VISIBLE);

            if (destination.getId() == R.id.FirstFragment || hideBars) {
                binding.btnBack.setVisibility(View.GONE);
            } else {
                binding.btnBack.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp()
                || super.onSupportNavigateUp();
    }
}