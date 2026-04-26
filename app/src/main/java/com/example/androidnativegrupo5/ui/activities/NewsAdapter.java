package com.example.androidnativegrupo5.ui.activities;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.data.model.NewsItem;
import com.example.androidnativegrupo5.databinding.ItemNewsBinding;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<NewsItem> newsList = new ArrayList<>();
    private final OnNewsClickListener listener;

    public interface OnNewsClickListener {
        void onNewsClick(NewsItem news);
    }

    public NewsAdapter(OnNewsClickListener listener) {
        this.listener = listener;
    }

    public void setNewsList(List<NewsItem> newsList) {
        this.newsList = newsList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNewsBinding binding = ItemNewsBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NewsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        holder.bind(newsList.get(position));
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    class NewsViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewsBinding binding;

        public NewsViewHolder(ItemNewsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(NewsItem news) {
            binding.textNewsTitle.setText(news.getTitle());
            binding.textNewsDescription.setText(news.getDescription());

            Glide.with(binding.imageNews.getContext())
                    .load(news.getImageUrl())
                    .into(binding.imageNews);

            binding.getRoot().setOnClickListener(v -> listener.onNewsClick(news));
        }
    }
}
