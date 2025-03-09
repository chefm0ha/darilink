package com.darilink.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.darilink.R;
import com.darilink.dataAccess.Firebase;
import com.darilink.models.Agent;
import com.darilink.models.Client;
import com.darilink.models.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    // UI Components - Step 1
    private LinearLayout stepOneLayout, stepTwoLayout, stepThreeLayout;
    private RadioGroup userTypeGroup;
    private RadioButton clientRadio, agentRadio;
    private EditText emailInput, passwordInput, reenterPasswordInput;
    private Button nextToStepTwoButton;

    // UI Components - Step 2
    private EditText firstNameInput, lastNameInput,
            addressInput, phoneInput;
    private AutoCompleteTextView countryInput, cityInput;
    private Button backToStepOneButton, nextToStepThreeButton;

    // UI Components - Step 3 (Agent specific)
    private EditText agencyNameInput, agencyAddressInput, agencyEmailInput, agencyPhoneInput;
    private AutoCompleteTextView agencyCountryInput, agencyCityInput;
    private Button backToStepTwoButton, finishSignUpButton;

    private Firebase firebase;

    private final Calendar calendar = Calendar.getInstance();
    private boolean isAgent = false;

    private Map<String, List<String>> countryCityMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        firebase = Firebase.getInstance();


        initializeViews();
        loadCountries();
        setupListeners();
    }

    private void initializeViews() {
        // Initialize layouts
        stepOneLayout = findViewById(R.id.stepOneLayout);
        stepTwoLayout = findViewById(R.id.stepTwoLayout);
        stepThreeLayout = findViewById(R.id.stepThreeLayout);

        // Initialize Step 1 views
        userTypeGroup = findViewById(R.id.userTypeGroup);
        clientRadio = findViewById(R.id.clientRadio);
        agentRadio = findViewById(R.id.agentRadio);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        reenterPasswordInput = findViewById(R.id.reenterPasswordInput);
        nextToStepTwoButton = findViewById(R.id.nextToStepTwoButton);

        // Initialize Step 2 views
        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        countryInput = findViewById(R.id.countryInput);
        cityInput = findViewById(R.id.cityInput);
        addressInput = findViewById(R.id.addressInput);
        phoneInput = findViewById(R.id.phoneInput);
        backToStepOneButton = findViewById(R.id.backToStepOneButton);
        nextToStepThreeButton = findViewById(R.id.nextToStepThreeButton);

        // Initialize Step 3 views (Agent specific)
        agencyNameInput = findViewById(R.id.agencyNameInput);
        agencyAddressInput = findViewById(R.id.agencyAddressInput);
        agencyCountryInput = findViewById(R.id.agencyCountryInput);
        agencyCityInput = findViewById(R.id.agencyCityInput);
        agencyEmailInput = findViewById(R.id.agencyEmailInput);
        agencyPhoneInput = findViewById(R.id.agencyPhoneInput);
        backToStepTwoButton = findViewById(R.id.backToStepTwoButton);
        finishSignUpButton = findViewById(R.id.finishSignUpButton);
    }

    private void setupListeners() {
        userTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isAgent = checkedId == R.id.agentRadio;
        });

        countryInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) countryInput.showDropDown();
        });

        cityInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) cityInput.showDropDown();
        });

        nextToStepTwoButton.setOnClickListener(v -> {
            if (validateStepOne()) {
                nextToStepThreeButton.setText(!isAgent ? "Sign up" : "Next");
                stepOneLayout.setVisibility(View.GONE);
                stepTwoLayout.setVisibility(View.VISIBLE);
            }
        });

        backToStepOneButton.setOnClickListener(v -> {
            stepTwoLayout.setVisibility(View.GONE);
            stepOneLayout.setVisibility(View.VISIBLE);
        });

        agencyCountryInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) agencyCountryInput.showDropDown();
        });

        agencyCityInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) agencyCityInput.showDropDown();
        });

        nextToStepThreeButton.setOnClickListener(v -> {
            if (validateStepTwo()) {
                if (isAgent) {
                    stepTwoLayout.setVisibility(View.GONE);
                    stepThreeLayout.setVisibility(View.VISIBLE);
                } else {
                    createUser();
                    Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_LONG).show();
                    // Finish current activity and go back to login
                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        });

        backToStepTwoButton.setOnClickListener(v -> {
            stepThreeLayout.setVisibility(View.GONE);
            stepTwoLayout.setVisibility(View.VISIBLE);
        });

        finishSignUpButton.setOnClickListener(v -> {
            if (validateStepThree()) {
                createUser();
            }
        });
    }

    private boolean validateStepOne() {
        if (userTypeGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select user type", Toast.LENGTH_SHORT).show();
            return false;
        }
        String email = emailInput.getText().toString().trim();
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return false;
        }
        if (!email.matches(emailPattern)) {
            emailInput.setError("Please enter a valid email address");
            return false;
        }
        if (passwordInput.getText().toString().trim().isEmpty()) {
            passwordInput.setError("Password is required");
            return false;
        }
        if (passwordInput.getText().toString().length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return false;
        }
        if (reenterPasswordInput.getText().toString().trim().isEmpty()) {
            reenterPasswordInput.setError("Re-enter password is required");
            return false;
        }
        if (!passwordInput.getText().toString().equals(reenterPasswordInput.getText().toString())) {
            reenterPasswordInput.setError("Passwords do not match");
            return false;
        }
        return true;
    }

    private boolean validateStepTwo() {
        if (firstNameInput.getText().toString().trim().isEmpty()) {
            firstNameInput.setError("First name is required");
            return false;
        }
        if (lastNameInput.getText().toString().trim().isEmpty()) {
            lastNameInput.setError("Last name is required");
            return false;
        }
        if (countryInput.getText().toString().trim().isEmpty()) {
            countryInput.setError("Country is required");
            return false;
        }
        if (cityInput.getText().toString().trim().isEmpty()) {
            cityInput.setError("City is required");
            return false;
        }
        if (addressInput.getText().toString().trim().isEmpty()) {
            addressInput.setError("Address is required");
            return false;
        }
        if (phoneInput.getText().toString().trim().isEmpty()) {
            phoneInput.setError("Phone is required");
            return false;
        }
        return true;
    }

    private boolean validateStepThree() {
        if (agencyNameInput.getText().toString().trim().isEmpty()) {
            agencyNameInput.setError("Agency name is required");
            return false;
        }
        if (agencyAddressInput.getText().toString().trim().isEmpty()) {
            agencyAddressInput.setError("Agency address is required");
            return false;
        }
        if (agencyCountryInput.getText().toString().trim().isEmpty()) {
            agencyCountryInput.setError("Agency country is required");
            return false;
        }
        if (agencyCityInput.getText().toString().trim().isEmpty()) {
            agencyCityInput.setError("Agency city is required");
            return false;
        }
        String email = agencyEmailInput.getText().toString().trim();
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return false;
        }
        if (!email.matches(emailPattern)) {
            agencyEmailInput.setError("Please enter a valid email address");
            return false;
        }
        if (agencyPhoneInput.getText().toString().trim().isEmpty()) {
            agencyPhoneInput.setError("Agency phone is required");
            return false;
        }
        return true;
    }

    private void createUser() {
        if (isAgent) {
            Agent agent = new Agent();
            setAgentData(agent); // Set common user data + agent-specific data
            firebase.createUserWithEmailAndPassword(agent);
        } else {
            Client client = new Client();
            setClientData(client); // Set common user data
            firebase.createUserWithEmailAndPassword(client);
        }
    }

    private void setUserData(@NonNull User user) {
        user.setEmail(emailInput.getText().toString().trim());
        user.setPassword(passwordInput.getText().toString().trim());
        user.setFirstName(firstNameInput.getText().toString().trim());
        user.setLastName(lastNameInput.getText().toString().trim());
        user.setCountry(countryInput.getText().toString().trim());
        user.setCity(cityInput.getText().toString().trim());
        user.setAddress(addressInput.getText().toString().trim());
        user.setPhone(phoneInput.getText().toString().trim());
    }

    private void setClientData(@NonNull Client client) {
        setUserData(client); // Set common user data
        // No additional attributes needed for Client
    }

    private void setAgentData(@NonNull Agent agent) {
        agent.setAgencyName(agencyNameInput.getText().toString().trim());
        agent.setAgencyAddress(agencyAddressInput.getText().toString().trim());
        agent.setAgencyCountry(agencyCountryInput.getText().toString().trim());
        agent.setAgencyCity(agencyCityInput.getText().toString().trim());
        agent.setAgencyEmail(agencyEmailInput.getText().toString().trim());
        agent.setAgencyPhone(agencyPhoneInput.getText().toString().trim());
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
            Log.e("SignUpActivity", "Error loading countries", e);
        }

        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, countryList);
        countryInput.setAdapter(countryAdapter);
        agencyCountryInput.setAdapter(countryAdapter);

        countryInput.setOnItemClickListener((parent, view, position, id) -> loadCities(countryList.get(position)));
        agencyCountryInput.setOnItemClickListener((parent, view, position, id) -> loadCities(countryList.get(position)));
    }

    private void loadCities(String selectedCountry) {
        List<String> cities = countryCityMap.get(selectedCountry);
        if (cities != null) {
            ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities);
            cityInput.setAdapter(cityAdapter);
            agencyCityInput.setAdapter(cityAdapter);
        }
    }
}