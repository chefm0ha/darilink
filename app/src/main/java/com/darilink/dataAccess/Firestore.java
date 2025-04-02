package com.darilink.dataAccess;

import com.darilink.dto.AgentDTO;
import com.darilink.dto.ClientDTO;
import com.darilink.dto.OfferDTO;
import com.darilink.models.Offer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.darilink.models.Agent;
import com.darilink.models.Client;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Firestore {
    private static final String OFFERS_COLLECTION = "Offer";
    private static Firestore instance;
    private final FirebaseFirestore db;
    private static final String AGENTS_COLLECTION = "agents";
    private static final String CLIENTS_COLLECTION = "clients";

    private Firestore() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized Firestore getInstance() {
        if (instance == null) {
            instance = new Firestore();
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public void addClient(String uid, Client client) {
        ClientDTO clientDTO = ClientDTO.fromClient(client);
        db.collection(CLIENTS_COLLECTION)
                .document(uid)
                .set(clientDTO)
                .addOnSuccessListener(unused -> Log.d("Firestore", "Client data stored successfully for UID: " + uid))
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to store client data for UID: " + uid, e));
    }

    public void addAgent(String uid, Agent agent) {
        AgentDTO agentDTO = AgentDTO.fromAgent(agent);
        db.collection(AGENTS_COLLECTION)
                .document(uid)
                .set(agentDTO)
                .addOnSuccessListener(unused -> Log.d("Firestore", "Agent data stored successfully for UID: " + uid))
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to store agent data for UID: " + uid, e));
    }

    public void updateOffer(Offer offer) {
        if (offer.getId() == null || offer.getId().isEmpty()) {
            Log.e("Firestore", "Cannot update offer without an ID");
            return;
        }

        OfferDTO offerDTO = OfferDTO.fromOffer(offer);
        db.collection(OFFERS_COLLECTION)
                .document(offer.getId())
                .set(offerDTO)
                .addOnSuccessListener(unused -> {
                    Log.d("Firestore", "Offer updated successfully with ID: " + offer.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to update offer with ID: " + offer.getId(), e);
                });
    }

    public void addOffer(Offer offer) {
        // Generate a new document ID if not already set
        String offerId = offer.getId();
        if (offerId == null || offerId.isEmpty()) {
            offerId = db.collection(OFFERS_COLLECTION).document().getId();
            offer.setId(offerId);
        }

        OfferDTO offerDTO = OfferDTO.fromOffer(offer);

        // Use the final offerId directly inside the lambda
        String finalOfferId = offerId;

        db.collection(OFFERS_COLLECTION)
                .document(finalOfferId)
                .set(offerDTO)
                .addOnSuccessListener(unused -> {
                    Log.d("Firestore", "Offer added successfully with ID: " + finalOfferId);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to add offer", e);
                });
    }

    public void getOffersByAgent(String agentId, FirestoreCallback<List<Offer>> callback) {
        if (agentId == null || agentId.isEmpty()) {
            callback.onFailure("Agent ID cannot be empty");
            return;
        }

        db.collection(OFFERS_COLLECTION)
                .whereEqualTo("agentId", agentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Offer> offers = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Offer offer = document.toObject(Offer.class);
                        if (offer != null) {
                            // Ensure ID is set (in case it wasn't stored in the object)
                            offer.setId(document.getId());
                            offers.add(offer);
                        }
                    }
                    Log.d("Firestore", "Retrieved " + offers.size() + " offers for agent: " + agentId);
                    callback.onSuccess(offers);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error getting offers by agent", e);
                    callback.onFailure(e.getMessage());
                });
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);

        void onFailure(String error);
    }
}