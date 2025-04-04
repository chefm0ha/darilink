package com.darilink.dataAccess;

import android.util.Log;

import com.darilink.models.ChatMessage;
import com.darilink.models.ChatThread;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatService {
    private static final String TAG = "ChatService";
    private static final String CHAT_THREADS_COLLECTION = "chat_threads";
    private static final String CHAT_MESSAGES_COLLECTION = "chat_messages";

    private static ChatService instance;
    private final FirebaseFirestore db;
    private Map<String, ListenerRegistration> messageListeners = new HashMap<>();
    private Map<String, ListenerRegistration> threadListeners = new HashMap<>();

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
                                    // Update the thread with its ID
                                    documentReference.update("id", threadId);
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
                    // Update the message with its ID
                    documentReference.update("id", documentReference.getId());

                    // First get the thread document to check IDs
                    db.collection(CHAT_THREADS_COLLECTION).document(threadId)
                            .get()
                            .addOnSuccessListener(threadDoc -> {
                                // Update thread's last message and timestamp
                                DocumentReference threadRef = db.collection(CHAT_THREADS_COLLECTION).document(threadId);
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("lastMessage", message.getMessage());
                                updates.put("lastMessageTimestamp", message.getTimestamp());

                                // Determine which unread counter to increment
                                String clientId = threadDoc.getString("clientId");
                                String agentId = threadDoc.getString("agentId");

                                // Increment the appropriate unread counter
                                if (message.getReceiverId().equals(clientId)) {
                                    // Message is to client, increment client unread count
                                    updates.put("clientUnreadCount", FieldValue.increment(1));
                                } else if (message.getReceiverId().equals(agentId)) {
                                    // Message is to agent, increment agent unread count
                                    updates.put("agentUnreadCount", FieldValue.increment(1));
                                }

                                threadRef.update(updates)
                                        .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error updating chat thread", e);
                                            callback.onFailure(e.getMessage());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error getting thread document", e);
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
                    List<ChatThread> chatThreads = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        ChatThread thread = doc.toObject(ChatThread.class);
                        if (thread != null) {
                            thread.setId(doc.getId());
                            chatThreads.add(thread);
                        }
                    }
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
                    List<ChatMessage> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        ChatMessage message = doc.toObject(ChatMessage.class);
                        if (message != null) {
                            message.setId(doc.getId());
                            messages.add(message);
                        }
                    }
                    callback.onSuccess(messages);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting chat messages", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // Set up a real-time listener for messages
    public void addMessagesListener(String threadId, MessagesListener listener) {
        // Remove any existing listener for this thread
        removeMessagesListener(threadId);

        // Create new listener
        ListenerRegistration registration = db.collection(CHAT_THREADS_COLLECTION)
                .document(threadId)
                .collection(CHAT_MESSAGES_COLLECTION)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for messages", error);
                        listener.onError(error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<ChatMessage> messages = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            ChatMessage message = doc.toObject(ChatMessage.class);
                            if (message != null) {
                                message.setId(doc.getId());
                                messages.add(message);
                            }
                        }
                        listener.onMessagesUpdated(messages);
                    }
                });

        // Store listener registration
        messageListeners.put(threadId, registration);
    }

    // Remove message listener
    public void removeMessagesListener(String threadId) {
        ListenerRegistration registration = messageListeners.get(threadId);
        if (registration != null) {
            registration.remove();
            messageListeners.remove(threadId);
        }
    }

    // Set up a real-time listener for threads
    public void addThreadsListener(String userId, boolean isAgent, ThreadsListener listener) {
        // Remove any existing listener for this user
        removeThreadsListener(userId);

        // Create new listener
        ListenerRegistration registration = db.collection(CHAT_THREADS_COLLECTION)
                .whereEqualTo(isAgent ? "agentId" : "clientId", userId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for threads", error);
                        listener.onError(error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<ChatThread> threads = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            ChatThread thread = doc.toObject(ChatThread.class);
                            if (thread != null) {
                                thread.setId(doc.getId());
                                threads.add(thread);
                            }
                        }
                        listener.onThreadsUpdated(threads);
                    }
                });

        // Store listener registration
        threadListeners.put(userId, registration);
    }

    // Remove thread listener
    public void removeThreadsListener(String userId) {
        ListenerRegistration registration = threadListeners.get(userId);
        if (registration != null) {
            registration.remove();
            threadListeners.remove(userId);
        }
    }

    // Mark messages as read
    public void markMessagesAsRead(String threadId, String userId, ChatServiceCallback<Void> callback) {
        // Get the thread to determine if user is agent or client
        db.collection(CHAT_THREADS_COLLECTION)
                .document(threadId)
                .get()
                .addOnSuccessListener(threadDoc -> {
                    ChatThread thread = threadDoc.toObject(ChatThread.class);
                    if (thread == null) {
                        callback.onFailure("Thread not found");
                        return;
                    }

                    // Determine counter field to reset
                    boolean isAgent = userId.equals(thread.getAgentId());
                    String counterField = isAgent ? "agentUnreadCount" : "clientUnreadCount";

                    // Mark messages as read
                    db.collection(CHAT_THREADS_COLLECTION)
                            .document(threadId)
                            .collection(CHAT_MESSAGES_COLLECTION)
                            .whereEqualTo("receiverId", userId)
                            .whereEqualTo("isRead", false)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (queryDocumentSnapshots.isEmpty()) {
                                    // No unread messages
                                    callback.onSuccess(null);
                                    return;
                                }

                                WriteBatch batch = db.batch();
                                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                    DocumentReference messageRef = document.getReference();
                                    batch.update(messageRef, "isRead", true);
                                }

                                // Reset unread count
                                DocumentReference threadRef = db.collection(CHAT_THREADS_COLLECTION).document(threadId);
                                batch.update(threadRef, counterField, 0);

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
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Error getting thread: " + e.getMessage());
                });
    }

    // Start a new chat from a property detail
    public void startChatFromProperty(String propertyId, String clientId, String agentId,
                                      String clientName, String agentName,
                                      String clientProfileImage, String agentProfileImage,
                                      ChatServiceCallback<String> callback) {

        ChatThread newThread = new ChatThread(propertyId, clientId, agentId,
                clientName, agentName, clientProfileImage, agentProfileImage);
        newThread.setLastMessage("Chat started");
        newThread.setLastMessageTimestamp(System.currentTimeMillis());

        createChatThread(newThread, callback);
    }

    // Get thread by ID
    public void getThreadById(String threadId, ChatServiceCallback<ChatThread> callback) {
        db.collection(CHAT_THREADS_COLLECTION)
                .document(threadId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    ChatThread thread = documentSnapshot.toObject(ChatThread.class);
                    if (thread != null) {
                        thread.setId(documentSnapshot.getId());
                        callback.onSuccess(thread);
                    } else {
                        callback.onFailure("Thread not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting thread", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // Callback interface for async operations
    public interface ChatServiceCallback<T> {
        void onSuccess(T result);
        void onFailure(String error);
    }

    // Interface for real-time messages updates
    public interface MessagesListener {
        void onMessagesUpdated(List<ChatMessage> messages);
        void onError(String error);
    }

    // Interface for real-time threads updates
    public interface ThreadsListener {
        void onThreadsUpdated(List<ChatThread> threads);
        void onError(String error);
    }
}