package com.darilink.dataAccess;

import com.darilink.dto.AgentDTO;
import com.darilink.dto.ClientDTO;
import com.google.firebase.firestore.FirebaseFirestore;
import com.darilink.models.Agent;
import com.darilink.models.Client;
import android.util.Log;

public class Firestore {
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
}