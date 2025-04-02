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
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.darilink.R;
import com.darilink.adapters.FavoritesAdapter;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.FavoriteList;
import com.darilink.models.Offer;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritesFragment extends Fragment implements FavoritesAdapter.FavoriteListener {

    private RecyclerView favoritesRecyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyStateLayout;
    private Button createNewListButton;
    private TextView createListText;

    private Firestore firestore;
    private Firebase firebase;
    private FirebaseUser currentUser;

    private FavoritesAdapter favoritesAdapter;
    private List<Offer> favoriteProperties = new ArrayList<>();
    private FavoriteList userFavoriteList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        // Set title in ActionBar
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(R.string.favorites);
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
        loadFavorites();
    }

    private void initViews(View view) {
        favoritesRecyclerView = view.findViewById(R.id.favoritesRecyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        createNewListButton = view.findViewById(R.id.createNewListButton);
        createListText = view.findViewById(R.id.createListText);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadFavorites);

        createNewListButton.setOnClickListener(v -> createNewFavoriteList());
    }

    private void initFirebase() {
        firestore = Firestore.getInstance();
        firebase = Firebase.getInstance();
        currentUser = firebase.getCurrentUser();
    }

    private void setupRecyclerView() {
        favoritesAdapter = new FavoritesAdapter(getContext(), favoriteProperties, this);
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        favoritesRecyclerView.setAdapter(favoritesAdapter);
    }

    private void loadFavorites() {
        if (currentUser == null) {
            showEmptyState(true);
            return;
        }

        swipeRefresh.setRefreshing(true);
        String userId = currentUser.getUid();

        // First, load user's favorite list
        firestore.getDb().collection("favorites")
                .whereEqualTo("clientId", userId)
                .limit(1) // For now, we're just supporting one list per user
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No favorite list found
                        swipeRefresh.setRefreshing(false);
                        showEmptyState(true);
                        return;
                    }

                    // Get the favorite list
                    userFavoriteList = queryDocumentSnapshots.getDocuments().get(0).toObject(FavoriteList.class);
                    if (userFavoriteList != null) {
                        userFavoriteList.setId(queryDocumentSnapshots.getDocuments().get(0).getId());

                        // Check if the list has any properties
                        if (userFavoriteList.getOfferIds() == null || userFavoriteList.getOfferIds().isEmpty()) {
                            swipeRefresh.setRefreshing(false);
                            showEmptyState(true);
                            return;
                        }

                        // Load the favorite properties
                        loadFavoriteProperties(userFavoriteList.getOfferIds());
                    } else {
                        swipeRefresh.setRefreshing(false);
                        showEmptyState(true);
                    }
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Error loading favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                });
    }

    private void loadFavoriteProperties(List<String> propertyIds) {
        if (propertyIds.isEmpty()) {
            swipeRefresh.setRefreshing(false);
            showEmptyState(true);
            return;
        }

        // We need to load each property individually since Firestore doesn't support "IN" queries with more than 10 items
        favoriteProperties.clear();
        final Map<String, Boolean> loadingStatus = new HashMap<>();
        for (String id : propertyIds) {
            loadingStatus.put(id, false); // Not loaded yet
        }

        for (String id : propertyIds) {
            firestore.getDb().collection("Offer").document(id)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Offer offer = documentSnapshot.toObject(Offer.class);
                        if (offer != null) {
                            offer.setId(documentSnapshot.getId());
                            favoriteProperties.add(offer);
                        }

                        loadingStatus.put(documentSnapshot.getId(), true); // Marked as loaded

                        // Check if all properties are loaded
                        boolean allLoaded = true;
                        for (Boolean status : loadingStatus.values()) {
                            if (!status) {
                                allLoaded = false;
                                break;
                            }
                        }

                        if (allLoaded) {
                            // All properties loaded, update UI
                            swipeRefresh.setRefreshing(false);
                            favoritesAdapter.notifyDataSetChanged();
                            showEmptyState(favoriteProperties.isEmpty());
                        }
                    })
                    .addOnFailureListener(e -> {
                        loadingStatus.put(id, true); // Mark as loaded even if it failed

                        // Check if all properties are loaded
                        boolean allLoaded = true;
                        for (Boolean status : loadingStatus.values()) {
                            if (!status) {
                                allLoaded = false;
                                break;
                            }
                        }

                        if (allLoaded) {
                            // All properties loaded, update UI
                            swipeRefresh.setRefreshing(false);
                            favoritesAdapter.notifyDataSetChanged();
                            showEmptyState(favoriteProperties.isEmpty());
                        }
                    });
        }
    }

    private void showEmptyState(boolean isEmpty) {
        if (isEmpty) {
            favoritesRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);

            // Show different message if user has no favorite list
            if (userFavoriteList == null) {
                createListText.setText(R.string.no_favorite_list);
                createNewListButton.setText(R.string.create_favorite_list);
            } else {
                createListText.setText(R.string.no_favorites_added);
                createNewListButton.setText(R.string.browse_properties);
            }
        } else {
            favoritesRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private void createNewFavoriteList() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to create a favorites list", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userFavoriteList != null) {
            // User already has a list, navigate to search
            navigateToSearch();
            return;
        }

        // Show dialog to create a new list
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.create_favorite_list);
        builder.setMessage(R.string.create_favorite_list_message);
        builder.setPositiveButton(R.string.create, (dialog, which) -> {
            FavoriteList newList = new FavoriteList();
            newList.setClientId(currentUser.getUid());
            newList.setLabel("My Favorites");
            newList.setOfferIds(new ArrayList<>());

            firestore.getDb().collection("favorites")
                    .add(newList)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Favorites list created!", Toast.LENGTH_SHORT).show();
                        loadFavorites(); // Reload favorites
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error creating list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void navigateToSearch() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new SearchPropertiesFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onFavoriteClick(Offer offer) {
        // Navigate to property details
        if (getActivity() != null) {
            PropertyDetailFragment fragment = PropertyDetailFragment.newInstance(offer.getId());
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onRemoveFavorite(Offer offer) {
        if (userFavoriteList == null || offer == null) return;

        // Remove from the list
        if (userFavoriteList.getOfferIds() != null) {
            userFavoriteList.getOfferIds().remove(offer.getId());

            // Update in Firestore
            firestore.getDb().collection("favorites")
                    .document(userFavoriteList.getId())
                    .set(userFavoriteList)
                    .addOnSuccessListener(aVoid -> {
                        // Remove from local list and update adapter
                        favoriteProperties.remove(offer);
                        favoritesAdapter.notifyDataSetChanged();

                        // Show empty state if needed
                        showEmptyState(favoriteProperties.isEmpty());

                        Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error removing from favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}