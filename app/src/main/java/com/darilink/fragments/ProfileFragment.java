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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.darilink.R;
import com.darilink.activities.MainActivity;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.Agent;
import com.darilink.models.Client;
import com.darilink.models.Subscription;
import com.darilink.models.User;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    // UI Components - Common User Fields
    private de.hdodenhof.circleimageview.CircleImageView profileImageView;
    private TextView userTypeLabel;
    private EditText firstNameInput, lastNameInput, emailInput, phoneInput, addressInput;
    private AutoCompleteTextView countryInput, cityInput;
    private Button saveProfileButton, changePasswordButton;

    // UI Components - Agent Specific Fields
    private LinearLayout agencyInfoLayout;
    private EditText agencyNameInput, agencyAddressInput, agencyEmailInput, agencyPhoneInput;
    private AutoCompleteTextView agencyCountryInput, agencyCityInput;
    private TextView subscriptionInfoText;

    // Firebase
    private Firebase firebase;
    private Firestore firestore;
    private FirebaseUser currentUser;

    // User data
    private User user;
    private boolean isAgent = false;
    private Uri selectedImageUri = null;
    private String profileImageUrl = null;

    // Country-City data
    private Map<String, List<String>> countryCityMap;

    // Image picker
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        firebase = Firebase.getInstance();
        firestore = Firestore.getInstance();
        currentUser = firebase.getCurrentUser();

        // Initialize the image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            // Display the selected image
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .circleCrop()
                                    .into(profileImageView);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Set title in ActionBar
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(R.string.profile);
            }
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
        loadCountries();
        loadUserData();
    }

    private void initViews(View view) {
        // Common User Fields
        profileImageView = view.findViewById(R.id.profileImageView);
        userTypeLabel = view.findViewById(R.id.userTypeLabel);
        firstNameInput = view.findViewById(R.id.firstNameInput);
        lastNameInput = view.findViewById(R.id.lastNameInput);
        emailInput = view.findViewById(R.id.emailInput);
        countryInput = view.findViewById(R.id.countryInput);
        cityInput = view.findViewById(R.id.cityInput);
        addressInput = view.findViewById(R.id.addressInput);
        phoneInput = view.findViewById(R.id.phoneInput);
        saveProfileButton = view.findViewById(R.id.saveProfileButton);
        changePasswordButton = view.findViewById(R.id.changePasswordButton);

        // Agent Specific Fields
        agencyInfoLayout = view.findViewById(R.id.agencyInfoLayout);
        agencyNameInput = view.findViewById(R.id.agencyNameInput);
        agencyAddressInput = view.findViewById(R.id.agencyAddressInput);
        agencyCountryInput = view.findViewById(R.id.agencyCountryInput);
        agencyCityInput = view.findViewById(R.id.agencyCityInput);
        agencyEmailInput = view.findViewById(R.id.agencyEmailInput);
        agencyPhoneInput = view.findViewById(R.id.agencyPhoneInput);
        subscriptionInfoText = view.findViewById(R.id.subscriptionInfoText);
    }

    private void setupListeners() {
        // Profile image click
        profileImageView.setOnClickListener(v -> openImagePicker());

        // Country selection updates city dropdown
        countryInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCountry = countryInput.getText().toString();
            loadCities(cityInput, selectedCountry);
        });

        agencyCountryInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCountry = agencyCountryInput.getText().toString();
            loadCities(agencyCityInput, selectedCountry);
        });

        // Save profile button
        saveProfileButton.setOnClickListener(v -> {
            if (validateInputs()) {
                saveProfile();
            }
        });

        // Change password button
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());

        // Show dropdown on focus
        countryInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) countryInput.showDropDown();
        });

        cityInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) cityInput.showDropDown();
        });

        agencyCountryInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) agencyCountryInput.showDropDown();
        });

        agencyCityInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) agencyCityInput.showDropDown();
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
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

        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                countryList
        );
        countryInput.setAdapter(countryAdapter);
        agencyCountryInput.setAdapter(countryAdapter);
    }

    private void loadCities(AutoCompleteTextView cityInput, String selectedCountry) {
        List<String> cities = countryCityMap.get(selectedCountry);
        if (cities != null) {
            ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    cities
            );
            cityInput.setAdapter(cityAdapter);
        }
    }

    private void loadUserData() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Loading Profile");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String uid = currentUser.getUid();

        // First check if user is an agent
        firestore.getDb().collection("agents").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User is an agent
                        isAgent = true;
                        Agent agent = documentSnapshot.toObject(Agent.class);
                        user = agent;
                        populateAgentFields(agent);
                        progressDialog.dismiss();
                    } else {
                        // Check if user is a client
                        firestore.getDb().collection("clients").document(uid).get()
                                .addOnSuccessListener(clientSnapshot -> {
                                    if (clientSnapshot.exists()) {
                                        // User is a client
                                        isAgent = false;
                                        Client client = clientSnapshot.toObject(Client.class);
                                        user = client;
                                        populateClientFields(client);
                                    }
                                    progressDialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void populateCommonFields(User user) {
        // Email (set as readonly since it's used for login)
        emailInput.setText(user.getEmail());
        emailInput.setEnabled(false);

        // Basic info
        firstNameInput.setText(user.getFirstName());
        lastNameInput.setText(user.getLastName());
        phoneInput.setText(user.getPhone());
        addressInput.setText(user.getAddress());

        // Country and city
        countryInput.setText(user.getCountry());
        if (user.getCountry() != null && !user.getCountry().isEmpty()) {
            loadCities(cityInput, user.getCountry());
        }
        cityInput.setText(user.getCity());

        // Profile image (if available)
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            profileImageUrl = user.getProfileImageUrl();
            Glide.with(this)
                    .load(profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.default_profile)
                    .into(profileImageView);
        }
    }

    private void populateClientFields(Client client) {
        populateCommonFields(client);
        userTypeLabel.setText(R.string.client);
        agencyInfoLayout.setVisibility(View.GONE);
    }

    private void populateAgentFields(Agent agent) {
        populateCommonFields(agent);
        userTypeLabel.setText(R.string.agent);
        agencyInfoLayout.setVisibility(View.VISIBLE);

        // Agency info
        agencyNameInput.setText(agent.getAgencyName());
        agencyAddressInput.setText(agent.getAgencyAddress());
        agencyEmailInput.setText(agent.getAgencyEmail());
        agencyPhoneInput.setText(agent.getAgencyPhone());

        // Country and city
        agencyCountryInput.setText(agent.getAgencyCountry());
        if (agent.getAgencyCountry() != null && !agent.getAgencyCountry().isEmpty()) {
            loadCities(agencyCityInput, agent.getAgencyCountry());
        }
        agencyCityInput.setText(agent.getAgencyCity());

        // Subscription info
        Subscription subscription = agent.getSubscription();
        if (subscription != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
            String startDate = subscription.getDate().format(formatter);
            String subscriptionInfo = String.format(
                    "Subscription: %s\nStart date: %s\nDuration: %d months",
                    subscription.getType().getType(),
                    startDate,
                    subscription.getDuration()
            );
            subscriptionInfoText.setText(subscriptionInfo);
            subscriptionInfoText.setVisibility(View.VISIBLE);
        } else {
            subscriptionInfoText.setVisibility(View.GONE);
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // First Name
        if (firstNameInput.getText().toString().trim().isEmpty()) {
            firstNameInput.setError("First name is required");
            isValid = false;
        }

        // Last Name
        if (lastNameInput.getText().toString().trim().isEmpty()) {
            lastNameInput.setError("Last name is required");
            isValid = false;
        }

        // Phone
        if (phoneInput.getText().toString().trim().isEmpty()) {
            phoneInput.setError("Phone number is required");
            isValid = false;
        }

        // Country
        if (countryInput.getText().toString().trim().isEmpty()) {
            countryInput.setError("Country is required");
            isValid = false;
        }

        // City
        if (cityInput.getText().toString().trim().isEmpty()) {
            cityInput.setError("City is required");
            isValid = false;
        }

        // Address
        if (addressInput.getText().toString().trim().isEmpty()) {
            addressInput.setError("Address is required");
            isValid = false;
        }

        // Validate agent fields if user is an agent
        if (isAgent && agencyInfoLayout.getVisibility() == View.VISIBLE) {
            // Agency Name
            if (agencyNameInput.getText().toString().trim().isEmpty()) {
                agencyNameInput.setError("Agency name is required");
                isValid = false;
            }

            // Agency Address
            if (agencyAddressInput.getText().toString().trim().isEmpty()) {
                agencyAddressInput.setError("Agency address is required");
                isValid = false;
            }

            // Agency Country
            if (agencyCountryInput.getText().toString().trim().isEmpty()) {
                agencyCountryInput.setError("Agency country is required");
                isValid = false;
            }

            // Agency City
            if (agencyCityInput.getText().toString().trim().isEmpty()) {
                agencyCityInput.setError("Agency city is required");
                isValid = false;
            }

            // Agency Email
            String agencyEmail = agencyEmailInput.getText().toString().trim();
            String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
            if (agencyEmail.isEmpty()) {
                agencyEmailInput.setError("Agency email is required");
                isValid = false;
            } else if (!agencyEmail.matches(emailPattern)) {
                agencyEmailInput.setError("Please enter a valid email address");
                isValid = false;
            }

            // Agency Phone
            if (agencyPhoneInput.getText().toString().trim().isEmpty()) {
                agencyPhoneInput.setError("Agency phone is required");
                isValid = false;
            }
        }

        return isValid;
    }

    private void saveProfile() {
        // Show save progress
        saveProfileButton.setEnabled(false);
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Saving Profile");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // If there's a new profile image, upload it first
        if (selectedImageUri != null) {
            uploadProfileImage(progressDialog);
        } else {
            updateUserData(progressDialog);
        }
    }

    private void uploadProfileImage(ProgressDialog progressDialog) {
        MediaManager.get().upload(selectedImageUri)
                .option("folder", "darilink_profiles")
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
                        profileImageUrl = resultData.get("secure_url").toString();
                        updateUserData(progressDialog);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Upload error: " + error.getDescription());
                        progressDialog.dismiss();
                        saveProfileButton.setEnabled(true);

                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Error uploading profile image: " + error.getDescription(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Upload rescheduled
                    }
                })
                .dispatch();
    }

    private void updateUserData(ProgressDialog progressDialog) {
        if (currentUser == null) {
            progressDialog.dismiss();
            saveProfileButton.setEnabled(true);
            Toast.makeText(getContext(), "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        // Update user data based on type
        if (isAgent) {
            Agent agent = (Agent) user;
            updateAgentData(agent);

            firestore.getDb().collection("agents").document(uid)
                    .set(agent)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        saveProfileButton.setEnabled(true);
                        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();

                        // Refresh the navigation header with the updated profile
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).refreshNavigationHeader();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        saveProfileButton.setEnabled(true);
                        Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Client client = (Client) user;
            updateClientData(client);

            firestore.getDb().collection("clients").document(uid)
                    .set(client)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        saveProfileButton.setEnabled(true);
                        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();

                        // Refresh the navigation header with the updated profile
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).refreshNavigationHeader();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        saveProfileButton.setEnabled(true);
                        Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateCommonData(User user) {
        user.setFirstName(firstNameInput.getText().toString().trim());
        user.setLastName(lastNameInput.getText().toString().trim());
        user.setPhone(phoneInput.getText().toString().trim());
        user.setAddress(addressInput.getText().toString().trim());
        user.setCountry(countryInput.getText().toString().trim());
        user.setCity(cityInput.getText().toString().trim());
        user.setProfileImageUrl(profileImageUrl);
    }

    private void updateClientData(Client client) {
        updateCommonData(client);
        // Additional client-specific fields would go here
    }

    private void updateAgentData(Agent agent) {
        updateCommonData(agent);

        // Agency info
        agent.setAgencyName(agencyNameInput.getText().toString().trim());
        agent.setAgencyAddress(agencyAddressInput.getText().toString().trim());
        agent.setAgencyEmail(agencyEmailInput.getText().toString().trim());
        agent.setAgencyPhone(agencyPhoneInput.getText().toString().trim());
        agent.setAgencyCountry(agencyCountryInput.getText().toString().trim());
        agent.setAgencyCity(agencyCityInput.getText().toString().trim());

        // Subscription details remain unchanged from what was loaded
    }

    private void showChangePasswordDialog() {
        // Create dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);

        TextInputLayout currentPasswordLayout = dialogView.findViewById(R.id.currentPasswordLayout);
        TextInputLayout newPasswordLayout = dialogView.findViewById(R.id.newPasswordLayout);
        TextInputLayout confirmPasswordLayout = dialogView.findViewById(R.id.confirmPasswordLayout);

        EditText currentPasswordInput = currentPasswordLayout.getEditText();
        EditText newPasswordInput = newPasswordLayout.getEditText();
        EditText confirmPasswordInput = confirmPasswordLayout.getEditText();

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Change", null) // Set listener later to prevent automatic dismiss
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                // Clear previous errors
                currentPasswordLayout.setError(null);
                newPasswordLayout.setError(null);
                confirmPasswordLayout.setError(null);

                // Get input values
                String currentPassword = currentPasswordInput.getText().toString().trim();
                String newPassword = newPasswordInput.getText().toString().trim();
                String confirmPassword = confirmPasswordInput.getText().toString().trim();

                // Validate input
                boolean isValid = true;

                if (currentPassword.isEmpty()) {
                    currentPasswordLayout.setError("Current password is required");
                    isValid = false;
                }

                if (newPassword.isEmpty()) {
                    newPasswordLayout.setError("New password is required");
                    isValid = false;
                } else if (newPassword.length() < 6) {
                    newPasswordLayout.setError("Password must be at least 6 characters");
                    isValid = false;
                }

                if (confirmPassword.isEmpty()) {
                    confirmPasswordLayout.setError("Confirm your new password");
                    isValid = false;
                } else if (!newPassword.equals(confirmPassword)) {
                    confirmPasswordLayout.setError("Passwords do not match");
                    isValid = false;
                }

                if (isValid) {
                    changePassword(currentPassword, newPassword, dialog);
                }
            });
        });

        dialog.show();
    }

    private void changePassword(String currentPassword, String newPassword, AlertDialog dialog) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Changing Password");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Re-authenticate before changing password
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Authenticated, now change password
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid1 -> {
                                progressDialog.dismiss();
                                dialog.dismiss();
                                Toast.makeText(getContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "Failed to change password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}