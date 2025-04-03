package com.darilink.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.darilink.R;
import com.darilink.adapters.ReviewAdapter;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.Evaluation;
import com.darilink.models.Offer;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyReviewsFragment extends Fragment {

    private static final String ARG_PROPERTY_ID = "property_id";

    private String propertyId;
    private Offer property;

    // UI components
    private RecyclerView reviewsRecyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyStateLayout;
    private Button writeReviewButton;
    private TextView averageRatingText, reviewCountText;

    private Firestore firestore;
    private Firebase firebase;
    private FirebaseUser currentUser;

    private ReviewAdapter reviewAdapter;
    private List<Evaluation> reviewsList = new ArrayList<>();
    private Map<String, String> usernames = new HashMap<>(); // Map to store user names by ID

    public static PropertyReviewsFragment newInstance(String propertyId) {
        PropertyReviewsFragment fragment = new PropertyReviewsFragment();
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_property_reviews, container, false);

        initViews(view);
        setupListeners();
        setupRecyclerView();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadPropertyDetails();
    }

    private void initViews(View view) {
        reviewsRecyclerView = view.findViewById(R.id.reviewsRecyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        writeReviewButton = view.findViewById(R.id.writeReviewButton);
        averageRatingText = view.findViewById(R.id.averageRatingText);
        reviewCountText = view.findViewById(R.id.reviewCountText);
    }

    private void setupListeners() {
        writeReviewButton.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(getContext(), "You must be logged in to write a review", Toast.LENGTH_SHORT).show();
                return;
            }

            // Open review dialog
            PropertyReviewFragment reviewDialog = PropertyReviewFragment.newInstance(propertyId);
            reviewDialog.show(getParentFragmentManager(), "PropertyReviewDialog");
        });

        swipeRefresh.setOnRefreshListener(() -> {
            // Reload reviews
            loadReviews();
        });
    }

    private void setupRecyclerView() {
        reviewAdapter = new ReviewAdapter(getContext(), reviewsList, usernames);
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reviewsRecyclerView.setAdapter(reviewAdapter);
    }

    private void loadPropertyDetails() {
        if (propertyId == null || propertyId.isEmpty()) {
            Toast.makeText(getContext(), "Property ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        swipeRefresh.setRefreshing(true);

        // Load property details
        firestore.getDb().collection("Offer").document(propertyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    property = documentSnapshot.toObject(Offer.class);
                    if (property != null) {
                        property.setId(documentSnapshot.getId());
                        // Load reviews for this property
                        loadReviews();
                    } else {
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(getContext(), "Property not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Error loading property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReviews() {
        if (propertyId == null) {
            swipeRefresh.setRefreshing(false);
            return;
        }

        // Clear existing data
        reviewsList.clear();
        usernames.clear();

        // Load reviews for this property
        firestore.getDb().collection("evaluations")
                .whereEqualTo("offerId", propertyId)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        updateUIWithReviews(new ArrayList<>());
                        return;
                    }

                    // Get reviews
                    List<Evaluation> reviews = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Evaluation review = document.toObject(Evaluation.class);
                        review.setId(document.getId());
                        reviews.add(review);
                    }

                    // Load user names for each review
                    loadUserNames(reviews);
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Error loading reviews: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                });
    }

    private void loadUserNames(List<Evaluation> reviews) {
        if (reviews.isEmpty()) {
            updateUIWithReviews(reviews);
            return;
        }

        // Get unique user IDs
        List<String> userIds = new ArrayList<>();
        for (Evaluation review : reviews) {
            if (!userIds.contains(review.getClientId())) {
                userIds.add(review.getClientId());
            }
        }

        // Load client names
        int[] loadedCount = {0};
        for (String userId : userIds) {
            // First check in clients collection
            firestore.getDb().collection("clients").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");
                            if (firstName != null && lastName != null) {
                                usernames.put(userId, firstName + " " + lastName);
                            } else {
                                usernames.put(userId, "Anonymous Client");
                            }
                        } else {
                            // If not found in clients, try agents collection
                            firestore.getDb().collection("agents").document(userId)
                                    .get()
                                    .addOnSuccessListener(agentSnapshot -> {
                                        if (agentSnapshot.exists()) {
                                            String firstName = agentSnapshot.getString("firstName");
                                            String lastName = agentSnapshot.getString("lastName");
                                            if (firstName != null && lastName != null) {
                                                usernames.put(userId, firstName + " " + lastName + " (Agent)");
                                            } else {
                                                usernames.put(userId, "Anonymous Agent");
                                            }
                                        } else {
                                            usernames.put(userId, "Anonymous User");
                                        }

                                        checkIfAllUsersLoaded(++loadedCount[0], userIds.size(), reviews);
                                    })
                                    .addOnFailureListener(e -> {
                                        usernames.put(userId, "Anonymous User");
                                        checkIfAllUsersLoaded(++loadedCount[0], userIds.size(), reviews);
                                    });
                        }

                        checkIfAllUsersLoaded(++loadedCount[0], userIds.size(), reviews);
                    })
                    .addOnFailureListener(e -> {
                        usernames.put(userId, "Anonymous User");
                        checkIfAllUsersLoaded(++loadedCount[0], userIds.size(), reviews);
                    });
        }
    }

    private void checkIfAllUsersLoaded(int loadedCount, int totalCount, List<Evaluation> reviews) {
        if (loadedCount >= totalCount) {
            // All users loaded, update UI
            updateUIWithReviews(reviews);
        }
    }

    private void updateUIWithReviews(List<Evaluation> reviews) {
        swipeRefresh.setRefreshing(false);
        reviewsList.clear();
        reviewsList.addAll(reviews);
        reviewAdapter.notifyDataSetChanged();

        // Update review stats
        updateReviewStatistics(reviews);

        // Show/hide empty state
        showEmptyState(reviews.isEmpty());
    }

    private void updateReviewStatistics(List<Evaluation> reviews) {
        if (reviews.isEmpty()) {
            averageRatingText.setText("0.0");
            reviewCountText.setText(getString(R.string.no_reviews_yet));
            return;
        }

        // Calculate average rating
        float totalRating = 0;
        for (Evaluation review : reviews) {
            totalRating += review.getRating();
        }

        float averageRating = totalRating / reviews.size();

        // Update UI
        averageRatingText.setText(String.format("%.1f", averageRating));
        reviewCountText.setText(getResources().getQuantityString(
                R.plurals.review_count, reviews.size(), reviews.size()));
    }

    private void showEmptyState(boolean isEmpty) {
        if (isEmpty) {
            reviewsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            reviewsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload reviews in case they were updated
        if (isAdded()) {
            loadReviews();
        }
    }
}