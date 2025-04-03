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
import com.darilink.adapters.AgentRequestAdapter;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.Offer;
import com.darilink.models.Request;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentRequestsFragment extends Fragment implements AgentRequestAdapter.AgentRequestListener {

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
        firestore.getOffersByAgent(agentId, new Firestore.FirestoreCallback<List<Offer>>() {
            @Override
            public void onSuccess(List<Offer> agentOffers) {
                if (agentOffers.isEmpty()) {
                    swipeRefresh.setRefreshing(false);
                    showEmptyState(true);
                    return;
                }

                // Store offers in map for quick lookup
                for (Offer offer : agentOffers) {
                    offersMap.put(offer.getId(), offer);
                }

                // Get requests for these properties
                List<String> offerIds = new ArrayList<>();
                for (Offer offer : agentOffers) {
                    offerIds.add(offer.getId());
                }

                loadRequestsForOffers(offerIds);
            }

            @Override
            public void onFailure(String error) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Error loading properties: " + error, Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            }
        });
    }

    private void loadRequestsForOffers(List<String> offerIds) {
        firestore.getDb().collection("requests")
                .whereIn("offerId", offerIds)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    agentRequests.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Request request = document.toObject(Request.class);
                        request.setId(document.getId());
                        agentRequests.add(request);
                    }

                    swipeRefresh.setRefreshing(false);
                    requestAdapter.notifyDataSetChanged();
                    showEmptyState(agentRequests.isEmpty());
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Error loading requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
}