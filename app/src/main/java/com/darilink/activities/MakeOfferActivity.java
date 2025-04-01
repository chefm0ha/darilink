package com.darilink.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MakeOfferActivity extends AppCompatActivity {

    private static final String TAG = "MakeOfferActivity";

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

    private ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.make_offer);

        // Initialize Firebase
        firestore = Firestore.getInstance();
        firebase = Firebase.getInstance();

        setupToolbar();
        initializeViews();
        setupListeners();
        loadDropdownData();

        // Check if editing an existing offer
        if (getIntent().hasExtra("offer_id")) {
            isEditMode = true;
            String offerId = getIntent().getStringExtra("offer_id");
            loadOfferData(offerId);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? R.string.edit_listing : R.string.create_new_listing);
        }
    }

    private void initializeViews() {
        imageContainer = findViewById(R.id.imageContainer);
        addImageCard = findViewById(R.id.addImageCard);
        titleInput = findViewById(R.id.titleInput);
        propertyTypeInput = findViewById(R.id.propertyTypeInput);
        areaInput = findViewById(R.id.areaInput);
        rentInput = findViewById(R.id.rentInput);
        bedroomsInput = findViewById(R.id.bedroomsInput);
        bathroomsInput = findViewById(R.id.bathroomsInput);
        floorNumberInput = findViewById(R.id.floorNumberInput);
        addressInput = findViewById(R.id.addressInput);
        countryInput = findViewById(R.id.countryInput);
        cityInput = findViewById(R.id.cityInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        amenitiesChipGroup = findViewById(R.id.amenitiesChipGroup);
        addAmenityChip = findViewById(R.id.addAmenityChip);
        availabilitySwitch = findViewById(R.id.availabilitySwitch);
        saveOfferButton = findViewById(R.id.saveOfferButton);
    }

    private void setupListeners() {
        addImageCard.setOnClickListener(v -> openImagePicker());

        addAmenityChip.setOnClickListener(v -> showAddAmenityDialog());

        // Country selection updates city dropdown
        countryInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCountry = countryInput.getText().toString();
            loadCities(selectedCountry);
        });

        saveOfferButton.setOnClickListener(v -> {
            if (validateInputs()) {
                if (selectedImages.isEmpty() && !isEditMode) {
                    Toast.makeText(this, "Please add at least one image", Toast.LENGTH_SHORT).show();
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
        View imageItemView = LayoutInflater.from(this).inflate(R.layout.item_property_image, imageContainer, false);

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
        View imageItemView = LayoutInflater.from(this).inflate(R.layout.item_property_image, imageContainer, false);

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
        String[] propertyTypes = {"Apartment", "House", "Villa", "Studio", "Condo", "Penthouse", "Duplex"};
        ArrayAdapter<String> propertyTypeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, propertyTypes);
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

        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, countryList);
        countryInput.setAdapter(countryAdapter);
    }

    private void loadCities(String selectedCountry) {
        List<String> cities = countryCityMap.get(selectedCountry);
        if (cities != null) {
            ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities);
            cityInput.setAdapter(cityAdapter);
        }
    }

    private void showAddAmenityDialog() {
        // Show dialog to add custom amenity
        EditText input = new EditText(this);
        input.setHint(R.string.amenity_name);

        new MaterialAlertDialogBuilder(this)
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
        Chip chip = new Chip(this);
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
        ProgressDialog progressDialog = new ProgressDialog(this);
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
                            Toast.makeText(MakeOfferActivity.this,
                                    "Error uploading image: " + error.getDescription(),
                                    Toast.LENGTH_SHORT).show();

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
                Toast.makeText(MakeOfferActivity.this,
                        "Failed to upload images. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createOfferInFirestore() {
        FirebaseUser currentUser = firebase.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Property listing saved successfully", Toast.LENGTH_SHORT).show();
        finish();
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
            ProgressDialog progressDialog = new ProgressDialog(this);
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
                                Toast.makeText(MakeOfferActivity.this,
                                        "Error uploading image: " + error.getDescription(),
                                        Toast.LENGTH_SHORT).show();

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
                                        Toast.makeText(MakeOfferActivity.this,
                                                "Failed to upload images. Please try again.",
                                                Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Property listing updated successfully", Toast.LENGTH_SHORT).show();
        finish();
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
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading Property");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Fetch offer data from Firestore
        firestore.getDb().collection("offers").document(offerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressDialog.dismiss();
                    if (documentSnapshot.exists()) {
                        currentOffer = documentSnapshot.toObject(Offer.class);
                        if (currentOffer != null) {
                            populateFieldsFromOffer();
                        }
                    } else {
                        Toast.makeText(this, "Offer not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error loading offer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
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

        // Set amenities
        if (currentOffer.getAmenities() != null) {
            for (String amenity : currentOffer.getAmenities()) {
                // Find if this amenity is one of the predefined ones
                boolean isPredefined = false;
                for (int i = 0; i < amenitiesChipGroup.getChildCount(); i++) {
                    View view = amenitiesChipGroup.getChildAt(i);
                    if (view instanceof Chip && ((Chip) view).getText().toString().equals(amenity)) {
                        ((Chip) view).setChecked(true);
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isEditMode || !titleInput.getText().toString().trim().isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.discard_changes)
                    .setMessage(R.string.discard_changes_message)
                    .setPositiveButton(R.string.discard, (dialog, which) -> super.onBackPressed())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    // Helper method to extract public ID from Cloudinary URL if needed for deletion
    private String getPublicIdFromUrl(String cloudinaryUrl) {
        try {
            // Example URL: https://res.cloudinary.com/your-cloud-name/image/upload/v1234567890/darilink_properties/abcdef.jpg
            String[] urlParts = cloudinaryUrl.split("/");

            // Get the last two parts (folder/filename)
            String filename = urlParts[urlParts.length - 1];
            String folder = urlParts[urlParts.length - 2];

            // Combined folder and filename
            String filenameWithFolder = folder + "/" + filename;

            // Remove extension
            int dotIndex = filenameWithFolder.lastIndexOf('.');
            if (dotIndex != -1) {
                filenameWithFolder = filenameWithFolder.substring(0, dotIndex);
            }

            return filenameWithFolder;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting public ID: " + e.getMessage());
            return null;
        }
    }
}