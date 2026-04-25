package com.example.androidnativegrupo5.ui.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.androidnativegrupo5.R;
import com.example.androidnativegrupo5.data.model.Rating;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Rating> comments = new ArrayList<>();

    public void setComments(List<Rating> newComments) {
        comments.clear();
        if (newComments != null) {
            comments.addAll(newComments);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(comments.get(position));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView imageProfile;
        private final TextView textUsername;
        private final TextView textDate;
        private final TextView textCommentBody;
        private final RatingBar ratingBar;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProfile = itemView.findViewById(R.id.image_user_profile);
            textUsername = itemView.findViewById(R.id.text_username);
            textDate = itemView.findViewById(R.id.text_date);
            textCommentBody = itemView.findViewById(R.id.text_comment_body);
            ratingBar = itemView.findViewById(R.id.comment_rating_bar);
        }

        public void bind(Rating rating) {
            textUsername.setText(rating.getUsername() != null ? rating.getUsername() : "Anónimo");
            textDate.setText(rating.getCreatedAt() != null ? rating.getCreatedAt() : "");
            textCommentBody.setText(rating.getComment() != null ? rating.getComment() : "");
            
            if (rating.getActivityScore() != null) {
                ratingBar.setVisibility(View.VISIBLE);
                ratingBar.setRating(rating.getActivityScore().floatValue());
            } else {
                ratingBar.setVisibility(View.GONE);
            }

            Glide.with(itemView.getContext())
                    .load(rating.getUserProfileImageUrl())
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_report_image)
                    .circleCrop()
                    .into(imageProfile);
        }
    }
}
