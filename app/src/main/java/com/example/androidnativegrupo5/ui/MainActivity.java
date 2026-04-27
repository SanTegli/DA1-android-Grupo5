package com.example.androidnativegrupo5.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.core.splashscreen.SplashScreen;

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

        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navController = Navigation.findNavController(
                this,
                R.id.nav_host_fragment_content_main
        );

        if (savedInstanceState == null && tokenManager.getToken() != null) {
            navController.navigate(R.id.FirstFragment);
        }

        binding.btnBack.setOnClickListener(v -> navController.navigateUp());

        binding.navDiscover.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() != R.id.FirstFragment) {
                navController.navigate(R.id.FirstFragment);
            }
        });

        binding.navSearch.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() != R.id.ExploreActivitiesFragment) {
                navController.navigate(R.id.ExploreActivitiesFragment);
            }
        });

        binding.btnMyReservations.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() != R.id.MyReservationsFragment) {
                navController.navigate(R.id.MyReservationsFragment);
            }
        });

        binding.navFavorites.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() != R.id.FavoritesFragment) {
                navController.navigate(R.id.FavoritesFragment);
            }
        });

        binding.navProfile.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() != R.id.ProfileFragment) {
                navController.navigate(R.id.ProfileFragment);
            }
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {

            boolean isAuthScreen =
                    destination.getId() == R.id.LoginFragment ||
                            destination.getId() == R.id.RegisterFragment ||
                            destination.getId() == R.id.WelcomeFragment;

            binding.header.setVisibility(isAuthScreen ? View.GONE : View.VISIBLE);
            binding.footer.setVisibility(isAuthScreen ? View.GONE : View.VISIBLE);

            if (destination.getId() == R.id.FirstFragment || isAuthScreen) {
                binding.btnBack.setVisibility(View.GONE);
            } else {
                binding.btnBack.setVisibility(View.VISIBLE);
            }

            if (destination.getId() == R.id.RatingFragment) {
                binding.txtHeaderTitle.setText("Calificar");
                clearFooterSelection();

            } else if (destination.getId() == R.id.FirstFragment) {
                binding.txtHeaderTitle.setText("Explora");
                updateFooterSelection(R.id.navDiscover);

            } else if (destination.getId() == R.id.ExploreActivitiesFragment) {
                binding.txtHeaderTitle.setText("Buscar");
                updateFooterSelection(R.id.navSearch);

            } else if (destination.getId() == R.id.MyReservationsFragment) {
                binding.txtHeaderTitle.setText("Mis Reservas");
                updateFooterSelection(R.id.btnMyReservations);

            } else if (destination.getId() == R.id.FavoritesFragment) {
                binding.txtHeaderTitle.setText("Favoritos");
                updateFooterSelection(R.id.navFavorites);

            } else if (destination.getId() == R.id.ProfileFragment) {
                binding.txtHeaderTitle.setText("Mi Perfil");
                updateFooterSelection(R.id.navProfile);

            } else {
                binding.txtHeaderTitle.setText("");
                clearFooterSelection();
            }
        });
    }

    private void updateFooterSelection(int selectedId) {
        int inactiveColor = ContextCompat.getColor(this, R.color.footer_inactive);
        int activeColor = ContextCompat.getColor(this, R.color.footer_active);

        binding.iconDiscover.setColorFilter(inactiveColor);
        binding.textDiscover.setTextColor(inactiveColor);

        binding.iconSearch.setColorFilter(inactiveColor);
        binding.textSearch.setTextColor(inactiveColor);

        binding.iconBookings.setColorFilter(inactiveColor);
        binding.textBookings.setTextColor(inactiveColor);

        binding.iconFavorites.setColorFilter(inactiveColor);
        binding.textFavorites.setTextColor(inactiveColor);

        binding.iconProfile.setColorFilter(inactiveColor);
        binding.textProfile.setTextColor(inactiveColor);

        if (selectedId == R.id.navDiscover) {
            binding.iconDiscover.setColorFilter(activeColor);
            binding.textDiscover.setTextColor(activeColor);

        } else if (selectedId == R.id.navSearch) {
            binding.iconSearch.setColorFilter(activeColor);
            binding.textSearch.setTextColor(activeColor);

        } else if (selectedId == R.id.btnMyReservations) {
            binding.iconBookings.setColorFilter(activeColor);
            binding.textBookings.setTextColor(activeColor);

        } else if (selectedId == R.id.navFavorites) {
            binding.iconFavorites.setColorFilter(activeColor);
            binding.textFavorites.setTextColor(activeColor);

        } else if (selectedId == R.id.navProfile) {
            binding.iconProfile.setColorFilter(activeColor);
            binding.textProfile.setTextColor(activeColor);
        }
    }

    private void clearFooterSelection() {
        int inactiveColor = ContextCompat.getColor(this, R.color.footer_inactive);

        binding.iconDiscover.setColorFilter(inactiveColor);
        binding.textDiscover.setTextColor(inactiveColor);

        binding.iconSearch.setColorFilter(inactiveColor);
        binding.textSearch.setTextColor(inactiveColor);

        binding.iconBookings.setColorFilter(inactiveColor);
        binding.textBookings.setTextColor(inactiveColor);

        binding.iconFavorites.setColorFilter(inactiveColor);
        binding.textFavorites.setTextColor(inactiveColor);

        binding.iconProfile.setColorFilter(inactiveColor);
        binding.textProfile.setTextColor(inactiveColor);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp()
                || super.onSupportNavigateUp();
    }
}