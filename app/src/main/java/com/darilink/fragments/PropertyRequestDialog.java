package com.darilink.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.darilink.R;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.Offer;
import com.darilink.models.Request;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class PropertyRequestDialog extends BottomSheetDialogFragment {

    private static final String ARG_PROPERTY_ID = "property_id";

    private String propertyId;
    private Offer property;

    // UI components
    private TextView propertyTitleText, rentAmountText;
    private EditText messageInput;
    private Slider rentProposalSlider;
    private TextView rentProposalText;
    private AutoCompleteTextView employmentStatusInput, maritalStatusInput;
    private EditText numChildrenInput, durationInput;
    private Button submitRequestButton, cancelButton;
    private ProgressBar progressBar;
    private View formLayout;

    private Firestore firestore;
    private Firebase firebase;
    private FirebaseUser currentUser;

    public static PropertyRequestDialog newInstance(String propertyId) {
        PropertyRequestDialog fragment = new PropertyRequestDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PROPERTY_ID, propertyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_App_BottomSheetDialog);

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
        return inflater.inflate(R.layout.dialog_property_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
        setupDropdowns();

        // Load property details
        loadPropertyDetails();
    }

    private void initViews(View view) {
        propertyTitleText = view.findViewById(R.id.propertyTitleText);
        rentAmountText = view.findViewById(R.id.rentAmountText);
        messageInput = view.findViewById(R.id.messageInput);
        rentProposalSlider = view.findViewById(R.id.rentProposalSlider);
        rentProposalText = view.findViewById(R.id.rentProposalText);
        employmentStatusInput = view.findViewById(R.id.employmentStatusInput);
        maritalStatusInput = view.findViewById(R.id.maritalStatusInput);
        numChildrenInput = view.findViewById(R.id.numChildrenInput);
        durationInput = view.findViewById(R.id.durationInput);
        submitRequestButton = view.findViewById(R.id.submitRequestButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        progressBar = view.findViewById(R.id.progressBar);
        formLayout = view.findViewById(R.id.formLayout);
    }

    private void setupListeners() {
        // Setup rent proposal slider
        rentProposalSlider.addOnChangeListener((slider, value, fromUser) -> {
            updateRentProposalText(value);
        });

        // Submit button
        submitRequestButton.setOnClickListener(v -> {
            if (validateInputs()) {
                submitRequest();
            }
        });

        // Cancel button
        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void setupDropdowns() {
        // Employment status dropdown
        String[] employmentStatuses = {"Employed", "Self-employed", "Unemployed", "Student", "Retired"};
        ArrayAdapter<String> employmentAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                employmentStatuses);
        employmentStatusInput.setAdapter(employmentAdapter);

        // Marital status dropdown
        String[] maritalStatuses = {"Single", "Married", "Divorced", "Widowed"};
        ArrayAdapter<String> maritalAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                maritalStatuses);
        maritalStatusInput.setAdapter(maritalAdapter);
    }

    private void loadPropertyDetails() {
        showLoading(true);

        firestore.getDb().collection("Offer").document(propertyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    property = documentSnapshot.toObject(Offer.class);
                    if (property != null) {
                        property.setId(documentSnapshot.getId());
                        displayPropertyDetails();
                    } else {
                        Toast.makeText(getContext(), "Property not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    dismiss();
                });
    }

    private void displayPropertyDetails() {
        propertyTitleText.setText(property.getTitle());

        // Format rent with currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        currencyFormat.setCurrency(Currency.getInstance("USD"));
        rentAmountText.setText(String.format("Listed for %s/month", currencyFormat.format(property.getRent())));

        // Setup rent proposal slider based on property rent
        double minRent = property.getRent() * 0.7; // 70% of listed rent
        double maxRent = property.getRent() * 1.2; // 120% of listed rent

        rentProposalSlider.setValueFrom((float) minRent);
        rentProposalSlider.setValueTo((float) maxRent);
        rentProposalSlider.setValue((float) property.getRent());

        updateRentProposalText(property.getRent());
    }

    private void updateRentProposalText(double rentAmount) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        currencyFormat.setCurrency(Currency.getInstance("USD"));
        rentProposalText.setText(String.format("Your offer: %s/month", currencyFormat.format(rentAmount)));
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Message input is optional

        // Employment status
        if (employmentStatusInput.getText().toString().isEmpty()) {
            employmentStatusInput.setError("Please select your employment status");
            isValid = false;
        }

        // Marital status
        if (maritalStatusInput.getText().toString().isEmpty()) {
            maritalStatusInput.setError("Please select your marital status");
            isValid = false;
        }

        // Number of children (optional but must be valid if provided)
        String numChildrenStr = numChildrenInput.getText().toString().trim();
        if (!numChildrenStr.isEmpty()) {
            try {
                int numChildren = Integer.parseInt(numChildrenStr);
                if (numChildren < 0) {
                    numChildrenInput.setError("Invalid number");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                numChildrenInput.setError("Please enter a valid number");
                isValid = false;
            }
        }

        // Duration (required)
        String durationStr = durationInput.getText().toString().trim();
        if (durationStr.isEmpty()) {
            durationInput.setError("Please enter rental duration");
            isValid = false;
        } else {
            try {
                int duration = Integer.parseInt(durationStr);
                if (duration <= 0) {
                    durationInput.setError("Duration must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                durationInput.setError("Please enter a valid number");
                isValid = false;
            }
        }

        return isValid;
    }

    private void submitRequest() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to make a request", Toast.LENGTH_SHORT).show();
            return;
        }

        if (property == null) {
            Toast.makeText(getContext(), "Property information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Create new request
        Request request = new Request();
        request.setClientId(currentUser.getUid());
        request.setOfferId(property.getId());
        request.setMessage(messageInput.getText().toString().trim());
        request.setRentProposal(rentProposalSlider.getValue());
        request.setEmploymentStatus(employmentStatusInput.getText().toString());
        request.setMaritalStatus(maritalStatusInput.getText().toString());

        // Parse optional number of children
        String numChildrenStr = numChildrenInput.getText().toString().trim();
        int numChildren = numChildrenStr.isEmpty() ? 0 : Integer.parseInt(numChildrenStr);
        request.setNumChildren(numChildren);

        // Parse duration
        int duration = Integer.parseInt(durationInput.getText().toString().trim());
        request.setDuration(duration);

        // Set timestamps and status
        request.setCreatedAt(System.currentTimeMillis());
        request.setStatus("pending");

        // Save to Firestore
        firestore.getDb().collection("requests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Request submitted successfully", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Failed to submit request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        formLayout.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
}