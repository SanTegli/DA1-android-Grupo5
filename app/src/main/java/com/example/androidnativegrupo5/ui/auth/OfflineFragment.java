package com.example.androidnativegrupo5.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.local.TokenManager;
import com.example.androidnativegrupo5.databinding.FragmentOfflineBinding;
import com.example.androidnativegrupo5.utils.NetworkUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class OfflineFragment extends Fragment {

    @Inject TokenManager tokenManager;
    private FragmentOfflineBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOfflineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnViewLocalReservations.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.action_OfflineFragment_to_MyReservationsFragment)
        );

        binding.btnRetryConnection.setOnClickListener(v -> {
            if (NetworkUtils.isOnline(requireContext())) {
                if (tokenManager.getToken() != null) {
                    // Si está logueado y hay red, vamos al Home limpiando el stack
                    NavHostFragment.findNavController(this).navigate(
                        R.id.FirstFragment, 
                        null, 
                        new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                    );
                } else {
                    NavHostFragment.findNavController(this).popBackStack();
                }
            } else {
                Toast.makeText(requireContext(), "Sigues sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
