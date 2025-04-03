package com.darilink.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.darilink.R;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.Evaluation;
import com.darilink.models.Offer;
import com.google.firebase.auth.FirebaseUser;

public class PropertyReviewFragment extends DialogFragment {

    private static final String ARG_PROPERTY_ID = "property_id";

    private String propertyId;
    private Offer property;

    private TextView propertyTitle;
    private RatingBar ratingBar;
    private EditText reviewText;
    private Button submitButton, cancelButton;

    private Firestore firestore;
    private Firebase firebase;
    private FirebaseUser currentUser;

    public static PropertyReviewFragment newInstance(String propertyId) {
        PropertyReviewFragment fragment = new PropertyReviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROPERTY_ID, propertyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_App_Dialog);

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
        return inflater.inflate(R.layout.fragment_property_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
        loadPropertyDetails();
    }

    private void initViews(View view) {
        propertyTitle = view.findViewById(R.id.propertyTitle);
        ratingBar = view.findViewById(R.id.ratingBar);
        reviewText = view.findViewById(R.id.reviewText);
        submitButton = view.findViewById(R.id.submitButton);
        cancelButton = view.findViewById(R.id.cancelButton);
    }

    private void setupListeners() {
        submitButton.setOnClickListener(v -> {
            if (validateInput()) {
                submitReview();
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void loadPropertyDetails() {
        if (propertyId == null || propertyId.isEmpty()) {
            Toast.makeText(getContext(), "Property ID not found", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        // Load property details
        firestore.getDb().collection("Offer").document(propertyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    property = documentSnapshot.toObject(Offer.class);
                    if (property != null) {
                        property.setId(documentSnapshot.getId());
                        propertyTitle.setText(property.getTitle());

                        // Check if user has already reviewed this property
                        checkExistingReview();
                    } else {
                        Toast.makeText(getContext(), "Property not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    dismiss();
                });
    }

    private void checkExistingReview() {
        if (currentUser == null) return;

        firestore.getDb().collection("evaluations")
                .whereEqualTo("clientId", currentUser.getUid())
                .whereEqualTo("offerId", propertyId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // User has already reviewed this property
                        Evaluation evaluation = queryDocumentSnapshots.getDocuments().get(0).toObject(Evaluation.class);
                        if (evaluation != null) {
                            // Fill in existing review data
                            ratingBar.setRating(evaluation.getRating());
                            reviewText.setText(evaluation.getComment());
                            submitButton.setText(R.string.update_review);
                        }
                    }
                });
    }

    private boolean validateInput() {
        if (ratingBar.getRating() == 0) {
            Toast.makeText(getContext(), "Please provide a rating", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (reviewText.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Please write a review", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void submitReview() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to submit a review", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable buttons to prevent multiple submissions
        submitButton.setEnabled(false);

        // Check if user already has a review for this property
        firestore.getDb().collection("evaluations")
                .whereEqualTo("clientId", currentUser.getUid())
                .whereEqualTo("offerId", propertyId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Create new review
                        createNewReview();
                    } else {
                        // Update existing review
                        Evaluation existingEvaluation = queryDocumentSnapshots.getDocuments().get(0).toObject(Evaluation.class);
                        if (existingEvaluation != null) {
                            existingEvaluation.setId(queryDocumentSnapshots.getDocuments().get(0).getId());
                            updateExistingReview(existingEvaluation);
                        } else {
                            createNewReview();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    submitButton.setEnabled(true);
                    Toast.makeText(getContext(), "Error checking existing reviews: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewReview() {
        Evaluation evaluation = new Evaluation();
        evaluation.setClientId(currentUser.getUid());
        evaluation.setOfferId(propertyId);
        evaluation.setRating((int) ratingBar.getRating());
        evaluation.setComment(reviewText.getText().toString().trim());
        evaluation.setDate(System.currentTimeMillis());

        firestore.getDb().collection("evaluations")
                .add(evaluation)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Review submitted successfully", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    submitButton.setEnabled(true);
                    Toast.makeText(getContext(), "Error submitting review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateExistingReview(Evaluation evaluation) {
        evaluation.setRating((int) ratingBar.getRating());
        evaluation.setComment(reviewText.getText().toString().trim());
        evaluation.setDate(System.currentTimeMillis());

        firestore.getDb().collection("evaluations")
                .document(evaluation.getId())
                .set(evaluation)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Review updated successfully", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    submitButton.setEnabled(true);
                    Toast.makeText(getContext(), "Error updating review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}