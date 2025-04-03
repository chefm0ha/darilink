package com.darilink.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;

import com.darilink.R;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.Offer;
import com.darilink.models.Request;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

public class AgentRequestDetailDialog extends DialogFragment {

    private static final String ARG_REQUEST_ID = "request_id";

    private String requestId;
    private Request request;
    private Offer offer;

    // UI components
    private TextView statusBadge, requestDate, propertyTitle;
    private TextView rentProposal, employmentStatus, maritalStatus, numChildren, duration;
    private TextView message, agentReplyLabel;
    private EditText agentReplyInput;
    private Button acceptRequestButton, rejectRequestButton, closeButton, viewPropertyButton;
    private ProgressBar progressBar;
    private LinearLayout agentReplyContainer;
    private NestedScrollView contentLayout;

    private Firestore firestore;
    private Firebase firebase;
    private FirebaseUser currentUser;

    public static AgentRequestDetailDialog newInstance(String requestId) {
        AgentRequestDetailDialog fragment = new AgentRequestDetailDialog();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_ID, requestId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_App_BottomSheetDialog);

        if (getArguments() != null) {
            requestId = getArguments().getString(ARG_REQUEST_ID);
        }

        // Initialize Firebase
        firestore = Firestore.getInstance();
        firebase = Firebase.getInstance();
        currentUser = firebase.getCurrentUser();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), getTheme());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_agent_request_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
        loadRequestDetails();
    }

    private void initViews(View view) {
        // Text views for request and client details
        statusBadge = view.findViewById(R.id.statusBadge);
        requestDate = view.findViewById(R.id.requestDate);
        propertyTitle = view.findViewById(R.id.propertyTitle);
        rentProposal = view.findViewById(R.id.rentProposal);
        employmentStatus = view.findViewById(R.id.employmentStatus);
        maritalStatus = view.findViewById(R.id.maritalStatus);
        numChildren = view.findViewById(R.id.numChildren);
        duration = view.findViewById(R.id.duration);
        message = view.findViewById(R.id.message);

        // Agent reply components
        agentReplyLabel = view.findViewById(R.id.agentReplyLabel);
        agentReplyInput = view.findViewById(R.id.agentReplyInput);
        agentReplyContainer = view.findViewById(R.id.agentReplyContainer);

        // Buttons
        acceptRequestButton = view.findViewById(R.id.acceptRequestButton);
        rejectRequestButton = view.findViewById(R.id.rejectRequestButton);
        closeButton = view.findViewById(R.id.closeButton);
        viewPropertyButton = view.findViewById(R.id.viewPropertyButton);

        // Loading
        progressBar = view.findViewById(R.id.progressBar);
        contentLayout = (NestedScrollView) view.findViewById(R.id.contentLayout);
    }

    private void setupListeners() {
        closeButton.setOnClickListener(v -> dismiss());

        viewPropertyButton.setOnClickListener(v -> {
            if (offer != null) {
                navigateToPropertyDetail();
                dismiss();
            }
        });

        acceptRequestButton.setOnClickListener(v -> {
            String agentReply = agentReplyInput.getText().toString().trim();
            updateRequestStatus("accepted", agentReply);
        });

        rejectRequestButton.setOnClickListener(v -> {
            String agentReply = agentReplyInput.getText().toString().trim();
            updateRequestStatus("rejected", agentReply);
        });
    }

    private void loadRequestDetails() {
        showLoading(true);

        if (requestId == null || requestId.isEmpty()) {
            Toast.makeText(getContext(), "Request ID not found", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        // Load request details
        firestore.getDb().collection("requests").document(requestId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    request = documentSnapshot.toObject(Request.class);
                    if (request != null) {
                        request.setId(documentSnapshot.getId());

                        // Load associated property
                        loadProperty(request.getOfferId());
                    } else {
                        showLoading(false);
                        Toast.makeText(getContext(), "Request not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Error loading request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    dismiss();
                });
    }

    private void loadProperty(String offerId) {
        if (offerId == null || offerId.isEmpty()) {
            displayRequestDetails();
            showLoading(false);
            return;
        }

        firestore.getDb().collection("Offer").document(offerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    offer = documentSnapshot.toObject(Offer.class);
                    if (offer != null) {
                        offer.setId(documentSnapshot.getId());
                    }

                    displayRequestDetails();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    displayRequestDetails();
                    showLoading(false);
                });
    }

    private void displayRequestDetails() {
        if (request == null) return;

        // Set status badge
        String requestStatus = request.getStatus().toLowerCase();
        statusBadge.setText(getStatusDisplayText(requestStatus));
        statusBadge.setBackgroundResource(getStatusBadgeBackground(requestStatus));

        // Set request date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        requestDate.setText(dateFormat.format(new Date(request.getCreatedAt())));

        // Set property title
        if (offer != null) {
            propertyTitle.setText(offer.getTitle());
            viewPropertyButton.setEnabled(true);
        } else {
            propertyTitle.setText(R.string.property_not_found);
            viewPropertyButton.setEnabled(false);
        }

        // Format rent proposal with currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        currencyFormat.setCurrency(Currency.getInstance("USD"));
        rentProposal.setText(currencyFormat.format(request.getRentProposal()));

        // Set personal information
        employmentStatus.setText(request.getEmploymentStatus());
        maritalStatus.setText(request.getMaritalStatus());
        numChildren.setText(String.valueOf(request.getNumChildren()));

        // Set duration
        duration.setText(String.format(Locale.getDefault(), "%d %s",
                request.getDuration(),
                request.getDuration() == 1 ? getString(R.string.month) : getString(R.string.months)));

        // Set message (if any)
        if (request.getMessage() != null && !request.getMessage().isEmpty()) {
            message.setText(request.getMessage());
            message.setVisibility(View.VISIBLE);
        } else {
            message.setVisibility(View.GONE);
        }

        // Manage UI based on request status
        updateActionButtonsVisibility(requestStatus);
    }

    private void updateActionButtonsVisibility(String status) {
        // Hide/show action buttons based on request status
        boolean isPending = "pending".equals(status);
        acceptRequestButton.setVisibility(isPending ? View.VISIBLE : View.GONE);
        rejectRequestButton.setVisibility(isPending ? View.VISIBLE : View.GONE);
        agentReplyContainer.setVisibility(isPending ? View.VISIBLE : View.GONE);

        // If not pending, show the existing reply (if any)
        if (!isPending && request != null && request.getAgentReply() != null) {
            agentReplyLabel.setText(R.string.agent_response);
            agentReplyInput.setText(request.getAgentReply());
            agentReplyInput.setEnabled(false);
            agentReplyContainer.setVisibility(View.VISIBLE);
        }
    }

    private void updateRequestStatus(String newStatus, String agentReply) {
        if (request == null) return;

        // Validate reply for non-accepted statuses
        if (!"accepted".equals(newStatus) && agentReply.isEmpty()) {
            Toast.makeText(getContext(), "Please provide a reason for rejection", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setAlpha(0.5f);
        acceptRequestButton.setEnabled(false);
        rejectRequestButton.setEnabled(false);

        // Update request in Firestore
        firestore.getDb().collection("requests").document(request.getId())
                .update(
                        "status", newStatus,
                        "agentReply", agentReply
                )
                .addOnSuccessListener(aVoid -> {
                    // Update local request object
                    request.setStatus(newStatus);
                    request.setAgentReply(agentReply);

                    // Update UI
                    statusBadge.setText(getStatusDisplayText(newStatus));
                    statusBadge.setBackgroundResource(getStatusBadgeBackground(newStatus));
                    updateActionButtonsVisibility(newStatus.toLowerCase());

                    // Hide loading
                    progressBar.setVisibility(View.GONE);
                    contentLayout.setAlpha(1f);

                    // Show success message
                    String message = newStatus.equals("accepted") ?
                            "Request accepted" : "Request rejected";
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Hide loading
                    progressBar.setVisibility(View.GONE);
                    contentLayout.setAlpha(1f);
                    acceptRequestButton.setEnabled(true);
                    rejectRequestButton.setEnabled(true);

                    Toast.makeText(getContext(),
                            "Error updating request: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private String getStatusDisplayText(String status) {
        switch (status.toLowerCase()) {
            case "pending":
                return getString(R.string.pending);
            case "accepted":
                return getString(R.string.accepted);
            case "rejected":
                return getString(R.string.rejected);
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

    private void navigateToPropertyDetail() {
        if (getActivity() != null && offer != null) {
            PropertyDetailFragment fragment = PropertyDetailFragment.newInstance(offer.getId());
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}