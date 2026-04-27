package com.example.androidnativegrupo5.ui.favorites;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.androidnativegrupo5.data.local.db.FavoriteEntity;
import com.example.androidnativegrupo5.data.model.Activity;
import com.example.androidnativegrupo5.data.model.CheckFavoriteResponse;
import com.example.androidnativegrupo5.data.model.FavoriteResponse;
import com.example.androidnativegrupo5.data.repository.FavoriteRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class FavoriteViewModel extends ViewModel {
    private final FavoriteRepository repository;
    private final MutableLiveData<Boolean> isFavorite = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    @Inject
    public FavoriteViewModel(FavoriteRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<FavoriteEntity>> getFavorites() {
        return repository.getAllFavorites();
    }

    public LiveData<Boolean> getIsFavorite() {
        return isFavorite;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void checkFavoriteStatus(long activityId) {
        repository.checkFavoriteStatus(activityId, new Callback<CheckFavoriteResponse>() {
            @Override
            public void onResponse(Call<CheckFavoriteResponse> call, Response<CheckFavoriteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isFavorite.setValue(response.body().getIsFavorite());
                }
            }

            @Override
            public void onFailure(Call<CheckFavoriteResponse> call, Throwable t) {
                errorMessage.setValue(t.getMessage());
            }
        });
    }

    public void toggleFavorite(long activityId, Activity activity) {
        repository.toggleFavorite(activityId, activity, new Callback<FavoriteResponse>() {
            @Override
            public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                if (response.isSuccessful()) {
                    isFavorite.setValue(response.body() != null);
                } else if (response.code() == 204 || response.code() == 200) {
                     // If it was a DELETE, it might return 204
                     // Need to be careful here depending on how toggleFavorite in Repo is implemented
                     // Repository handles the local DB, we just update UI state.
                }
            }

            @Override
            public void onFailure(Call<FavoriteResponse> call, Throwable t) {
                errorMessage.setValue(t.getMessage());
            }
        });
    }

    public void refreshFavorites() {
        repository.syncFavoritesWithApi();
    }

    public void clearIndicators(long favoriteId, long activityId) {
        repository.clearChangeIndicators(favoriteId, activityId);
    }
}
