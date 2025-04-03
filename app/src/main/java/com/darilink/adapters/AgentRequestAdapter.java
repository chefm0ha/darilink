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
import com.darilink.models.Request;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AgentRequestAdapter extends RecyclerView.Adapter<AgentRequestAdapter.AgentRequestViewHolder> {

    private final Context context;
    private final List<Request> requests;
    private final Map<String, Offer> offersMap;
    private final AgentRequestListener listener;

    public AgentRequestAdapter(Context context, List<Request> requests,
                               Map<String, Offer> offersMap, AgentRequestListener listener) {
        this.context = context;
        this.requests = requests;
        this.offersMap = offersMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AgentRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_agent_request, parent, false);
        return new AgentRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AgentRequestViewHolder holder, int position) {
        Request request = requests.get(position);
        Offer offer = offersMap.get(request.getOfferId());

        // Set request date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        holder.requestDate.setText(dateFormat.format(new Date(request.getCreatedAt())));

        // Set status badge
        holder.statusBadge.setText(getStatusDisplayText(request.getStatus()));
        holder.statusBadge.setBackgroundResource(getStatusBadgeBackground(request.getStatus()));

        // Load property details
        if (offer != null) {
            // Set property title
            holder.propertyTitle.setText(offer.getTitle());
            holder.propertyLocation.setText(String.format("%s, %s", offer.getCity(), offer.getCountry()));

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

            // Show property container
            holder.propertyContainer.setVisibility(View.VISIBLE);
            holder.propertyNotFound.setVisibility(View.GONE);
        } else {
            // Property not found
            holder.propertyContainer.setVisibility(View.GONE);
            holder.propertyNotFound.setVisibility(View.VISIBLE);
        }

        // Format rent proposal with currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        currencyFormat.setCurrency(java.util.Currency.getInstance("USD"));
        String rentText = currencyFormat.format(request.getRentProposal()) + "/month";
        holder.rentProposal.setText(rentText);

        // Set click listeners
        holder.itemCard.setOnClickListener(v -> {
            if (offer != null) {
                listener.onRequestDetailClick(request, offer);
            }
        });

        holder.propertyCard.setOnClickListener(v -> {
            if (offer != null) {
                listener.onPropertyClick(offer);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class AgentRequestViewHolder extends RecyclerView.ViewHolder {
        CardView itemCard, propertyCard;
        TextView requestDate, statusBadge, rentProposal;
        TextView propertyTitle, propertyLocation, propertyNotFound;
        ImageView propertyImage;
        View propertyContainer;

        AgentRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            itemCard = itemView.findViewById(R.id.itemCard);
            requestDate = itemView.findViewById(R.id.requestDate);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            rentProposal = itemView.findViewById(R.id.rentProposal);
            propertyCard = itemView.findViewById(R.id.propertyCard);
            propertyTitle = itemView.findViewById(R.id.propertyTitle);
            propertyLocation = itemView.findViewById(R.id.propertyLocation);
            propertyImage = itemView.findViewById(R.id.propertyImage);
            propertyNotFound = itemView.findViewById(R.id.propertyNotFound);
            propertyContainer = itemView.findViewById(R.id.propertyContainer);
        }
    }

    private String getStatusDisplayText(String status) {
        switch (status.toLowerCase()) {
            case "pending":
                return context.getString(R.string.pending);
            case "accepted":
                return context.getString(R.string.accepted);
            case "rejected":
                return context.getString(R.string.rejected);
            default:
                return status;
        }
    }

    private int getStatusBadgeBackground(String status) {
        switch (status.toLowerCase()) {
            case "pending":
                return R.drawable.badge_pending;
            case "accepted":
                return R.drawable.badge_accepted;
            case "rejected":
                return R.drawable.badge_rejected;
            default:
                return R.drawable.badge_pending;
        }
    }

    public interface AgentRequestListener {
        void onRequestDetailClick(Request request, Offer offer);
        void onPropertyClick(Offer offer);
    }
}