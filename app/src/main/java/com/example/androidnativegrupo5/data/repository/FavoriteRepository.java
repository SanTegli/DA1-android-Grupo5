package com.example.androidnativegrupo5.data.repository;

import androidx.lifecycle.LiveData;

import com.example.androidnativegrupo5.data.local.db.FavoriteDao;
import com.example.androidnativegrupo5.data.local.db.FavoriteEntity;
import com.example.androidnativegrupo5.data.model.CheckFavoriteResponse;
import com.example.androidnativegrupo5.data.model.FavoriteResponse;
import com.example.androidnativegrupo5.data.network.ApiService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class FavoriteRepository {
    private final FavoriteDao favoriteDao;
    private final ApiService apiService;
    private final ExecutorService executorService;

    @Inject
    public FavoriteRepository(FavoriteDao favoriteDao, ApiService apiService) {
        this.favoriteDao = favoriteDao;
        this.apiService = apiService;
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<FavoriteEntity>> getAllFavorites() {
        return favoriteDao.getAllFavorites();
    }

    public void syncFavoritesWithApi() {
        apiService.getMyFavorites().enqueue(new Callback<List<FavoriteResponse>>() {
            @Override
            public void onResponse(Call<List<FavoriteResponse>> call, Response<List<FavoriteResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executorService.execute(() -> {
                        for (FavoriteResponse fav : response.body()) {
                            FavoriteEntity entity = convertToEntity(fav);
                            favoriteDao.insertFavorite(entity);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<FavoriteResponse>> call, Throwable t) {
                // Handle failure
            }
        });
    }

    public void toggleFavorite(long activityId, com.example.androidnativegrupo5.data.model.Activity activity, Callback<FavoriteResponse> callback) {
        executorService.execute(() -> {
            FavoriteEntity existing = favoriteDao.getFavoriteByActivityId(activityId);
            if (existing != null) {
                // Remove
                apiService.removeFromFavorites(activityId).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            executorService.execute(() -> favoriteDao.removeFavoriteByActivityId(activityId));
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (callback != null) callback.onFailure(null, t);
                    }
                });
            } else {
                // Add
                apiService.addToFavorites(activityId).enqueue(new Callback<FavoriteResponse>() {
                    @Override
                    public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            executorService.execute(() -> favoriteDao.insertFavorite(convertToEntity(response.body())));
                        }
                        if (callback != null) callback.onResponse(call, response);
                    }

                    @Override
                    public void onFailure(Call<FavoriteResponse> call, Throwable t) {
                        if (callback != null) callback.onFailure(call, t);
                    }
                });
            }
        });
    }

    public void checkFavoriteStatus(long activityId, Callback<CheckFavoriteResponse> callback) {
        apiService.checkFavoriteStatus(activityId).enqueue(callback);
    }

    public void clearChangeIndicators(long favoriteId, long activityId) {
        apiService.clearChangeIndicators(favoriteId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    executorService.execute(() -> {
                        favoriteDao.clearPriceChangeIndicator(activityId);
                        favoriteDao.clearAvailabilityChangeIndicator(activityId);
                    });
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private FavoriteEntity convertToEntity(FavoriteResponse fav) {
        FavoriteEntity entity = new FavoriteEntity();
        entity.setActivityId(fav.getId());
        entity.setActivityName(fav.getName());
        entity.setDescription(fav.getDescription());
        entity.setDestination(fav.getDestination());
        entity.setCategory(fav.getCategory());
        entity.setDuration(fav.getDuration());
        entity.setPrice(fav.getPrice());
        entity.setAvailableSlots(fav.getAvailableSlots());
        entity.setImageUrl(fav.getImageUrl());
        entity.setPriceWhenMarked(fav.getPriceWhenMarked());
        entity.setAvailableSlotsWhenMarked(fav.getAvailableSlotsWhenMarked());
        entity.setHasPriceChanged(fav.getHasPriceChanged());
        entity.setHasAvailabilityChanged(fav.getHasAvailabilityChanged());
        return entity;
    }
}
