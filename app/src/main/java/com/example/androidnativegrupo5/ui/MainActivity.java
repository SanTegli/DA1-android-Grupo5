package com.example.androidnativegrupo5.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.view.View;
import android.content.res.Configuration;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;
import com.example.androidnativegrupo5.databinding.ActivityMainBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject
    TokenManager tokenManager;

    @Inject
    ReservaDao reservaDao;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ActivityMainBinding binding;
    private boolean darkModeEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        if (savedInstanceState == null) {
            if (tokenManager.getToken() != null) {
                navController.navigate(R.id.MyReservationsFragment);
            }
        }

        binding.btnBack.setOnClickListener(v -> {
            NavController controller = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            controller.navigateUp();
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {

            boolean hideBars =
                    destination.getId() == R.id.LoginFragment ||
                            destination.getId() == R.id.RegisterFragment;

            // 🔹 Header y Footer
            if (hideBars) {
                binding.header.setVisibility(View.GONE);
                binding.footer.setVisibility(View.GONE);
            } else {
                binding.header.setVisibility(View.VISIBLE);
                binding.footer.setVisibility(View.VISIBLE);
            }

            // 🔹 Flecha back
            if (destination.getId() == R.id.FirstFragment || hideBars) {
                binding.btnBack.setVisibility(View.GONE);
            } else {
                binding.btnBack.setVisibility(View.VISIBLE);
            }
        });

        binding.btnTheme.setOnClickListener(v -> {
            int nightMode = getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;

            if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });

        binding.navDiscover.setOnClickListener(v ->
                navController.navigate(R.id.FirstFragment)
        );

        binding.btnMyReservations.setOnClickListener(v ->
                navController.navigate(R.id.MyReservationsFragment)
        );

        binding.navProfile.setOnClickListener(v ->
                navController.navigate(R.id.ProfileFragment)
        );
    }
}