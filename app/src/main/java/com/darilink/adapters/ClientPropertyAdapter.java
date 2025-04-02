package com.darilink.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.darilink.R;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.FavoriteList;
import com.darilink.models.Offer;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClientPropertyAdapter extends RecyclerView.Adapter<ClientPropertyAdapter.PropertyViewHolder> {

    private final Context context;
    private final List<Offer> properties;
    private final PropertyListener listener;
    private final Firebase firebase;
    private final Firestore firestore;
    private List<String> favoritePropertyIds;

    public ClientPropertyAdapter(Context context, List<Offer> properties, PropertyListener listener) {
        this.context = context;
        this.properties = properties;
        this.listener = listener;
        this.firebase = Firebase.getInstance();
        this.firestore = Firestore.getInstance();
        this.favoritePropertyIds = new ArrayList<>();

        // Load favorite properties
        loadFavoriteProperties();
    }

    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_client_property, parent, false);
        return new PropertyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        Offer property = properties.get(position);

        // Set property title
        holder.propertyTitle.setText(property.getTitle());

        // Set property rent with currency formatting
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        currencyFormat.setCurrency(Currency.getInstance("USD"));
        String rentText = currencyFormat.format(property.getRent()) + "/month";
        holder.propertyRent.setText(rentText);

        // Set property location
        String locationText = property.getCity() + ", " + property.getCountry();
        holder.propertyLocation.setText(locationText);

        // Set property details
        holder.propertyBedrooms.setText(context.getString(R.string.bedrooms_count, property.getNumBedrooms()));
        holder.propertyBathrooms.setText(context.getString(R.string.bathrooms_count, property.getNumBathrooms()));
        holder.propertyArea.setText(context.getString(R.string.area_value, property.getArea()));

        // Load property image
        if (property.getImages() != null && !property.getImages().isEmpty()) {
            Glide.with(context)
                    .load(property.getImages().get(0))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.placeholder_property)
                    .error(R.drawable.placeholder_property)
                    .centerCrop()
                    .into(holder.propertyImage);
        } else {
            holder.propertyImage.setImageResource(R.drawable.placeholder_property);
        }

        // Set posted date
        String timeAgo = getTimeAgo(property.getCreatedAt());
        holder.postedTime.setText(timeAgo);

        // Check if property is in favorites
        boolean isFavorite = favoritePropertyIds.contains(property.getId());
        holder.favoriteIcon.setImageResource(isFavorite ?
                R.drawable.ic_favorite_filled : R.drawable.ic_favorite);

        // Set click listeners
        holder.propertyCard.setOnClickListener(v -> listener.onPropertyClick(property));

        holder.favoriteIcon.setOnClickListener(v -> {
            boolean newFavoriteState = !favoritePropertyIds.contains(property.getId());

            // Update UI immediately for better UX
            holder.favoriteIcon.setImageResource(newFavoriteState ?
                    R.drawable.ic_favorite_filled : R.drawable.ic_favorite);

            // Notify listener
            listener.onFavoriteClick(property, newFavoriteState);

            // Update local list
            if (newFavoriteState) {
                favoritePropertyIds.add(property.getId());
            } else {
                favoritePropertyIds.remove(property.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return properties.size();
    }

    static class PropertyViewHolder extends RecyclerView.ViewHolder {
        CardView propertyCard;
        ImageView propertyImage, favoriteIcon;
        TextView propertyTitle, propertyRent, propertyLocation;
        TextView propertyBedrooms, propertyBathrooms, propertyArea;
        TextView postedTime;

        PropertyViewHolder(@NonNull View itemView) {
            super(itemView);
            propertyCard = itemView.findViewById(R.id.propertyCard);
            propertyImage = itemView.findViewById(R.id.propertyImage);
            propertyTitle = itemView.findViewById(R.id.propertyTitle);
            propertyRent = itemView.findViewById(R.id.propertyRent);
            propertyLocation = itemView.findViewById(R.id.propertyLocation);
            propertyBedrooms = itemView.findViewById(R.id.propertyBedrooms);
            propertyBathrooms = itemView.findViewById(R.id.propertyBathrooms);
            propertyArea = itemView.findViewById(R.id.propertyArea);
            postedTime = itemView.findViewById(R.id.postedTime);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
        }
    }

    public interface PropertyListener {
        void onPropertyClick(Offer offer);
        void onFavoriteClick(Offer offer, boolean isFavorite);
    }

    private void loadFavoriteProperties() {
        FirebaseUser currentUser = firebase.getCurrentUser();
        if (currentUser == null) return;

        firestore.getDb().collection("favorites")
                .whereEqualTo("clientId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    favoritePropertyIds.clear();
                    for (FavoriteList favoriteList : queryDocumentSnapshots.toObjects(FavoriteList.class)) {
                        if (favoriteList.getOfferIds() != null) {
                            favoritePropertyIds.addAll(favoriteList.getOfferIds());
                        }
                    }
                    // Refresh the adapter to show favorites
                    notifyDataSetChanged();
                });
    }

    // Helper method to get relative time ago
    private String getTimeAgo(long timeInMillis) {
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - timeInMillis;

        // Convert to appropriate time unit
        long seconds = timeDifference / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;

        if (months > 0) {
            return months == 1 ? "1 month ago" : months + " months ago";
        } else if (weeks > 0) {
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        } else if (days > 0) {
            return days == 1 ? "1 day ago" : days + " days ago";
        } else if (hours > 0) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        } else if (minutes > 0) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        } else {
            return "Just now";
        }
    }

    // Method to update favorites status
    public void updateFavorites(List<String> favoriteIds) {
        this.favoritePropertyIds = favoriteIds;
        notifyDataSetChanged();
    }
}