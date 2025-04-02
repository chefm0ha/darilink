package com.darilink.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.darilink.adapters.PropertyAdapter;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.Offer;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MyPropertiesFragment extends Fragment implements PropertyAdapter.PropertyListener {

    private RecyclerView propertiesRecyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyStateLayout;
    private EditText searchInput;
    private ImageView clearSearchBtn;
    private ChipGroup filterChipGroup;
    private FloatingActionButton addPropertyFab;

    private Firestore firestore;
    private Firebase firebase;
    private PropertyAdapter propertyAdapter;
    private List<Offer> allProperties = new ArrayList<>();
    private List<Offer> filteredProperties = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_properties, container, false);

        // Set title in ActionBar
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(R.string.my_properties);
            }
        }

        initViews(view);
        setupListeners();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initFirebase();
        setupRecyclerView();
        loadProperties();
    }

    private void initViews(View view) {
        propertiesRecyclerView = view.findViewById(R.id.propertiesRecyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        searchInput = view.findViewById(R.id.searchInput);
        clearSearchBtn = view.findViewById(R.id.clearSearchBtn);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
        addPropertyFab = view.findViewById(R.id.addPropertyFab);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadProperties);

        addPropertyFab.setOnClickListener(v -> {
            // Navigate to MakeOfferFragment
            if (getActivity() != null) {
                MakeOfferFragment fragment = MakeOfferFragment.newInstance(null);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProperties();
                clearSearchBtn.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearSearchBtn.setOnClickListener(v -> {
            searchInput.setText("");
            clearSearchBtn.setVisibility(View.GONE);
        });

        filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> filterProperties());
    }

    private void initFirebase() {
        firestore = Firestore.getInstance();
        firebase = Firebase.getInstance();
    }

    private void setupRecyclerView() {
        propertyAdapter = new PropertyAdapter(getContext(), filteredProperties, this);
        propertiesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        propertiesRecyclerView.setAdapter(propertyAdapter);
    }

    private void loadProperties() {
        FirebaseUser currentUser = firebase.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        swipeRefresh.setRefreshing(true);

        firestore.getOffersByAgent(currentUser.getUid(), new Firestore.FirestoreCallback<List<Offer>>() {
            @Override
            public void onSuccess(List<Offer> result) {
                swipeRefresh.setRefreshing(false);
                allProperties.clear();
                allProperties.addAll(result);
                filterProperties();

                updateEmptyState();
            }

            @Override
            public void onFailure(String error) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Error loading properties: " + error, Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void filterProperties() {
        filteredProperties.clear();

        // Get filter criteria
        String searchQuery = searchInput.getText().toString().toLowerCase().trim();
        int checkedChipId = filterChipGroup.getCheckedChipId();

        for (Offer offer : allProperties) {
            // Apply availability filter
            if (checkedChipId == R.id.availablePropertiesChip && !offer.isAvailable()) {
                continue;
            } else if (checkedChipId == R.id.rentedPropertiesChip && offer.isAvailable()) {
                continue;
            }

            // Apply search filter
            if (!searchQuery.isEmpty()) {
                boolean matchesSearch = offer.getTitle().toLowerCase().contains(searchQuery) ||
                        offer.getDescription().toLowerCase().contains(searchQuery) ||
                        offer.getAddress().toLowerCase().contains(searchQuery) ||
                        offer.getCity().toLowerCase().contains(searchQuery) ||
                        offer.getCountry().toLowerCase().contains(searchQuery);

                if (!matchesSearch) {
                    continue;
                }
            }

            // If we get here, the property passed all filters
            filteredProperties.add(offer);
        }

        propertyAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredProperties.isEmpty()) {
            propertiesRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            propertiesRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the list when coming back to this fragment
        if (isAdded()) {
            loadProperties();
        }
    }

    // Property adapter click listeners

    @Override
    public void onPropertyClick(Offer offer) {
        // Navigate to property details view
        // TODO: Implement property details fragment
    }

    @Override
    public void onEditClick(Offer offer) {
        // Navigate to MakeOfferFragment with offer ID
        if (getActivity() != null) {
            MakeOfferFragment fragment = MakeOfferFragment.newInstance(offer.getId());
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onDeleteClick(Offer offer) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_property)
                .setMessage(R.string.delete_property_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteProperty(offer))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteProperty(Offer offer) {
        swipeRefresh.setRefreshing(true);

        firestore.getDb().collection("Offer").document(offer.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Property deleted successfully", Toast.LENGTH_SHORT).show();
                    loadProperties();
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Failed to delete property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}