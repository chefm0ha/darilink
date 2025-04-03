package com.darilink.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.darilink.R;
import com.darilink.models.Evaluation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final Context context;
    private final List<Evaluation> reviews;
    private final Map<String, String> usernames;

    public ReviewAdapter(Context context, List<Evaluation> reviews, Map<String, String> usernames) {
        this.context = context;
        this.reviews = reviews;
        this.usernames = usernames;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Evaluation review = reviews.get(position);

        // Set reviewer name
        String reviewerName = usernames.getOrDefault(review.getClientId(), "Anonymous User");
        holder.reviewerName.setText(reviewerName);

        // Set review date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        holder.reviewDate.setText(dateFormat.format(new Date(review.getDate())));

        // Set rating
        holder.reviewRating.setRating(review.getRating());

        // Set review content
        holder.reviewContent.setText(review.getComment());

        // We could load profile image here if available
        // For now, use default avatar
        holder.reviewerAvatar.setImageResource(R.drawable.default_profile);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        CircleImageView reviewerAvatar;
        TextView reviewerName, reviewDate, reviewContent;
        RatingBar reviewRating;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewerAvatar = itemView.findViewById(R.id.reviewerAvatar);
            reviewerName = itemView.findViewById(R.id.reviewerName);
            reviewDate = itemView.findViewById(R.id.reviewDate);
            reviewContent = itemView.findViewById(R.id.reviewContent);
            reviewRating = itemView.findViewById(R.id.reviewRating);
        }
    }
}