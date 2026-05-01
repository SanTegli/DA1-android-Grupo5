package com.example.androidnativegrupo5.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.core.splashscreen.SplashScreen;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.data.local.db.Reserva;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;
import com.example.androidnativegrupo5.data.model.RescheduleReservationRequest;
import com.example.androidnativegrupo5.data.model.ReservationResponse;
import com.example.androidnativegrupo5.data.network.ApiService;
import com.example.androidnativegrupo5.databinding.ActivityMainBinding;
import com.example.androidnativegrupo5.utils.NetworkMonitor;
import com.example.androidnativegrupo5.utils.NetworkUtils;
import com.example.androidnativegrupo5.utils.SyncManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Response;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Inject TokenManager tokenManager;
    @Inject ReservaDao reservaDao;
    @Inject ApiService apiService;

    private NetworkMonitor networkMonitor = new NetworkMonitor();
    private ActivityMainBinding binding;
    private NavController navController;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate: Inicializando actividad principal");

        networkMonitor.start(this, () -> {
            Log.i(TAG, "Conexión restaurada, iniciando sincronización");
            runOnUiThread(this::syncOfflineChanges);
        });

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        setupNavigationHandlers();
        setupDestinationListener();
    }

    private void setupNavigationHandlers() {
        binding.btnBack.setOnClickListener(v -> navController.navigateUp());

        binding.navDiscover.setOnClickListener(v -> navigateTo(R.id.FirstFragment));
        binding.navSearch.setOnClickListener(v -> navigateTo(R.id.ExploreActivitiesFragment));
        binding.btnMyReservations.setOnClickListener(v -> navigateTo(R.id.MyReservationsFragment));
        binding.navProfile.setOnClickListener(v -> navigateTo(R.id.ProfileFragment));
    }

    private void navigateTo(int destinationId) {
        if (navController.getCurrentDestination() != null &&
                navController.getCurrentDestination().getId() != destinationId) {
            Log.d(TAG, "Navegando a: " + destinationId);
            navController.navigate(destinationId);
        }
    }

    private void setupDestinationListener() {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            Log.d(TAG, "Destino actual: " + destination.getLabel());

            boolean isAuthScreen =
                    destination.getId() == R.id.LoginFragment ||
                    destination.getId() == R.id.RegisterFragment ||
                    destination.getId() == R.id.WelcomeFragment ||
                    destination.getId() == R.id.OfflineFragment;

            binding.header.setVisibility(isAuthScreen ? View.GONE : View.VISIBLE);
            binding.footer.setVisibility(isAuthScreen ? View.GONE : View.VISIBLE);

            if (destination.getId() == R.id.FirstFragment || isAuthScreen) {
                binding.btnBack.setVisibility(View.GONE);
            } else {
                binding.btnBack.setVisibility(View.VISIBLE);
            }

            updateUIForDestination(destination.getId());
        });
    }

    private void updateUIForDestination(int destinationId) {
        if (destinationId == R.id.RatingFragment) {
            binding.txtHeaderTitle.setText("Calificar");
            clearFooterSelection();
        } else if (destinationId == R.id.FirstFragment) {
            binding.txtHeaderTitle.setText("Explora");
            updateFooterSelection(R.id.navDiscover);
        } else if (destinationId == R.id.ExploreActivitiesFragment) {
            binding.txtHeaderTitle.setText("Buscar");
            updateFooterSelection(R.id.navSearch);
        } else if (destinationId == R.id.MyReservationsFragment) {
            binding.txtHeaderTitle.setText("Mis Reservas");
            updateFooterSelection(R.id.btnMyReservations);
        } else if (destinationId == R.id.ProfileFragment) {
            binding.txtHeaderTitle.setText("Mi Perfil");
            updateFooterSelection(R.id.navProfile);
        } else {
            binding.txtHeaderTitle.setText("");
            clearFooterSelection();
        }
    }

    private void syncOfflineChanges() {
        if (!NetworkUtils.isOnline(this)) return;

        executor.execute(() -> {
            syncReschedules();
            syncCancellations();
        });

        SyncManager.needsRefresh = true;
    }

    private void syncReschedules() {
        List<Reserva> pendingSync = reservaDao.getPendingSync();
        for (Reserva r : pendingSync) {
            RescheduleReservationRequest req = new RescheduleReservationRequest(
                    r.getNewDate(), r.getNewTime(), r.getNewParticipants()
            );
            try {
                Response<ReservationResponse> response = apiService.rescheduleReservation(r.getId(), req).execute();
                if (response.isSuccessful() && response.body() != null) {
                    Reserva updated = Reserva.fromResponse(response.body());
                    updated.setPendingSync(false);
                    reservaDao.update(updated);
                    Log.d(TAG, "Reserva " + r.getId() + " sincronizada.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sincronizando reserva " + r.getId(), e);
            }
        }
    }

    private void syncCancellations() {
        List<Reserva> cancellations = reservaDao.getPendingCancellations();
        for (Reserva r : cancellations) {
            try {
                Response<Void> response = apiService.cancelReservation(r.getId()).execute();
                if (response.isSuccessful()) {
                    reservaDao.clearPendingCancellation(r.getId());
                    Log.d(TAG, "Cancelación " + r.getId() + " sincronizada.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sincronizando cancelación " + r.getId(), e);
            }
        }
    }

    private void updateFooterSelection(int selectedId) {
        int inactiveColor = ContextCompat.getColor(this, R.color.footer_inactive);
        int activeColor = ContextCompat.getColor(this, R.color.footer_active);

        resetFooterColors(inactiveColor);

        if (selectedId == R.id.navDiscover) {
            binding.iconDiscover.setColorFilter(activeColor);
            binding.textDiscover.setTextColor(activeColor);
        } else if (selectedId == R.id.navSearch) {
            binding.iconSearch.setColorFilter(activeColor);
            binding.textSearch.setTextColor(activeColor);
        } else if (selectedId == R.id.btnMyReservations) {
            binding.iconBookings.setColorFilter(activeColor);
            binding.textBookings.setTextColor(activeColor);
        } else if (selectedId == R.id.navProfile) {
            binding.iconProfile.setColorFilter(activeColor);
            binding.textProfile.setTextColor(activeColor);
        }
    }

    private void clearFooterSelection() {
        resetFooterColors(ContextCompat.getColor(this, R.color.footer_inactive));
    }

    private void resetFooterColors(int color) {
        binding.iconDiscover.setColorFilter(color);
        binding.textDiscover.setTextColor(color);
        binding.iconSearch.setColorFilter(color);
        binding.textSearch.setTextColor(color);
        binding.iconBookings.setColorFilter(color);
        binding.textBookings.setTextColor(color);
        binding.iconProfile.setColorFilter(color);
        binding.textProfile.setTextColor(color);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && navController.navigateUp()) || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkMonitor.stop();
    }
}
