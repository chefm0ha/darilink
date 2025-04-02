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
import com.darilink.models.Offer;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    private final Context context;
    private final List<Offer> favorites;
    private final FavoriteListener listener;

    public FavoritesAdapter(Context context, List<Offer> favorites, FavoriteListener listener) {
        this.context = context;
        this.favorites = favorites;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Offer offer = favorites.get(position);

        // Set property title
        holder.propertyTitle.setText(offer.getTitle());

        // Set property type and location
        String typeAndLocation = String.format("%s • %s, %s",
                offer.getPropertyType(), offer.getCity(), offer.getCountry());
        holder.propertyTypeLocation.setText(typeAndLocation);

        // Set property details
        String details = String.format(Locale.getDefault(), "%d bed • %d bath • %.1f m²",
                offer.getNumBedrooms(), offer.getNumBathrooms(), offer.getArea());
        holder.propertyDetails.setText(details);

        // Format rent with currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        currencyFormat.setCurrency(Currency.getInstance("USD"));
        String rentText = currencyFormat.format(offer.getRent()) + "/month";
        holder.propertyRent.setText(rentText);

        // Load property image
        if (offer.getImages() != null && !offer.getImages().isEmpty()) {
            Glide.with(context)
                    .load(offer.getImages().get(0))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.placeholder_property)
                    .error(R.drawable.placeholder_property)
                    .centerCrop()
                    .into(holder.propertyImage);
        } else {
            holder.propertyImage.setImageResource(R.drawable.placeholder_property);
        }

        // Set available badge
        holder.availabilityBadge.setVisibility(offer.isAvailable() ? View.VISIBLE : View.GONE);

        // Set click listeners
        holder.propertyCard.setOnClickListener(v -> listener.onFavoriteClick(offer));
        holder.removeButton.setOnClickListener(v -> listener.onRemoveFavorite(offer));
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        CardView propertyCard;
        ImageView propertyImage, removeButton;
        TextView propertyTitle, propertyTypeLocation, propertyDetails, propertyRent;
        TextView availabilityBadge;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            propertyCard = itemView.findViewById(R.id.propertyCard);
            propertyImage = itemView.findViewById(R.id.propertyImage);
            propertyTitle = itemView.findViewById(R.id.propertyTitle);
            propertyTypeLocation = itemView.findViewById(R.id.propertyTypeLocation);
            propertyDetails = itemView.findViewById(R.id.propertyDetails);
            propertyRent = itemView.findViewById(R.id.propertyRent);
            availabilityBadge = itemView.findViewById(R.id.availabilityBadge);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }

    public interface FavoriteListener {
        void onFavoriteClick(Offer offer);
        void onRemoveFavorite(Offer offer);
    }
}