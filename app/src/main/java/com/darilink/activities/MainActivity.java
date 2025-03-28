package com.darilink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.darilink.R;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.Agent;
import com.darilink.models.Client;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Firebase firebase;
    private Firestore firestore;

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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            checkUserTypeAndUpdateUI(uid);
        }
    }

    private void checkUserTypeAndUpdateUI(String uid) {
        // Check in agents collection
        firestore.getDb().collection("agents").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User is an agent
                        setupAgentNavigation(documentSnapshot.toObject(Agent.class));
                    } else {
                        // Check in clients collection
                        firestore.getDb().collection("clients").document(uid).get()
                                .addOnSuccessListener(clientSnapshot -> {
                                    if (clientSnapshot.exists()) {
                                        // User is a client
                                        setupClientNavigation(clientSnapshot.toObject(Client.class));
                                    }
                                });
                    }
                });
    }

    private void setupAgentNavigation(Agent agent) {
        // Set agent menu
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.nav_menu_agent);

        // Update header
        View headerView = navigationView.getHeaderView(0);
        TextView userName = headerView.findViewById(R.id.nav_user_name);
        TextView userEmail = headerView.findViewById(R.id.nav_user_email);
        ImageView userImage = headerView.findViewById(R.id.nav_user_image);

        userName.setText(agent.getFirstName() + " " + agent.getLastName());
        userEmail.setText(agent.getEmail());
        // TODO: Set agent image if available
    }

    private void setupClientNavigation(Client client) {
        // Set client menu
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.nav_menu_client);

        // Update header
        View headerView = navigationView.getHeaderView(0);
        TextView userName = headerView.findViewById(R.id.nav_user_name);
        TextView userEmail = headerView.findViewById(R.id.nav_user_email);
        ImageView userImage = headerView.findViewById(R.id.nav_user_image);

        userName.setText(client.getFirstName() + " " + client.getLastName());
        userEmail.setText(client.getEmail());
        // TODO: Set client image if available
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            firebase.signOut();
            // Navigate to login
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        // Handle other menu items based on user type
        // TODO: Implement navigation to other screens

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}