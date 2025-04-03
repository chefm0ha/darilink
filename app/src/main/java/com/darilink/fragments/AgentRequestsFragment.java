package com.darilink.fragments;

import android.os.Bundle;
import android.util.Log;
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
import com.darilink.adapters.AgentRequestAdapter;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.Offer;
import com.darilink.models.Request;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentRequestsFragment extends Fragment implements AgentRequestAdapter.AgentRequestListener {

    private static final String TAG = "AgentRequestsFragment";
    private RecyclerView requestsRecyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyStateLayout;

    private Firestore firestore;
    private Firebase firebase;
    private FirebaseUser currentUser;

    private AgentRequestAdapter requestAdapter;
    private List<Request> agentRequests = new ArrayList<>();
    private Map<String, Offer> offersMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agent_requests, container, false);

        // Set title in ActionBar
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(R.string.property_requests);
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
        requestAdapter = new AgentRequestAdapter(getContext(), agentRequests, offersMap, this);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestsRecyclerView.setAdapter(requestAdapter);
    }

    private void loadRequests() {
        if (currentUser == null) {
            showEmptyState(true);
            return;
        }

        swipeRefresh.setRefreshing(true);
        String agentId = currentUser.getUid();

        // Clear existing data
        agentRequests.clear();
        offersMap.clear();

        // First, get agent's properties
        firestore.getDb().collection("Offer")
                .whereEqualTo("agentId", agentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        swipeRefresh.setRefreshing(false);
                        showEmptyState(true);
                        return;
                    }

                    // Build a list of offer IDs and map of offers
                    List<String> offerIds = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Offer offer = document.toObject(Offer.class);
                        if (offer != null) {
                            offer.setId(document.getId());
                            offersMap.put(document.getId(), offer);
                            offerIds.add(document.getId());
                        }
                    }

                    if (offerIds.isEmpty()) {
                        swipeRefresh.setRefreshing(false);
                        showEmptyState(true);
                        return;
                    }

                    loadRequestsForOffers(offerIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading properties", e);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Error loading properties: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                });
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
    public void onRequestDetailClick(Request request, Offer offer) {
        // Show request details dialog for agents
        AgentRequestDetailDialog dialog = AgentRequestDetailDialog.newInstance(request.getId());
        dialog.show(getParentFragmentManager(), "AgentRequestDetailDialog");
    }

    @Override
    public void onPropertyClick(Offer offer) {
        // Navigate to property details
        if (getActivity() != null) {
            PropertyDetailFragment fragment = PropertyDetailFragment.newInstance(offer.getId());
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void loadRequestsForOffers(List<String> offerIds) {
        // Don't proceed if there are no offer IDs
        if (offerIds.isEmpty()) {
            swipeRefresh.setRefreshing(false);
            showEmptyState(true);
            return;
        }

        // Process in batches if list is large
        if (offerIds.size() > 10) {
            // Firebase limits "in" queries to 10 items
            List<List<String>> batches = new ArrayList<>();
            for (int i = 0; i < offerIds.size(); i += 10) {
                batches.add(offerIds.subList(i, Math.min(i + 10, offerIds.size())));
            }

            final int[] batchesCompleted = {0};
            for (List<String> batch : batches) {
                firestore.getDb().collection("requests")
                        .whereIn("offerId", batch)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Request request = doc.toObject(Request.class);
                                if (request != null) {
                                    request.setId(doc.getId());
                                    agentRequests.add(request);
                                }
                            }

                            batchesCompleted[0]++;
                            if (batchesCompleted[0] == batches.size()) {
                                // All batches completed
                                swipeRefresh.setRefreshing(false);
                                requestAdapter.notifyDataSetChanged();
                                showEmptyState(agentRequests.isEmpty());
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error loading requests batch", e);
                            batchesCompleted[0]++;
                            if (batchesCompleted[0] == batches.size()) {
                                // All batches completed
                                swipeRefresh.setRefreshing(false);
                                requestAdapter.notifyDataSetChanged();
                                showEmptyState(agentRequests.isEmpty());
                            }
                        });
            }
        } else {
            // Original query for small number of offers
            firestore.getDb().collection("requests")
                    .whereIn("offerId", offerIds)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        agentRequests.clear();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Request request = doc.toObject(Request.class);
                            if (request != null) {
                                request.setId(doc.getId());
                                agentRequests.add(request);
                            }
                        }

                        swipeRefresh.setRefreshing(false);
                        requestAdapter.notifyDataSetChanged();
                        showEmptyState(agentRequests.isEmpty());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading requests", e);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(getContext(), "Error loading requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        showEmptyState(true);
                    });
        }
    }
}