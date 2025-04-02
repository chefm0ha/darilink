package com.darilink.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.darilink.adapters.ClientPropertyAdapter;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.FavoriteList;
import com.darilink.models.Offer;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class SearchPropertiesFragment extends Fragment implements ClientPropertyAdapter.PropertyListener {

    private RecyclerView propertiesRecyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyStateLayout;
    private EditText searchInput;
    private ImageView clearSearchBtn;
    private ChipGroup sortChipGroup;
    private Button filterButton;

    // Filter variables
    private String selectedCity = "";
    private String selectedCountry = "";
    private int minPrice = 0;
    private int maxPrice = 10000;
    private int minBedrooms = 0;
    private int maxBedrooms = 10;
    private List<String> selectedAmenities = new ArrayList<>();
    private String selectedPropertyType = "";

    private Firestore firestore;
    private Firebase firebase;
    private FirebaseUser currentUser;
    private ClientPropertyAdapter propertyAdapter;
    private List<Offer> allProperties = new ArrayList<>();
    private List<Offer> filteredProperties = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_properties, container, false);

        // Set title in ActionBar
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(R.string.search_properties);
            }
        }

        initViews(view);
        setupListeners();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initFirestore();
        setupRecyclerView();
        loadProperties();
    }

    private void initViews(View view) {
        propertiesRecyclerView = view.findViewById(R.id.propertiesRecyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        searchInput = view.findViewById(R.id.searchInput);
        clearSearchBtn = view.findViewById(R.id.clearSearchBtn);
        sortChipGroup = view.findViewById(R.id.sortChipGroup);
        filterButton = view.findViewById(R.id.filterButton);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadProperties);

        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProperties();
                clearSearchBtn.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        clearSearchBtn.setOnClickListener(v -> {
            searchInput.setText("");
            clearSearchBtn.setVisibility(View.GONE);
        });

        sortChipGroup.setOnCheckedChangeListener((group, checkedId) -> sortProperties());

        filterButton.setOnClickListener(v -> showFilterDialog());
    }

    private void initFirestore() {
        firestore = Firestore.getInstance();
        firebase = Firebase.getInstance();
        currentUser = firebase.getCurrentUser();
    }

    private void setupRecyclerView() {
        propertyAdapter = new ClientPropertyAdapter(getContext(), filteredProperties, this);
        propertiesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        propertiesRecyclerView.setAdapter(propertyAdapter);
    }

    private void loadProperties() {
        swipeRefresh.setRefreshing(true);

        firestore.getDb().collection("Offer")
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    swipeRefresh.setRefreshing(false);
                    allProperties.clear();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Offer offer = document.toObject(Offer.class);
                        if (offer != null) {
                            offer.setId(document.getId());
                            allProperties.add(offer);
                        }
                    }

                    // Default sort: Newest first
                    Collections.sort(allProperties, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));

                    filterProperties();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Error loading properties: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void filterProperties() {
        filteredProperties.clear();

        // Get search query
        String searchQuery = searchInput.getText().toString().toLowerCase().trim();

        for (Offer offer : allProperties) {
            // Check if offer is available
            if (!offer.isAvailable()) continue;

            // Apply search query filter
            if (!searchQuery.isEmpty()) {
                boolean matchesSearch = offer.getTitle().toLowerCase().contains(searchQuery) ||
                        offer.getDescription().toLowerCase().contains(searchQuery) ||
                        offer.getAddress().toLowerCase().contains(searchQuery) ||
                        offer.getCity().toLowerCase().contains(searchQuery) ||
                        offer.getCountry().toLowerCase().contains(searchQuery);

                if (!matchesSearch) continue;
            }

            // Apply city filter
            if (!selectedCity.isEmpty() && !offer.getCity().equals(selectedCity)) continue;

            // Apply country filter
            if (!selectedCountry.isEmpty() && !offer.getCountry().equals(selectedCountry)) continue;

            // Apply price filter
            if (offer.getRent() < minPrice || offer.getRent() > maxPrice) continue;

            // Apply bedroom filter
            if (offer.getNumBedrooms() < minBedrooms || offer.getNumBedrooms() > maxBedrooms) continue;

            // Apply property type filter
            if (!selectedPropertyType.isEmpty() && !offer.getPropertyType().equals(selectedPropertyType)) continue;

            // Apply amenities filter
            if (!selectedAmenities.isEmpty()) {
                boolean hasAllAmenities = true;
                for (String amenity : selectedAmenities) {
                    if (offer.getAmenities() == null || !offer.getAmenities().contains(amenity)) {
                        hasAllAmenities = false;
                        break;
                    }
                }
                if (!hasAllAmenities) continue;
            }

            // If we get here, the property passed all filters
            filteredProperties.add(offer);
        }

        // Apply sorting
        sortProperties();

        // Update UI
        propertyAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void sortProperties() {
        int checkedId = sortChipGroup.getCheckedChipId();

        if (checkedId == R.id.sortNewest) {
            // Sort by newest first
            Collections.sort(filteredProperties, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
        } else if (checkedId == R.id.sortPriceAsc) {
            // Sort by price (low to high)
            Collections.sort(filteredProperties, Comparator.comparingDouble(Offer::getRent));
        } else if (checkedId == R.id.sortPriceDesc) {
            // Sort by price (high to low)
            Collections.sort(filteredProperties, (o1, o2) -> Double.compare(o2.getRent(), o1.getRent()));
        } else if (checkedId == R.id.sortArea) {
            // Sort by area (largest first)
            Collections.sort(filteredProperties, (o1, o2) -> Double.compare(o2.getArea(), o1.getArea()));
        }

        propertyAdapter.notifyDataSetChanged();
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

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_property_filter, null);
        dialog.setContentView(dialogView);

        // Find views in the dialog
        EditText cityInput = dialogView.findViewById(R.id.cityInput);
        EditText countryInput = dialogView.findViewById(R.id.countryInput);
        TextView priceRangeText = dialogView.findViewById(R.id.priceRangeText);
        RangeSlider priceRangeSlider = dialogView.findViewById(R.id.priceRangeSlider);
        TextView bedroomsRangeText = dialogView.findViewById(R.id.bedroomsRangeText);
        RangeSlider bedroomsRangeSlider = dialogView.findViewById(R.id.bedroomsRangeSlider);
        ChipGroup propertyTypeChipGroup = dialogView.findViewById(R.id.propertyTypeChipGroup);
        ChipGroup amenitiesChipGroup = dialogView.findViewById(R.id.amenitiesChipGroup);
        Button applyFiltersButton = dialogView.findViewById(R.id.applyFiltersButton);
        Button resetFiltersButton = dialogView.findViewById(R.id.resetFiltersButton);

        // Set current filter values
        cityInput.setText(selectedCity);
        countryInput.setText(selectedCountry);
        priceRangeSlider.setValues((float) minPrice, (float) maxPrice);
        bedroomsRangeSlider.setValues((float) minBedrooms, (float) maxBedrooms);

        // Format price range text
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        currencyFormat.setCurrency(Currency.getInstance("USD"));
        priceRangeText.setText(String.format("Price: %s - %s",
                currencyFormat.format(minPrice), currencyFormat.format(maxPrice)));

        bedroomsRangeText.setText(String.format("Bedrooms: %d - %d", minBedrooms, maxBedrooms));

        // Setup property type chip group
        if (!selectedPropertyType.isEmpty()) {
            for (int i = 0; i < propertyTypeChipGroup.getChildCount(); i++) {
                View view = propertyTypeChipGroup.getChildAt(i);
                if (view instanceof Chip) {
                    Chip chip = (Chip) view;
                    if (chip.getText().toString().equals(selectedPropertyType)) {
                        chip.setChecked(true);
                        break;
                    }
                }
            }
        }

        // Setup amenities chip group
        for (int i = 0; i < amenitiesChipGroup.getChildCount(); i++) {
            View view = amenitiesChipGroup.getChildAt(i);
            if (view instanceof Chip) {
                Chip chip = (Chip) view;
                chip.setChecked(selectedAmenities.contains(chip.getText().toString()));
            }
        }

        // Setup price range slider listener
        priceRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            priceRangeText.setText(String.format("Price: %s - %s",
                    currencyFormat.format(values.get(0)), currencyFormat.format(values.get(1))));
        });

        // Setup bedrooms range slider listener
        bedroomsRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            bedroomsRangeText.setText(String.format("Bedrooms: %d - %d", Math.round(values.get(0)), Math.round(values.get(1))));
        });

        // Apply filters button
        applyFiltersButton.setOnClickListener(v -> {
            // Get selected property type
            selectedPropertyType = "";
            int checkedPropertyTypeId = propertyTypeChipGroup.getCheckedChipId();
            if (checkedPropertyTypeId != View.NO_ID) {
                Chip selectedChip = dialogView.findViewById(checkedPropertyTypeId);
                if (selectedChip != null) {
                    selectedPropertyType = selectedChip.getText().toString();
                }
            }

            // Get selected amenities
            selectedAmenities.clear();
            for (int i = 0; i < amenitiesChipGroup.getChildCount(); i++) {
                View view = amenitiesChipGroup.getChildAt(i);
                if (view instanceof Chip) {
                    Chip chip = (Chip) view;
                    if (chip.isChecked()) {
                        selectedAmenities.add(chip.getText().toString());
                    }
                }
            }

            // Get other filter values
            selectedCity = cityInput.getText().toString().trim();
            selectedCountry = countryInput.getText().toString().trim();

            List<Float> priceValues = priceRangeSlider.getValues();
            minPrice = Math.round(priceValues.get(0));
            maxPrice = Math.round(priceValues.get(1));

            List<Float> bedroomValues = bedroomsRangeSlider.getValues();
            minBedrooms = Math.round(bedroomValues.get(0));
            maxBedrooms = Math.round(bedroomValues.get(1));

            // Apply filters
            filterProperties();

            // Dismiss dialog
            dialog.dismiss();
        });

        // Reset filters button
        resetFiltersButton.setOnClickListener(v -> {
            selectedCity = "";
            selectedCountry = "";
            minPrice = 0;
            maxPrice = 10000;
            minBedrooms = 0;
            maxBedrooms = 10;
            selectedAmenities.clear();
            selectedPropertyType = "";

            filterProperties();
            dialog.dismiss();
        });

        dialog.show();
    }

    // Property click handlers
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

    @Override
    public void onFavoriteClick(Offer offer, boolean isFavorite) {
        // Handle favorite toggle
        if (isFavorite) {
            // Add to favorites
            addToFavorites(offer);
        } else {
            // Remove from favorites
            removeFromFavorites(offer);
        }
    }

    private void addToFavorites(Offer offer) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to add favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find user's default favorite list or create one
        firestore.getDb().collection("favorites")
                .whereEqualTo("clientId", currentUser.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Create new favorite list for user
                        createNewFavoriteList(offer);
                    } else {
                        // Update existing favorite list
                        FavoriteList favoriteList = queryDocumentSnapshots.getDocuments()
                                .get(0).toObject(FavoriteList.class);

                        if (favoriteList != null) {
                            favoriteList.setId(queryDocumentSnapshots.getDocuments().get(0).getId());
                            updateFavoriteList(favoriteList, offer, true);
                        } else {
                            createNewFavoriteList(offer);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error adding to favorites: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFromFavorites(Offer offer) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to remove favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.getDb().collection("favorites")
                .whereEqualTo("clientId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            FavoriteList favoriteList = doc.toObject(FavoriteList.class);
                            if (favoriteList != null) {
                                favoriteList.setId(doc.getId());
                                if (favoriteList.getOfferIds() != null &&
                                        favoriteList.getOfferIds().contains(offer.getId())) {
                                    updateFavoriteList(favoriteList, offer, false);
                                    break;
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error removing from favorites: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewFavoriteList(Offer offer) {
        FavoriteList favoriteList = new FavoriteList();
        favoriteList.setClientId(currentUser.getUid());
        favoriteList.setLabel("My Favorites");

        List<String> offerIds = new ArrayList<>();
        offerIds.add(offer.getId());
        favoriteList.setOfferIds(offerIds);

        firestore.getDb().collection("favorites")
                .add(favoriteList)
                .addOnSuccessListener(documentReference -> {
                    propertyAdapter.updateFavorites(offerIds);
                    Toast.makeText(getContext(), "Added to favorites", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error adding to favorites: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFavoriteList(FavoriteList favoriteList, Offer offer, boolean add) {
        if (favoriteList.getOfferIds() == null) {
            favoriteList.setOfferIds(new ArrayList<>());
        }

        if (add) {
            // Add to favorites if not already added
            if (!favoriteList.getOfferIds().contains(offer.getId())) {
                favoriteList.getOfferIds().add(offer.getId());
            }
        } else {
            // Remove from favorites
            favoriteList.getOfferIds().remove(offer.getId());
        }

        firestore.getDb().collection("favorites")
                .document(favoriteList.getId())
                .set(favoriteList)
                .addOnSuccessListener(aVoid -> {
                    String message = add ? "Added to favorites" : "Removed from favorites";
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    propertyAdapter.updateFavorites(favoriteList.getOfferIds());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating favorites: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}