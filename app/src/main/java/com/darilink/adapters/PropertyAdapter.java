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
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder> {

    private final Context context;
    private final List<Offer> properties;
    private final PropertyListener listener;

    public PropertyAdapter(Context context, List<Offer> properties, PropertyListener listener) {
        this.context = context;
        this.properties = properties;
        this.listener = listener;
    }
    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_property, parent, false);
        return new PropertyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        Offer property = properties.get(position);

        // Set property title
        holder.propertyTitle.setText(property.getTitle());

        // Set property rent
        String rentText = String.format(Locale.getDefault(), "$%.0f/month", property.getRent());
        holder.propertyRent.setText(rentText);

        // Set property location
        String locationText = property.getCity() + ", " + property.getCountry();
        holder.propertyLocation.setText(locationText);

        // Set property details
        holder.propertyBedrooms.setText(context.getString(R.string.bedrooms_count, property.getNumBedrooms()));
        holder.propertyBathrooms.setText(context.getString(R.string.bathrooms_count, property.getNumBathrooms()));
        holder.propertyArea.setText(context.getString(R.string.area_value, property.getArea()));

        // Set status badge
        if (property.isAvailable()) {
            holder.statusBadge.setText(R.string.available);
            holder.statusBadge.setBackgroundResource(R.drawable.badge_available);
        } else {
            holder.statusBadge.setText(R.string.rented);
            holder.statusBadge.setBackgroundResource(R.drawable.badge_rented);
        }

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

        // Set published date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        String publishedText = context.getString(R.string.posted_on, dateFormat.format(new Date(property.getCreatedAt())));
        holder.publishedDate.setText(publishedText);

        // Set click listeners
        holder.propertyCard.setOnClickListener(v -> listener.onPropertyClick(property));
        holder.editButton.setOnClickListener(v -> listener.onEditClick(property));
        holder.deleteButton.setOnClickListener(v -> listener.onDeleteClick(property));

        // TODO: Load requests count when implementation is ready
        holder.requestsCount.setText(context.getString(R.string.requests_count, 0));
    }

    @Override
    public int getItemCount() {
        return properties.size();
    }

    static class PropertyViewHolder extends RecyclerView.ViewHolder {
        CardView propertyCard;
        ImageView propertyImage;
        TextView statusBadge, propertyTitle, propertyRent, propertyLocation;
        TextView propertyBedrooms, propertyBathrooms, propertyArea;
        TextView requestsCount, publishedDate;
        MaterialButton editButton, deleteButton;

        PropertyViewHolder(@NonNull View itemView) {
            super(itemView);
            propertyCard = itemView.findViewById(R.id.propertyCard);
            propertyImage = itemView.findViewById(R.id.propertyImage);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            propertyTitle = itemView.findViewById(R.id.propertyTitle);
            propertyRent = itemView.findViewById(R.id.propertyRent);
            propertyLocation = itemView.findViewById(R.id.propertyLocation);
            propertyBedrooms = itemView.findViewById(R.id.propertyBedrooms);
            propertyBathrooms = itemView.findViewById(R.id.propertyBathrooms);
            propertyArea = itemView.findViewById(R.id.propertyArea);
            requestsCount = itemView.findViewById(R.id.requestsCount);
            publishedDate = itemView.findViewById(R.id.publishedDate);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    public interface PropertyListener {
        void onPropertyClick(Offer offer);
        void onEditClick(Offer offer);
        void onDeleteClick(Offer offer);
    }
}