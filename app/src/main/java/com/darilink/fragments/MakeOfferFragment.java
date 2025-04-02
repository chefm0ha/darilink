package com.darilink.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.darilink.R;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.Offer;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MakeOfferFragment extends Fragment {

    private static final String TAG = "MakeOfferFragment";
    private static final String ARG_OFFER_ID = "offer_id";

    private LinearLayout imageContainer;
    private CardView addImageCard;
    private TextInputEditText titleInput, areaInput, rentInput, bedroomsInput, bathroomsInput,
            floorNumberInput, addressInput, descriptionInput;
    private AutoCompleteTextView propertyTypeInput, countryInput, cityInput;
    private ChipGroup amenitiesChipGroup;
    private Chip addAmenityChip;
    private SwitchMaterial availabilitySwitch;
    private Button saveOfferButton;

    private Firestore firestore;
    private Firebase firebase;

    private List<Uri> selectedImages = new ArrayList<>();
    private List<String> uploadedImageUrls = new ArrayList<>();
    private List<String> selectedAmenities = new ArrayList<>();
    private Map<String, List<String>> countryCityMap;

    private Offer currentOffer; // For editing existing offers
    private boolean isEditMode = false;
    private String offerId;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public MakeOfferFragment() {
        // Required empty public constructor
    }

    public static MakeOfferFragment newInstance(String offerId) {
        MakeOfferFragment fragment = new MakeOfferFragment();
        Bundle args = new Bundle();
        if (offerId != null) {
            args.putString(ARG_OFFER_ID, offerId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase
        firestore = Firestore.getInstance();
        firebase = Firebase.getInstance();

        if (getArguments() != null && getArguments().containsKey(ARG_OFFER_ID)) {
            offerId = getArguments().getString(ARG_OFFER_ID);
            isEditMode = offerId != null && !offerId.isEmpty();
        }

        // Initialize the image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        if (selectedImage != null) {
                            selectedImages.add(selectedImage);
                            addImageView(selectedImage);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_make_offer, container, false);

        // Set the title in the activity's action bar
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(isEditMode ? R.string.edit_listing : R.string.create_new_listing);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupListeners();
        loadDropdownData();

        // Check if editing an existing offer
        if (isEditMode) {
            loadOfferData(offerId);
        }
    }

    private void initializeViews(View view) {
        imageContainer = view.findViewById(R.id.imageContainer);
        addImageCard = view.findViewById(R.id.addImageCard);
        titleInput = view.findViewById(R.id.titleInput);
        propertyTypeInput = view.findViewById(R.id.propertyTypeInput);
        areaInput = view.findViewById(R.id.areaInput);
        rentInput = view.findViewById(R.id.rentInput);
        bedroomsInput = view.findViewById(R.id.bedroomsInput);
        bathroomsInput = view.findViewById(R.id.bathroomsInput);
        floorNumberInput = view.findViewById(R.id.floorNumberInput);
        addressInput = view.findViewById(R.id.addressInput);
        countryInput = view.findViewById(R.id.countryInput);
        cityInput = view.findViewById(R.id.cityInput);
        descriptionInput = view.findViewById(R.id.descriptionInput);
        amenitiesChipGroup = view.findViewById(R.id.amenitiesChipGroup);
        addAmenityChip = view.findViewById(R.id.addAmenityChip);
        availabilitySwitch = view.findViewById(R.id.availabilitySwitch);
        saveOfferButton = view.findViewById(R.id.saveOfferButton);
    }

    private void setupListeners() {
        addImageCard.setOnClickListener(v -> openImagePicker());

        addAmenityChip.setOnClickListener(v -> showAddAmenityDialog());

        // Country selection updates city dropdown
        countryInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCountry = countryInput.getText().toString();
            loadCities(selectedCountry);
        });

        // Show dropdown on focus
        countryInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                countryInput.showDropDown();
            }
        });

        cityInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                cityInput.showDropDown();
            }
        });

        propertyTypeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                propertyTypeInput.showDropDown();
            }
        });

        saveOfferButton.setOnClickListener(v -> {
            if (validateInputs()) {
                if (selectedImages.isEmpty() && !isEditMode) {
                    Toast.makeText(getContext(), "Please add at least one image", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveOffer();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void addImageView(Uri imageUri) {
        // Inflate image item view
        View imageItemView = LayoutInflater.from(getContext()).inflate(R.layout.item_property_image, imageContainer, false);

        // Find views in the inflated layout
        ImageView imageView = imageItemView.findViewById(R.id.propertyImage);
        ImageView deleteButton = imageItemView.findViewById(R.id.deleteImageButton);

        // Load image with Glide
        Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .into(imageView);

        // Set delete button click listener
        deleteButton.setOnClickListener(v -> {
            imageContainer.removeView(imageItemView);
            selectedImages.remove(imageUri);
        });

        // Add to container before the add image card
        imageContainer.addView(imageItemView, imageContainer.getChildCount() - 1);
    }

    private void displayExistingImage(String imageUrl) {
        // Inflate image item view
        View imageItemView = LayoutInflater.from(getContext()).inflate(R.layout.item_property_image, imageContainer, false);

        // Find views in the inflated layout
        ImageView imageView = imageItemView.findViewById(R.id.propertyImage);
        ImageView deleteButton = imageItemView.findViewById(R.id.deleteImageButton);

        // Load image with Glide
        Glide.with(this)
                .load(imageUrl)
                .centerCrop()
                .into(imageView);

        // Tag the view with the URL for identification
        imageItemView.setTag(imageUrl);

        // Set delete button click listener
        deleteButton.setOnClickListener(v -> {
            imageContainer.removeView(imageItemView);
            uploadedImageUrls.remove(imageUrl);
        });

        // Add to container before the add image card
        imageContainer.addView(imageItemView, imageContainer.getChildCount() - 1);
    }

    private void loadDropdownData() {
        // Property types
        String[] propertyTypes = {"Apartment", "Studio", "Duplex"};
        ArrayAdapter<String> propertyTypeAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, propertyTypes);
        propertyTypeInput.setAdapter(propertyTypeAdapter);

        // Load countries
        loadCountries();
    }

    private void loadCountries() {
        countryCityMap = new HashMap<>();
        List<String> countryList = new ArrayList<>();

        try {
            InputStream inputStream = getResources().openRawResource(R.raw.countries);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(json);
            JSONArray countriesArray = jsonObject.getJSONArray("countries");

            for (int i = 0; i < countriesArray.length(); i++) {
                JSONObject countryObject = countriesArray.getJSONObject(i);
                String countryName = countryObject.getString("name");

                JSONArray citiesArray = countryObject.getJSONArray("cities");
                List<String> cityList = new ArrayList<>();
                for (int j = 0; j < citiesArray.length(); j++) {
                    cityList.add(citiesArray.getString(j));
                }

                countryCityMap.put(countryName, cityList);
                countryList.add(countryName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading countries", e);
        }

        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, countryList);
        countryInput.setAdapter(countryAdapter);
    }

    private void loadCities(String selectedCountry) {
        List<String> cities = countryCityMap.get(selectedCountry);
        if (cities != null) {
            ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, cities);
            cityInput.setAdapter(cityAdapter);
        }
    }

    private void showAddAmenityDialog() {
        // Show dialog to add custom amenity
        EditText input = new EditText(getContext());
        input.setHint(R.string.amenity_name);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_amenity)
                .setView(input)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String amenityName = input.getText().toString().trim();
                    if (!amenityName.isEmpty()) {
                        addCustomAmenity(amenityName);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void addCustomAmenity(String amenityName) {
        Chip chip = new Chip(requireContext());
        chip.setText(amenityName);
        chip.setCheckable(true);
        chip.setChecked(true);

        // Add before the "Add More" chip
        amenitiesChipGroup.addView(chip, amenitiesChipGroup.getChildCount() - 1);
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate title
        if (titleInput.getText().toString().trim().isEmpty()) {
            titleInput.setError("Title is required");
            isValid = false;
        }

        // Validate property type
        if (propertyTypeInput.getText().toString().trim().isEmpty()) {
            propertyTypeInput.setError("Property type is required");
            isValid = false;
        }

        // Validate area
        String areaStr = areaInput.getText().toString().trim();
        if (areaStr.isEmpty()) {
            areaInput.setError("Area is required");
            isValid = false;
        } else {
            try {
                double area = Double.parseDouble(areaStr);
                if (area <= 0) {
                    areaInput.setError("Area must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                areaInput.setError("Invalid area value");
                isValid = false;
            }
        }

        // Validate rent
        String rentStr = rentInput.getText().toString().trim();
        if (rentStr.isEmpty()) {
            rentInput.setError("Rent is required");
            isValid = false;
        } else {
            try {
                double rent = Double.parseDouble(rentStr);
                if (rent <= 0) {
                    rentInput.setError("Rent must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                rentInput.setError("Invalid rent value");
                isValid = false;
            }
        }

        // Validate bedrooms
        String bedroomsStr = bedroomsInput.getText().toString().trim();
        if (bedroomsStr.isEmpty()) {
            bedroomsInput.setError("Number of bedrooms is required");
            isValid = false;
        }

        // Validate bathrooms
        String bathroomsStr = bathroomsInput.getText().toString().trim();
        if (bathroomsStr.isEmpty()) {
            bathroomsInput.setError("Number of bathrooms is required");
            isValid = false;
        }

        // Validate floor number
        String floorStr = floorNumberInput.getText().toString().trim();
        if (floorStr.isEmpty()) {
            floorNumberInput.setError("Floor number is required");
            isValid = false;
        }

        // Validate address
        if (addressInput.getText().toString().trim().isEmpty()) {
            addressInput.setError("Address is required");
            isValid = false;
        }

        // Validate country
        if (countryInput.getText().toString().trim().isEmpty()) {
            countryInput.setError("Country is required");
            isValid = false;
        }

        // Validate city
        if (cityInput.getText().toString().trim().isEmpty()) {
            cityInput.setError("City is required");
            isValid = false;
        }

        // Validate description
        if (descriptionInput.getText().toString().trim().isEmpty()) {
            descriptionInput.setError("Description is required");
            isValid = false;
        }

        return isValid;
    }

    private void saveOffer() {
        // Show loading indicator
        saveOfferButton.setEnabled(false);
        saveOfferButton.setText(R.string.saving);

        if (isEditMode && currentOffer != null) {
            updateExistingOffer();
        } else {
            createNewOffer();
        }
    }

    private void createNewOffer() {
        // Upload images first if there are any
        if (!selectedImages.isEmpty()) {
            uploadImages();
        } else {
            // If no new images, create offer with existing data
            createOfferInFirestore();
        }
    }

    private void uploadImages() {
        uploadedImageUrls.clear();
        final int totalImages = selectedImages.size();
        final int[] uploadedCount = {0};
        final int[] errorCount = {0};

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Uploading Images");
        progressDialog.setMessage("Please wait while we upload your images...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        for (Uri imageUri : selectedImages) {
            String requestId = MediaManager.get().upload(imageUri)
                    .option("folder", "darilink_properties")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            // Upload started
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            // Upload progress
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            // Get the secure URL
                            String imageUrl = resultData.get("secure_url").toString();
                            uploadedImageUrls.add(imageUrl);
                            uploadedCount[0]++;

                            checkIfAllUploaded(uploadedCount[0], errorCount[0], totalImages, progressDialog);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            errorCount[0]++;
                            Log.e(TAG, "Upload error: " + error.getDescription());
                            if (getContext() != null) {
                                Toast.makeText(getContext(),
                                        "Error uploading image: " + error.getDescription(),
                                        Toast.LENGTH_SHORT).show();
                            }

                            checkIfAllUploaded(uploadedCount[0], errorCount[0], totalImages, progressDialog);
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            // Upload rescheduled
                        }
                    })
                    .dispatch();
        }
    }

    private void checkIfAllUploaded(int uploadedCount, int errorCount, int totalImages, ProgressDialog progressDialog) {
        if (uploadedCount + errorCount == totalImages) {
            progressDialog.dismiss();

            if (uploadedCount > 0) {
                // Create or update the offer
                if (isEditMode && currentOffer != null) {
                    updateOfferInFirestore();
                } else {
                    createOfferInFirestore();
                }
            } else {
                // All uploads failed
                saveOfferButton.setEnabled(true);
                saveOfferButton.setText(R.string.save_listing);
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Failed to upload images. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void createOfferInFirestore() {
        FirebaseUser currentUser = firebase.getCurrentUser();
        if (currentUser == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "You must be logged in", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Offer offer = new Offer();
        populateOfferFromInputs(offer);
        offer.setAgentId(currentUser.getUid());
        offer.setCreatedAt(System.currentTimeMillis());
        offer.setUpdatedAt(System.currentTimeMillis());
        offer.setImages(uploadedImageUrls);

        firestore.addOffer(offer);

        saveOfferButton.setEnabled(true);
        if (getContext() != null) {
            Toast.makeText(getContext(), "Property listing saved successfully", Toast.LENGTH_SHORT).show();
        }

        // Navigate back to the previous fragment
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void updateExistingOffer() {
        if (currentOffer == null) return;

        // Handle image changes if needed
        if (!selectedImages.isEmpty()) {
            uploadNewImages();
        } else {
            // If no new images, update offer with existing data
            updateOfferInFirestore();
        }
    }

    private void uploadNewImages() {
        List<Uri> newImagesToUpload = new ArrayList<>(selectedImages);

        // First, gather existing images from the offer
        List<String> existingImages = new ArrayList<>();
        if (currentOffer.getImages() != null) {
            existingImages.addAll(currentOffer.getImages());
        }

        // Then upload new images
        if (!newImagesToUpload.isEmpty()) {
            final int totalImages = newImagesToUpload.size();
            final int[] uploadedCount = {0};
            final int[] errorCount = {0};

            // Show progress dialog
            ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setTitle("Uploading Images");
            progressDialog.setMessage("Please wait while we upload your new images...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            for (Uri imageUri : newImagesToUpload) {
                String requestId = MediaManager.get().upload(imageUri)
                        .option("folder", "darilink_properties")
                        .callback(new UploadCallback() {
                            @Override
                            public void onStart(String requestId) {
                                // Upload started
                            }

                            @Override
                            public void onProgress(String requestId, long bytes, long totalBytes) {
                                // Upload progress
                            }

                            @Override
                            public void onSuccess(String requestId, Map resultData) {
                                // Get the secure URL
                                String imageUrl = resultData.get("secure_url").toString();
                                existingImages.add(imageUrl);
                                uploadedCount[0]++;

                                if (uploadedCount[0] + errorCount[0] == totalImages) {
                                    progressDialog.dismiss();
                                    // Update the offer with combined images
                                    uploadedImageUrls = existingImages;
                                    updateOfferInFirestore();
                                }
                            }

                            @Override
                            public void onError(String requestId, ErrorInfo error) {
                                errorCount[0]++;
                                Log.e(TAG, "Upload error: " + error.getDescription());
                                if (getContext() != null) {
                                    Toast.makeText(getContext(),
                                            "Error uploading image: " + error.getDescription(),
                                            Toast.LENGTH_SHORT).show();
                                }

                                if (uploadedCount[0] + errorCount[0] == totalImages) {
                                    progressDialog.dismiss();
                                    if (uploadedCount[0] > 0 || !existingImages.isEmpty()) {
                                        // Update the offer with successfully uploaded images
                                        uploadedImageUrls = existingImages;
                                        updateOfferInFirestore();
                                    } else {
                                        // All uploads failed and no existing images
                                        saveOfferButton.setEnabled(true);
                                        saveOfferButton.setText(R.string.save_listing);
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(),
                                                    "Failed to upload images. Please try again.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onReschedule(String requestId, ErrorInfo error) {
                                // Upload rescheduled
                            }
                        })
                        .dispatch();
            }
        } else {
            // No new images to upload, just update the offer with existing images
            uploadedImageUrls = existingImages;
            updateOfferInFirestore();
        }
    }

    private void updateOfferInFirestore() {
        populateOfferFromInputs(currentOffer);
        currentOffer.setUpdatedAt(System.currentTimeMillis());
        currentOffer.setImages(uploadedImageUrls);

        firestore.updateOffer(currentOffer);

        saveOfferButton.setEnabled(true);
        saveOfferButton.setText(R.string.save_listing);
        if (getContext() != null) {
            Toast.makeText(getContext(), "Property listing updated successfully", Toast.LENGTH_SHORT).show();
        }

        // Navigate back to the previous fragment
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void populateOfferFromInputs(Offer offer) {
        offer.setTitle(titleInput.getText().toString().trim());
        offer.setDescription(descriptionInput.getText().toString().trim());
        offer.setPropertyType(propertyTypeInput.getText().toString().trim());
        offer.setArea(Double.parseDouble(areaInput.getText().toString().trim()));
        offer.setRent(Double.parseDouble(rentInput.getText().toString().trim()));
        offer.setNumBedrooms(Integer.parseInt(bedroomsInput.getText().toString().trim()));
        offer.setNumBathrooms(Integer.parseInt(bathroomsInput.getText().toString().trim()));
        offer.setFloorNumber(Integer.parseInt(floorNumberInput.getText().toString().trim()));
        offer.setAddress(addressInput.getText().toString().trim());
        offer.setCountry(countryInput.getText().toString().trim());
        offer.setCity(cityInput.getText().toString().trim());
        offer.setAvailable(availabilitySwitch.isChecked());

        // Get selected amenities
        List<String> amenities = new ArrayList<>();
        for (int i = 0; i < amenitiesChipGroup.getChildCount(); i++) {
            View view = amenitiesChipGroup.getChildAt(i);
            if (view instanceof Chip && view.getId() != R.id.addAmenityChip) {
                Chip chip = (Chip) view;
                if (chip.isChecked()) {
                    amenities.add(chip.getText().toString());
                }
            }
        }
        offer.setAmenities(amenities);
    }

    private void loadOfferData(String offerId) {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Loading Property");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Fetch offer data from Firestore
        firestore.getDb().collection("Offer").document(offerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressDialog.dismiss();
                    if (documentSnapshot.exists()) {
                        currentOffer = documentSnapshot.toObject(Offer.class);
                        if (currentOffer != null) {
                            populateFieldsFromOffer();
                        }
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Offer not found", Toast.LENGTH_SHORT).show();
                        }
                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().popBackStack();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading offer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                });
    }

    private void populateFieldsFromOffer() {
        titleInput.setText(currentOffer.getTitle());
        propertyTypeInput.setText(currentOffer.getPropertyType());
        areaInput.setText(String.valueOf(currentOffer.getArea()));
        rentInput.setText(String.valueOf(currentOffer.getRent()));
        bedroomsInput.setText(String.valueOf(currentOffer.getNumBedrooms()));
        bathroomsInput.setText(String.valueOf(currentOffer.getNumBathrooms()));
        floorNumberInput.setText(String.valueOf(currentOffer.getFloorNumber()));
        addressInput.setText(currentOffer.getAddress());
        countryInput.setText(currentOffer.getCountry());
        loadCities(currentOffer.getCountry());
        cityInput.setText(currentOffer.getCity());
        descriptionInput.setText(currentOffer.getDescription());
        availabilitySwitch.setChecked(currentOffer.isAvailable());

        // Load images
        uploadedImageUrls.clear();
        if (currentOffer.getImages() != null) {
            uploadedImageUrls.addAll(currentOffer.getImages());
            for (String imageUrl : uploadedImageUrls) {
                displayExistingImage(imageUrl);
            }
        }

        // Clear existing amenities first (keep only the Add More chip)
        for (int i = amenitiesChipGroup.getChildCount() - 1; i >= 0; i--) {
            View view = amenitiesChipGroup.getChildAt(i);
            if (view instanceof Chip && view.getId() != R.id.addAmenityChip) {
                amenitiesChipGroup.removeView(view);
            }
        }

        // Now load the amenities from the offer
        if (currentOffer.getAmenities() != null) {
            for (String amenity : currentOffer.getAmenities()) {
                // Check if this is a predefined amenity
                boolean isPredefined = false;
                int[] predefinedChipIds = {R.id.wifiChip, R.id.parkingChip, R.id.poolChip,
                        R.id.gymChip, R.id.acChip, R.id.furnishedChip};

                for (int chipId : predefinedChipIds) {
                    Chip chip = amenitiesChipGroup.findViewById(chipId);
                    if (chip != null && chip.getText().toString().equals(amenity)) {
                        chip.setChecked(true);
                        isPredefined = true;
                        break;
                    }
                }

                // If not predefined, add it as a custom amenity
                if (!isPredefined) {
                    addCustomAmenity(amenity);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up any resources if needed
    }
}