package com.darilink.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.darilink.R;
import com.darilink.adapters.RequestAdapter;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.Offer;
import com.darilink.models.Request;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyRequestsFragment extends Fragment implements RequestAdapter.RequestListener {

    private RecyclerView requestsRecyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyStateLayout;

    private Firestore firestore;
    private Firebase firebase;
    private FirebaseUser currentUser;

    private RequestAdapter requestAdapter;
    private List<Request> myRequests = new ArrayList<>();
    private Map<String, Offer> offersMap = new HashMap<>(); // To store offers by ID

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_requests, container, false);

        // Set title in ActionBar
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(R.string.my_requests);
            }
        }

        initViews(view);
        setupListeners();
        initFirebase();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        loadRequests();
    }

    private void initViews(View view) {
        requestsRecyclerView = view.findViewById(R.id.requestsRecyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadRequests);
    }

    private void initFirebase() {
        firestore = Firestore.getInstance();
        firebase = Firebase.getInstance();
        currentUser = firebase.getCurrentUser();
    }

    private void setupRecyclerView() {
        requestAdapter = new RequestAdapter(getContext(), myRequests, offersMap, this);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestsRecyclerView.setAdapter(requestAdapter);
    }

    private void loadRequests() {
        if (currentUser == null) {
            showEmptyState(true);
            return;
        }

        swipeRefresh.setRefreshing(true);
        String userId = currentUser.getUid();

        // Clear existing data
        myRequests.clear();
        offersMap.clear();

        // Load requests for the current user
        firestore.getDb().collection("requests")
                .whereEqualTo("clientId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        swipeRefresh.setRefreshing(false);
                        showEmptyState(true);
                        return;
                    }

                    // Get requests and store offer IDs to load later
                    List<String> offerIds = new ArrayList<>();
                    for (Request request : queryDocumentSnapshots.toObjects(Request.class)) {
                        request.setId(queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.toObjects(Request.class).indexOf(request)).getId());
                        myRequests.add(request);

                        // Add offer ID to list (avoid duplicates)
                        if (!offerIds.contains(request.getOfferId())) {
                            offerIds.add(request.getOfferId());
                        }
                    }

                    // Load offers information
                    loadOffers(offerIds);
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Error loading requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                });
    }

    private void loadOffers(List<String> offerIds) {
        if (offerIds.isEmpty()) {
            swipeRefresh.setRefreshing(false);
            requestAdapter.notifyDataSetChanged();
            showEmptyState(myRequests.isEmpty());
            return;
        }

        // Load each offer
        final Map<String, Boolean> loadingStatus = new HashMap<>();
        for (String id : offerIds) {
            loadingStatus.put(id, false); // Not loaded yet
        }

        for (String id : offerIds) {
            firestore.getDb().collection("Offer").document(id)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Offer offer = documentSnapshot.toObject(Offer.class);
                        if (offer != null) {
                            offer.setId(documentSnapshot.getId());
                            offersMap.put(documentSnapshot.getId(), offer);
                        }

                        loadingStatus.put(documentSnapshot.getId(), true); // Marked as loaded

                        // Check if all offers are loaded
                        boolean allLoaded = true;
                        for (Boolean status : loadingStatus.values()) {
                            if (!status) {
                                allLoaded = false;
                                break;
                            }
                        }

                        if (allLoaded) {
                            // All offers loaded, update UI
                            swipeRefresh.setRefreshing(false);
                            requestAdapter.notifyDataSetChanged();
                            showEmptyState(myRequests.isEmpty());
                        }
                    })
                    .addOnFailureListener(e -> {
                        loadingStatus.put(id, true); // Mark as loaded even if it failed

                        // Check if all offers are loaded
                        boolean allLoaded = true;
                        for (Boolean status : loadingStatus.values()) {
                            if (!status) {
                                allLoaded = false;
                                break;
                            }
                        }

                        if (allLoaded) {
                            // All offers loaded, update UI
                            swipeRefresh.setRefreshing(false);
                            requestAdapter.notifyDataSetChanged();
                            showEmptyState(myRequests.isEmpty());
                        }
                    });
        }
    }

    private void showEmptyState(boolean isEmpty) {
        if (isEmpty) {
            requestsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            requestsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestClick(Request request, Offer offer) {
        // Navigate to request detail dialog
        RequestDetailDialog dialog = RequestDetailDialog.newInstance(request.getId());
        dialog.show(getParentFragmentManager(), "RequestDetailDialog");
    }

    @Override
    public void onPropertyClick(Offer offer) {
        // Navigate to property details
        if (getActivity() != null && offer != null) {
            PropertyDetailFragment fragment = PropertyDetailFragment.newInstance(offer.getId());
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}