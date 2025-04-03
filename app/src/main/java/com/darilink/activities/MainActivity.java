package com.darilink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.darilink.R;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.fragments.FavoritesFragment;
import com.darilink.fragments.MakeOfferFragment;
import com.darilink.fragments.MyPropertiesFragment;
import com.darilink.fragments.MyRequestsFragment;
import com.darilink.fragments.ProfileFragment;
import com.darilink.fragments.SearchPropertiesFragment;
import com.darilink.fragments.AgentRequestsFragment;
import com.darilink.models.Agent;
import com.darilink.models.Client;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Firebase firebase;
    private Firestore firestore;
    private boolean isAgent = false;

    // Nav header views
    private CircleImageView navUserImage;
    private TextView navUserName, navUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        firebase = Firebase.getInstance();
        firestore = Firestore.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Get nav header views
        View headerView = navigationView.getHeaderView(0);
        navUserName = headerView.findViewById(R.id.nav_user_name);
        navUserEmail = headerView.findViewById(R.id.nav_user_email);
        navUserImage = headerView.findViewById(R.id.nav_user_image);

        // Add burger icon
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set navigation item selected listener
        navigationView.setNavigationItemSelectedListener(this);

        // Get current user
        FirebaseUser currentUser = firebase.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            checkUserTypeAndUpdateUI(uid);
        } else {
            // No user logged in, go to login
            navigateToLogin();
        }
    }

    private void checkUserTypeAndUpdateUI(String uid) {
        // Check in agents collection
        firestore.getDb().collection("agents").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User is an agent
                        isAgent = true;
                        Agent agent = documentSnapshot.toObject(Agent.class);
                        setupAgentNavigation(agent);
                    } else {
                        // Check in clients collection
                        firestore.getDb().collection("clients").document(uid).get()
                                .addOnSuccessListener(clientSnapshot -> {
                                    if (clientSnapshot.exists()) {
                                        // User is a client
                                        isAgent = false;
                                        Client client = clientSnapshot.toObject(Client.class);
                                        setupClientNavigation(client);
                                    } else {
                                        // User not found in either collection
                                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                                        navigateToLogin();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    navigateToLogin();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                });
    }

    private void setupAgentNavigation(Agent agent) {
        // Set agent menu
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.nav_menu_agent);

        // Update header
        updateNavigationHeader(agent.getFirstName(), agent.getLastName(),
                agent.getEmail(), agent.getProfileImageUrl());

        // Load My Properties fragment by default for agents
        loadFragment(new MyPropertiesFragment());
    }

    private void setupClientNavigation(Client client) {
        // Set client menu
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.nav_menu_client);

        // Update header
        updateNavigationHeader(client.getFirstName(), client.getLastName(),
                client.getEmail(), client.getProfileImageUrl());

        // Load Search Properties fragment by default for clients
        loadFragment(new SearchPropertiesFragment());
    }

    private void updateNavigationHeader(String firstName, String lastName, String email, String profileImageUrl) {
        // Set user name and email
        String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        navUserName.setText(fullName);
        navUserEmail.setText(email);

        // Load profile image if available
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(navUserImage);
        } else {
            // Use default profile image
            navUserImage.setImageResource(R.drawable.default_profile);
        }

        // Make navigation header clickable to go to profile
        View headerView = navigationView.getHeaderView(0);
        headerView.setOnClickListener(v -> {
            // Load Profile fragment
            loadFragment(new ProfileFragment());
            drawerLayout.closeDrawer(GravityCompat.START);
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            firebase.signOut();
            // Navigate to login
            navigateToLogin();
            return true;
        } else if (id == R.id.nav_profile) {
            // Load Profile fragment
            loadFragment(new ProfileFragment());
        } else if (isAgent) {
            // Agent specific navigation
            if (id == R.id.nav_make_offer) {
                // Load Make Offer fragment
                loadFragment(MakeOfferFragment.newInstance(null));
            } else if (id == R.id.nav_my_properties) {
                // Load My Properties fragment
                loadFragment(new MyPropertiesFragment());
            } else if (id == R.id.nav_requests) {
                // Load Agent Requests fragment
                loadFragment(new AgentRequestsFragment());
            }
        } else {
            // Client specific navigation
            if (id == R.id.nav_search) {
                // Load Search Properties fragment
                loadFragment(new SearchPropertiesFragment());
            } else if (id == R.id.nav_favorites) {
                // Load Favorites fragment
                loadFragment(new FavoritesFragment());
            } else if (id == R.id.nav_my_requests) {
                // Load My Requests fragment
                loadFragment(new MyRequestsFragment());
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // Helper method to load fragments
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, fragment);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                super.onBackPressed();
            }
        }
    }

    // Navigate to login activity
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // Method to refresh navigation header (call this after profile updates)
    public void refreshNavigationHeader() {
        FirebaseUser currentUser = firebase.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            checkUserTypeAndUpdateUI(uid);
        }
    }
}