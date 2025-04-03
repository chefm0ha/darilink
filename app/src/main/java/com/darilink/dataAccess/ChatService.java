package com.darilink.dataAccess;

import android.util.Log;

import com.darilink.models.ChatMessage;
import com.darilink.models.ChatThread;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatService {
    private static final String TAG = "ChatService";
    private static final String CHAT_THREADS_COLLECTION = "chat_threads";
    private static final String CHAT_MESSAGES_COLLECTION = "chat_messages";

    private static ChatService instance;
    private final FirebaseFirestore db;

    private ChatService() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized ChatService getInstance() {
        if (instance == null) {
            instance = new ChatService();
        }
        return instance;
    }

    // Create a new chat thread for a property
    public void createChatThread(ChatThread chatThread, ChatServiceCallback<String> callback) {
        // Check if a thread already exists for this property and users
        db.collection(CHAT_THREADS_COLLECTION)
                .whereEqualTo("propertyId", chatThread.getPropertyId())
                .whereEqualTo("clientId", chatThread.getClientId())
                .whereEqualTo("agentId", chatThread.getAgentId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No existing thread, create a new one
                        db.collection(CHAT_THREADS_COLLECTION)
                                .add(chatThread)
                                .addOnSuccessListener(documentReference -> {
                                    String threadId = documentReference.getId();
                                    callback.onSuccess(threadId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating chat thread", e);
                                    callback.onFailure(e.getMessage());
                                });
                    } else {
                        // Thread already exists, return its ID
                        String threadId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        callback.onSuccess(threadId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing chat thread", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // Send a message in a chat thread
    public void sendMessage(String threadId, ChatMessage message, ChatServiceCallback<Void> callback) {
        // Add message to messages subcollection
        db.collection(CHAT_THREADS_COLLECTION)
                .document(threadId)
                .collection(CHAT_MESSAGES_COLLECTION)
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    // Update thread's last message and timestamp
                    DocumentReference threadRef = db.collection(CHAT_THREADS_COLLECTION).document(threadId);
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastMessage", message.getMessage());
                    updates.put("lastMessageTimestamp", message.getTimestamp());
                    updates.put("unreadCount", FieldValue.increment(1));

                    threadRef.update(updates)
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating chat thread", e);
                                callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // Get chat threads for a user
    public void getChatThreads(String userId, boolean isAgent, ChatServiceCallback<List<ChatThread>> callback) {
        Query query = db.collection(CHAT_THREADS_COLLECTION)
                .whereEqualTo(isAgent ? "agentId" : "clientId", userId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChatThread> chatThreads = queryDocumentSnapshots.toObjects(ChatThread.class);
                    callback.onSuccess(chatThreads);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting chat threads", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // Get messages for a specific chat thread
    public void getChatMessages(String threadId, ChatServiceCallback<List<ChatMessage>> callback) {
        db.collection(CHAT_THREADS_COLLECTION)
                .document(threadId)
                .collection(CHAT_MESSAGES_COLLECTION)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChatMessage> messages = queryDocumentSnapshots.toObjects(ChatMessage.class);
                    callback.onSuccess(messages);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting chat messages", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // Mark messages as read
    public void markMessagesAsRead(String threadId, String userId, ChatServiceCallback<Void> callback) {
        db.collection(CHAT_THREADS_COLLECTION)
                .document(threadId)
                .collection(CHAT_MESSAGES_COLLECTION)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        DocumentReference messageRef = document.getReference();
                        batch.update(messageRef, "isRead", true);
                    }

                    // Reset unread count
                    DocumentReference threadRef = db.collection(CHAT_THREADS_COLLECTION).document(threadId);
                    batch.update(threadRef, "unreadCount", 0);

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error marking messages as read", e);
                                callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding unread messages", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // Callback interface for async operations
    public interface ChatServiceCallback<T> {
        void onSuccess(T result);
        void onFailure(String error);
    }
}