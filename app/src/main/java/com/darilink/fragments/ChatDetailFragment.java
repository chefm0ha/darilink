package com.darilink.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.darilink.R;
import com.darilink.adapters.ChatMessagesAdapter;
import com.darilink.dataAccess.ChatService;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.ChatMessage;
import com.darilink.models.ChatThread;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatDetailFragment extends Fragment {
    private static final String ARG_THREAD_ID = "thread_id";
    private static final String ARG_RECEIVER_ID = "receiver_id";
    private static final String ARG_RECEIVER_NAME = "receiver_name";
    private static final String ARG_RECEIVER_PROFILE = "receiver_profile";

    private String threadId;
    private String receiverId;
    private String receiverName;
    private String receiverProfileImage;

    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private ImageView sendButton;
    private CircleImageView receiverProfileImageView;
    private TextView receiverNameText;

    private ChatService chatService;
    private Firebase firebase;
    private Firestore firestore;
    private FirebaseUser currentUser;
    private String currentUserName;
    private String currentUserProfileImage;
    private TextView propertyInfoText;

    private ChatMessagesAdapter messagesAdapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private boolean isInitialLoad = true;

    public static ChatDetailFragment newInstance(String threadId, String receiverId,
                                                 String receiverName, String receiverProfileImage) {
        ChatDetailFragment fragment = new ChatDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_THREAD_ID, threadId);
        args.putString(ARG_RECEIVER_ID, receiverId);
        args.putString(ARG_RECEIVER_NAME, receiverName);
        args.putString(ARG_RECEIVER_PROFILE, receiverProfileImage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            threadId = getArguments().getString(ARG_THREAD_ID);
            receiverId = getArguments().getString(ARG_RECEIVER_ID);
            receiverName = getArguments().getString(ARG_RECEIVER_NAME);
            receiverProfileImage = getArguments().getString(ARG_RECEIVER_PROFILE);
        }

        chatService = ChatService.getInstance();
        firebase = Firebase.getInstance();
        firestore = Firestore.getInstance();
        currentUser = firebase.getCurrentUser();

        // Fetch current user details for messages
        loadCurrentUserInfo();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_detail, container, false);

        // Set up ActionBar
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle("");
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        initializeViews(view);
        setupListeners();
        setupRecyclerView();
        setupRealtimeMessagesListener();

        // Mark messages as read when this chat is opened
        markMessagesAsRead();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupListeners();
        setupRecyclerView();
        setupRealtimeMessagesListener();

        // Load property info to show context
        loadPropertyInfo();

        // Mark messages as read when this chat is opened
        markMessagesAsRead();
    }

    private void loadPropertyInfo() {
        if (threadId == null) return;

        chatService.getThreadById(threadId, new ChatService.ChatServiceCallback<ChatThread>() {
            @Override
            public void onSuccess(ChatThread thread) {
                if (thread.getPropertyId() != null && propertyInfoText != null) {
                    // Load property info
                    firestore.getDb().collection("Offer").document(thread.getPropertyId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String propertyTitle = documentSnapshot.getString("title");
                                    if (propertyTitle != null && !propertyTitle.isEmpty()) {
                                        propertyInfoText.setText("Re: " + propertyTitle);
                                        propertyInfoText.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("ChatDetailFragment", "Failed to get thread info for property: " + error);
            }
        });
    }
    private void initializeViews(View view) {
        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        receiverProfileImageView = view.findViewById(R.id.receiverProfileImage);
        receiverNameText = view.findViewById(R.id.receiverNameText);
        propertyInfoText = view.findViewById(R.id.propertyInfoText); // Add this to your layout

        // Set receiver info
        receiverNameText.setText(receiverName);
        if (receiverProfileImage != null && !receiverProfileImage.isEmpty()) {
            Glide.with(this)
                    .load(receiverProfileImage)
                    .placeholder(R.drawable.default_profile)
                    .circleCrop()
                    .into(receiverProfileImageView);
        }
    }

    private void setupRecyclerView() {
        messagesAdapter = new ChatMessagesAdapter(requireContext(), messages, currentUser.getUid());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    private void setupListeners() {
        sendButton.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            }
        });
    }

    private void loadCurrentUserInfo() {
        if (currentUser == null) return;

        Log.d("ChatDetailFragment", "Loading user info for: " + currentUser.getUid());

        // First check if user is agent by directly getting the thread info
        chatService.getThreadById(threadId, new ChatService.ChatServiceCallback<ChatThread>() {
            @Override
            public void onSuccess(ChatThread thread) {
                boolean isAgent = thread.getAgentId().equals(currentUser.getUid());

                if (isAgent) {
                    Log.d("ChatDetailFragment", "Current user is the agent");
                    // User is agent, get info from agents collection
                    firestore.getDb().collection("agents").document(currentUser.getUid()).get()
                            .addOnSuccessListener(agentSnapshot -> {
                                if (agentSnapshot.exists()) {
                                    String firstName = agentSnapshot.getString("firstName");
                                    String lastName = agentSnapshot.getString("lastName");
                                    currentUserName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "") + " (Agent)";
                                    currentUserProfileImage = agentSnapshot.getString("profileImageUrl");

                                    Log.d("ChatDetailFragment", "Agent info loaded: " + currentUserName);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ChatDetailFragment", "Error loading agent info", e);
                            });
                } else {
                    Log.d("ChatDetailFragment", "Current user is the client");
                    // User is client, get info from clients collection
                    firestore.getDb().collection("clients").document(currentUser.getUid()).get()
                            .addOnSuccessListener(clientSnapshot -> {
                                if (clientSnapshot.exists()) {
                                    String firstName = clientSnapshot.getString("firstName");
                                    String lastName = clientSnapshot.getString("lastName");
                                    currentUserName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                                    currentUserProfileImage = clientSnapshot.getString("profileImageUrl");

                                    Log.d("ChatDetailFragment", "Client info loaded: " + currentUserName);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ChatDetailFragment", "Error loading client info", e);
                            });
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("ChatDetailFragment", "Failed to get thread info: " + error);
                // Fallback to checking both collections
                checkAgentThenClient();
            }
        });
    }

    private void checkAgentThenClient() {
        firestore.getDb().collection("agents").document(currentUser.getUid()).get()
                .addOnSuccessListener(agentSnapshot -> {
                    if (agentSnapshot.exists()) {
                        String firstName = agentSnapshot.getString("firstName");
                        String lastName = agentSnapshot.getString("lastName");
                        currentUserName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "") + " (Agent)";
                        currentUserProfileImage = agentSnapshot.getString("profileImageUrl");

                        Log.d("ChatDetailFragment", "Fallback: Agent info loaded: " + currentUserName);
                    } else {
                        // Not found in agents, try clients
                        firestore.getDb().collection("clients").document(currentUser.getUid()).get()
                                .addOnSuccessListener(clientSnapshot -> {
                                    if (clientSnapshot.exists()) {
                                        String firstName = clientSnapshot.getString("firstName");
                                        String lastName = clientSnapshot.getString("lastName");
                                        currentUserName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                                        currentUserProfileImage = clientSnapshot.getString("profileImageUrl");

                                        Log.d("ChatDetailFragment", "Fallback: Client info loaded: " + currentUserName);
                                    }
                                });
                    }
                });
    }

    private void setupRealtimeMessagesListener() {
        chatService.addMessagesListener(threadId, new ChatService.MessagesListener() {
            @Override
            public void onMessagesUpdated(List<ChatMessage> updatedMessages) {
                // Update the messages list
                messages.clear();
                messages.addAll(updatedMessages);
                messagesAdapter.notifyDataSetChanged();

                // Scroll to bottom on updates
                if (!messages.isEmpty()) {
                    messagesRecyclerView.scrollToPosition(messages.size() - 1);

                    // Mark as read if this is not the initial load
                    if (!isInitialLoad) {
                        markMessagesAsRead();
                    }
                    isInitialLoad = false;
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error updating messages: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendMessage(String messageText) {
        if (currentUser == null) return;

        // If currentUserName is still null, use a default
        String senderName = currentUserName != null ? currentUserName : "User";

        ChatMessage message = new ChatMessage(
                currentUser.getUid(),
                senderName,
                receiverId,
                messageText,
                currentUserProfileImage
        );

        // Clear input immediately for better UX
        messageInput.setText("");

        chatService.sendMessage(threadId, message, new ChatService.ChatServiceCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Message sent successfully - we'll get it back via the listener
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to send message: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void markMessagesAsRead() {
        if (currentUser == null || threadId == null) return;

        // First determine if the current user is agent or client for this thread
        chatService.getThreadById(threadId, new ChatService.ChatServiceCallback<ChatThread>() {
            @Override
            public void onSuccess(ChatThread thread) {
                boolean isAgent = currentUser.getUid().equals(thread.getAgentId());
                String fieldToUpdate = isAgent ? "agentUnreadCount" : "clientUnreadCount";

                // Update the thread document directly to reset the unread count
                firestore.getDb().collection("chat_threads")
                        .document(threadId)
                        .update(fieldToUpdate, 0)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("ChatDetailFragment", "Reset unread count for " +
                                    (isAgent ? "agent" : "client"));
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ChatDetailFragment", "Failed to reset unread count", e);
                        });

                // Also mark individual messages as read
                chatService.markMessagesAsRead(threadId, currentUser.getUid(),
                        new ChatService.ChatServiceCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d("ChatDetailFragment", "Marked messages as read");
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e("ChatDetailFragment", "Error marking messages as read: " + error);
                            }
                        });
            }

            @Override
            public void onFailure(String error) {
                Log.e("ChatDetailFragment", "Error getting thread to mark messages as read: " + error);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Remove message listener when fragment is destroyed
        if (threadId != null) {
            chatService.removeMessagesListener(threadId);
        }
    }
}