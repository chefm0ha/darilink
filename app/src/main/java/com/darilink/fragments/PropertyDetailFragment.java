package com.darilink.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.darilink.R;
import com.darilink.adapters.ImageSliderAdapter;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.FavoriteList;
import com.darilink.models.Offer;
import com.darilink.dataAccess.ChatService;
import com.darilink.utils.PropertyInteractionTracker;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PropertyDetailFragment extends Fragment {

    private static final String ARG_PROPERTY_ID = "property_id";

    private String propertyId;
    private Offer property;
    private boolean isFavorite = false;

    // UI components
    private ViewPager2 imageSlider;
    private TextView propertyTitle, propertyRent, propertyLocation, propertyDescription;
    private TextView propertyBedrooms, propertyBathrooms, propertyArea, propertyFloor;
    private TextView propertyType, agentInfo, postedDate;
    private ChipGroup amenitiesChipGroup;
    private Button makeRequestButton, contactAgentButton, addToFavoritesButton, viewReviewsButton;
    private LinearLayout progressLayout;
    private NestedScrollView contentLayout;
    private ImageView backButton, favoriteIcon;

    private Firestore firestore;
    private Firebase firebase;
    private FirebaseUser currentUser;
    private PropertyInteractionTracker interactionTracker;

    public static PropertyDetailFragment newInstance(String propertyId) {
        PropertyDetailFragment fragment = new PropertyDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROPERTY_ID, propertyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            propertyId = getArguments().getString(ARG_PROPERTY_ID);
        }

        // Initialize Firebase
        firestore = Firestore.getInstance();
        firebase = Firebase.getInstance();
        currentUser = firebase.getCurrentUser();

        // Initialize interaction tracker
        interactionTracker = new PropertyInteractionTracker(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_property_detail, container, false);

        // Hide the action bar for immersive experience
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().hide();
            }
        }

        initViews(view);
        setupListeners();
        loadPropertyDetails();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Show the action bar again when leaving
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().show();
            }
        }
    }

    private void initViews(View view) {
        // Main components
        imageSlider = view.findViewById(R.id.imageSlider);
        propertyTitle = view.findViewById(R.id.propertyTitle);
        propertyRent = view.findViewById(R.id.propertyRent);
        propertyLocation = view.findViewById(R.id.propertyLocation);
        propertyDescription = view.findViewById(R.id.propertyDescription);
        propertyBedrooms = view.findViewById(R.id.propertyBedrooms);
        propertyBathrooms = view.findViewById(R.id.propertyBathrooms);
        propertyArea = view.findViewById(R.id.propertyArea);
        propertyFloor = view.findViewById(R.id.propertyFloor);
        propertyType = view.findViewById(R.id.propertyType);
        agentInfo = view.findViewById(R.id.agentInfo);
        postedDate = view.findViewById(R.id.postedDate);
        amenitiesChipGroup = view.findViewById(R.id.amenitiesChipGroup);

        // Buttons
        makeRequestButton = view.findViewById(R.id.makeRequestButton);
        contactAgentButton = view.findViewById(R.id.contactAgentButton);
        addToFavoritesButton = view.findViewById(R.id.addToFavoritesButton);
        viewReviewsButton = view.findViewById(R.id.viewReviewsButton);
        backButton = view.findViewById(R.id.backButton);
        favoriteIcon = view.findViewById(R.id.favoriteIcon);

        // Layouts
        progressLayout = view.findViewById(R.id.progressLayout);
        contentLayout = view.findViewById(R.id.contentLayout);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        favoriteIcon.setOnClickListener(v -> toggleFavorite());

        addToFavoritesButton.setOnClickListener(v -> toggleFavorite());

        makeRequestButton.setOnClickListener(v -> {
            if (property != null) {
                // Track interaction
                if (propertyId != null) {
                    interactionTracker.recordPropertyRequest(propertyId);
                }

                showMakeRequestDialog();
            }
        });

        contactAgentButton.setOnClickListener(v -> {
            Log.d("PropertyDetailFragment", "Contact agent button clicked");
            if (property != null) {
                // Track interaction
                if (propertyId != null) {
                    interactionTracker.recordAgentContact(propertyId);
                }

                // Show options dialog for contact methods
                BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_contact_agent, null);
                dialog.setContentView(dialogView);

                TextView agentNameText = dialogView.findViewById(R.id.agentNameText);
                Button callButton = dialogView.findViewById(R.id.callButton);
                Button chatButton = dialogView.findViewById(R.id.chatButton); // This is important - make sure this ID exists in your XML
                Button cancelButton = dialogView.findViewById(R.id.cancelButton);

                // Debug to make sure all buttons are found
                Log.d("PropertyDetailFragment", "Call button null? " + (callButton == null));
                Log.d("PropertyDetailFragment", "Chat button null? " + (chatButton == null));

                // Load agent details
                firestore.getDb().collection("agents").document(property.getAgentId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String firstName = documentSnapshot.getString("firstName");
                                String lastName = documentSnapshot.getString("lastName");
                                String phone = documentSnapshot.getString("phone");
                                String email = documentSnapshot.getString("email");

                                // Set agent name
                                if (firstName != null && lastName != null) {
                                    agentNameText.setText(String.format("Contact %s %s", firstName, lastName));
                                }

                                // Setup call button
                                callButton.setOnClickListener(view -> {
                                    if (phone != null && !phone.isEmpty()) {
                                        Intent intent = new Intent(Intent.ACTION_DIAL);
                                        intent.setData(Uri.parse("tel:" + phone));
                                        startActivity(intent);
                                        dialog.dismiss();
                                    } else {
                                        Toast.makeText(getContext(), "Phone number not available",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });


                                // Setup chat button with debug logs
                                if (chatButton != null) {
                                    chatButton.setOnClickListener(view -> {
                                        Log.d("PropertyDetailFragment", "Chat button clicked");
                                        dialog.dismiss();
                                        startChat();
                                    });
                                } else {
                                    Log.e("PropertyDetailFragment", "Chat button not found in layout");
                                }
                            }
                        });

                // Cancel button
                cancelButton.setOnClickListener(view -> dialog.dismiss());

                dialog.show();
            }
        });

        viewReviewsButton.setOnClickListener(v -> {
            if (property != null) {
                // Navigate to property reviews
                if (getActivity() != null) {
                    PropertyReviewsFragment fragment = PropertyReviewsFragment.newInstance(property.getId());
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });
    }

    private void loadPropertyDetails() {
        showLoading(true);

        if (propertyId == null || propertyId.isEmpty()) {
            Toast.makeText(getContext(), "Property ID not found", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        // Load property details
        firestore.getDb().collection("Offer").document(propertyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    property = documentSnapshot.toObject(Offer.class);
                    if (property != null) {
                        property.setId(documentSnapshot.getId());
                        displayPropertyDetails();
                        checkIfFavorite();

                        // Track property view
                        interactionTracker.recordPropertyView(propertyId);

                        // Check if we should prompt for review
                        if (interactionTracker.shouldPromptForReview(propertyId)) {
                            // Check if user already has a review
                            checkExistingReview();
                        }
                    } else {
                        Toast.makeText(getContext(), "Property not found", Toast.LENGTH_SHORT).show();
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void displayPropertyDetails() {
        // Display basic property info
        propertyTitle.setText(property.getTitle());

        // Format rent with currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        currencyFormat.setCurrency(Currency.getInstance("USD"));
        propertyRent.setText(String.format("%s/month", currencyFormat.format(property.getRent())));

        // Location
        propertyLocation.setText(String.format("%s, %s", property.getCity(), property.getCountry()));

        // Description
        propertyDescription.setText(property.getDescription());

        // Property details
        propertyBedrooms.setText(String.valueOf(property.getNumBedrooms()));
        propertyBathrooms.setText(String.valueOf(property.getNumBathrooms()));
        propertyArea.setText(String.format(Locale.getDefault(), "%.1f m²", property.getArea()));
        propertyFloor.setText(String.valueOf(property.getFloorNumber()));
        propertyType.setText(property.getPropertyType());

        // Posted date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        postedDate.setText(String.format("Posted on %s", dateFormat.format(new Date(property.getCreatedAt()))));

        // Load agent info
        loadAgentInfo(property.getAgentId());

        // Setup images slider
        setupImageSlider();

        // Setup amenities
        setupAmenities();
    }

    private void setupImageSlider() {
        if (property.getImages() != null && !property.getImages().isEmpty()) {
            ImageSliderAdapter adapter = new ImageSliderAdapter(requireContext(), property.getImages());
            imageSlider.setAdapter(adapter);

            // Add page indicator dots if needed
            // For simplicity, we're skipping that here
        } else {
            // Add default image if no images available
            List<String> defaultImage = new ArrayList<>();
            defaultImage.add("drawable://" + R.drawable.placeholder_property);
            ImageSliderAdapter adapter = new ImageSliderAdapter(requireContext(), defaultImage);
            imageSlider.setAdapter(adapter);
        }
    }

    private void setupAmenities() {
        amenitiesChipGroup.removeAllViews();

        if (property.getAmenities() != null && !property.getAmenities().isEmpty()) {
            for (String amenity : property.getAmenities()) {
                Chip chip = new Chip(requireContext());
                chip.setText(amenity);
                chip.setCheckable(false);
                chip.setClickable(false);
                amenitiesChipGroup.addView(chip);
            }
        } else {
            // Add a placeholder chip if no amenities
            Chip chip = new Chip(requireContext());
            chip.setText(R.string.no_amenities_listed);
            chip.setCheckable(false);
            chip.setClickable(false);
            amenitiesChipGroup.addView(chip);
        }
    }

    private void loadAgentInfo(String agentId) {
        if (agentId == null || agentId.isEmpty()) {
            agentInfo.setText(R.string.agent_info_not_available);
            return;
        }

        firestore.getDb().collection("agents").document(agentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String agencyName = documentSnapshot.getString("agencyName");

                        if (firstName != null && lastName != null) {
                            if (agencyName != null && !agencyName.isEmpty()) {
                                agentInfo.setText(String.format("%s %s (%s)", firstName, lastName, agencyName));
                            } else {
                                agentInfo.setText(String.format("%s %s", firstName, lastName));
                            }
                        } else {
                            agentInfo.setText(R.string.agent_info_not_available);
                        }
                    } else {
                        agentInfo.setText(R.string.agent_info_not_available);
                    }
                })
                .addOnFailureListener(e -> agentInfo.setText(R.string.agent_info_not_available));
    }

    private void showLoading(boolean isLoading) {
        progressLayout.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void checkExistingReview() {
        if (currentUser == null) return;

        firestore.getDb().collection("evaluations")
                .whereEqualTo("clientId", currentUser.getUid())
                .whereEqualTo("offerId", propertyId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No existing review, prompt user to leave one
                        promptForReview();
                    }
                });
    }

    private void promptForReview() {
        if (getContext() == null) return;

        // Wait a bit to show the prompt (let the user explore the property details first)
        contentLayout.postDelayed(() -> {
            if (isAdded() && getContext() != null) {
                new android.app.AlertDialog.Builder(getContext())
                        .setTitle(R.string.rate_this_property)
                        .setMessage(R.string.would_you_like_to_review)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            // Show review dialog
                            PropertyReviewFragment reviewDialog = PropertyReviewFragment.newInstance(propertyId);
                            reviewDialog.show(getParentFragmentManager(), "PropertyReviewDialog");
                        })
                        .setNegativeButton(R.string.maybe_later, null)
                        .show();
            }
        }, 3000); // 3 seconds delay
    }

    private void checkIfFavorite() {
        if (currentUser == null) return;

        firestore.getDb().collection("favorites")
                .whereEqualTo("clientId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (FavoriteList favoriteList : queryDocumentSnapshots.toObjects(FavoriteList.class)) {
                        if (favoriteList.getOfferIds() != null &&
                                favoriteList.getOfferIds().contains(property.getId())) {
                            isFavorite = true;
                            updateFavoriteUI();
                            return;
                        }
                    }

                    // Not found in any favorite list
                    isFavorite = false;
                    updateFavoriteUI();
                });
    }

    private void updateFavoriteUI() {
        favoriteIcon.setImageResource(isFavorite ?
                R.drawable.ic_favorite_filled : R.drawable.ic_favorite);

        addToFavoritesButton.setText(isFavorite ?
                R.string.remove_from_favorites : R.string.add_to_favorites);
    }

    private void toggleFavorite() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to add favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        if (property == null) return;

        // Toggle favorite state for immediate UI feedback
        isFavorite = !isFavorite;
        updateFavoriteUI();

        // Find user's default favorite list or create one
        firestore.getDb().collection("favorites")
                .whereEqualTo("clientId", currentUser.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Create new favorite list for user
                        createNewFavoriteList();
                    } else {
                        // Update existing favorite list
                        FavoriteList favoriteList = queryDocumentSnapshots.getDocuments()
                                .get(0).toObject(FavoriteList.class);

                        if (favoriteList != null) {
                            favoriteList.setId(queryDocumentSnapshots.getDocuments().get(0).getId());
                            updateFavoriteList(favoriteList);
                        } else {
                            createNewFavoriteList();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Revert UI change on failure
                    isFavorite = !isFavorite;
                    updateFavoriteUI();
                    Toast.makeText(getContext(), "Error updating favorites: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewFavoriteList() {
        FavoriteList favoriteList = new FavoriteList();
        favoriteList.setClientId(currentUser.getUid());
        favoriteList.setLabel("My Favorites");

        List<String> offerIds = new ArrayList<>();
        offerIds.add(property.getId());
        favoriteList.setOfferIds(offerIds);

        firestore.getDb().collection("favorites")
                .add(favoriteList)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(getContext(), "Added to favorites", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    // Revert UI change on failure
                    isFavorite = !isFavorite;
                    updateFavoriteUI();
                    Toast.makeText(getContext(), "Error adding to favorites: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFavoriteList(FavoriteList favoriteList) {
        if (favoriteList.getOfferIds() == null) {
            favoriteList.setOfferIds(new ArrayList<>());
        }

        if (isFavorite) {
            // Add to favorites if not already added
            if (!favoriteList.getOfferIds().contains(property.getId())) {
                favoriteList.getOfferIds().add(property.getId());
            }
        } else {
            // Remove from favorites
            favoriteList.getOfferIds().remove(property.getId());
        }

        firestore.getDb().collection("favorites")
                .document(favoriteList.getId())
                .set(favoriteList)
                .addOnSuccessListener(aVoid -> {
                    String message = isFavorite ? "Added to favorites" : "Removed from favorites";
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Revert UI change on failure
                    isFavorite = !isFavorite;
                    updateFavoriteUI();
                    Toast.makeText(getContext(), "Error updating favorites: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showMakeRequestDialog() {
        // Create the request dialog fragment and show it
        PropertyRequestDialog requestDialog = PropertyRequestDialog.newInstance(property.getId());
        requestDialog.show(getParentFragmentManager(), "PropertyRequestDialog");
    }

    private void startChat() {
        Log.d("PropertyDetailFragment", "startChat method called");

        try {
            if (currentUser == null) {
                Toast.makeText(getContext(), "You must be logged in to chat", Toast.LENGTH_SHORT).show();
                return;
            }

            if (property == null || property.getAgentId() == null) {
                Toast.makeText(getContext(), "Cannot start chat: missing property or agent info", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading indicator
            Toast.makeText(getContext(), "Starting chat...", Toast.LENGTH_SHORT).show();
            Log.d("PropertyDetailFragment", "Starting chat with agent: " + property.getAgentId());

            // Get client details
            firestore.getDb().collection("clients").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(clientDoc -> {
                        if (!clientDoc.exists()) {
                            // Not a client
                            Toast.makeText(getContext(), "Only clients can start chats", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String firstName = clientDoc.getString("firstName");
                        String lastName = clientDoc.getString("lastName");

                        if (firstName == null) firstName = "";
                        if (lastName == null) lastName = "";

                        String clientName = firstName + " " + lastName;
                        String clientProfileImage = clientDoc.getString("profileImageUrl");

                        Log.d("PropertyDetailFragment", "Client info retrieved: " + clientName);

                        // Get agent details
                        firestore.getDb().collection("agents").document(property.getAgentId())
                                .get()
                                .addOnSuccessListener(agentDoc -> {
                                    if (!agentDoc.exists()) {
                                        Toast.makeText(getContext(), "Agent information not available", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    String agentFirstName = agentDoc.getString("firstName");
                                    String agentLastName = agentDoc.getString("lastName");

                                    if (agentFirstName == null) agentFirstName = "";
                                    if (agentLastName == null) agentLastName = "";

                                    String agentName = agentFirstName + " " + agentLastName;
                                    String agentProfileImage = agentDoc.getString("profileImageUrl");

                                    Log.d("PropertyDetailFragment", "Agent info retrieved: " + agentName);

                                    // Initialize ChatService if needed
                                    ChatService chatService = ChatService.getInstance();

                                    // Start chat using ChatService
                                    chatService.startChatFromProperty(
                                            property.getId(),
                                            currentUser.getUid(),
                                            property.getAgentId(),
                                            clientName,
                                            agentName,
                                            clientProfileImage,
                                            agentProfileImage,
                                            new ChatService.ChatServiceCallback<String>() {
                                                @Override
                                                public void onSuccess(String threadId) {
                                                    Log.d("PropertyDetailFragment", "Chat thread created with ID: " + threadId);

                                                    // Navigate to chat detail screen
                                                    if (getActivity() != null) {
                                                        ChatDetailFragment chatDetailFragment = ChatDetailFragment.newInstance(
                                                                threadId,
                                                                property.getAgentId(),
                                                                agentName,
                                                                agentProfileImage
                                                        );

                                                        getActivity().getSupportFragmentManager().beginTransaction()
                                                                .replace(R.id.content_frame, chatDetailFragment)
                                                                .addToBackStack(null)
                                                                .commit();
                                                    }
                                                }

                                                @Override
                                                public void onFailure(String error) {
                                                    Log.e("PropertyDetailFragment", "Failed to start chat: " + error);
                                                    Toast.makeText(getContext(), "Failed to start chat: " + error, Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("PropertyDetailFragment", "Error getting agent details: " + e.getMessage());
                                    Toast.makeText(getContext(), "Error getting agent details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("PropertyDetailFragment", "Error getting user details: " + e.getMessage());
                        Toast.makeText(getContext(), "Error getting user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e("PropertyDetailFragment", "Exception in startChat: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error starting chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showContactAgentDialog() {
        // Create and show a bottom sheet dialog for contacting the agent
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_contact_agent, null);
        dialog.setContentView(dialogView);

        TextView agentNameText = dialogView.findViewById(R.id.agentNameText);
        Button callButton = dialogView.findViewById(R.id.callButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Load agent details
        firestore.getDb().collection("agents").document(property.getAgentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String phone = documentSnapshot.getString("phone");
                        String email = documentSnapshot.getString("email");

                        // Set agent name
                        if (firstName != null && lastName != null) {
                            agentNameText.setText(String.format("Contact %s %s", firstName, lastName));
                        }

                        // Setup call button
                        callButton.setOnClickListener(v -> {
                            if (phone != null && !phone.isEmpty()) {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:" + phone));
                                startActivity(intent);
                                dialog.dismiss();
                            } else {
                                Toast.makeText(getContext(), "Phone number not available",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

        // Cancel button
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}