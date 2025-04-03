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
import com.darilink.adapters.ChatThreadAdapter;
import com.darilink.adapters.ChatMessagesAdapter;
import com.darilink.dataAccess.ChatService;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.ChatMessage;
import com.darilink.models.ChatThread;
import com.darilink.models.Offer;
import com.darilink.models.User;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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
    private ImageView sendButton, receiverProfileView;
    private TextView receiverNameText;

    private ChatService chatService;
    private Firebase firebase;
    private Firestore firestore;
    private FirebaseUser currentUser;

    private ChatMessagesAdapter messagesAdapter;
    private List<ChatMessage> messages = new ArrayList<>();

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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_detail, container, false);

        // Set title to receiver's name
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(receiverName);
            }
        }

        initializeViews(view);
        setupListeners();
        loadChatMessages();
        return view;
    }

    private void initializeViews(View view) {
        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        receiverProfileView = view.findViewById(R.id.receiverProfileImage);
        receiverNameText = view.findViewById(R.id.receiverNameText);

        // Set receiver info
        receiverNameText.setText(receiverName);
        if (receiverProfileImage != null && !receiverProfileImage.isEmpty()) {
            Glide.with(this)
                    .load(receiverProfileImage)
                    .placeholder(R.drawable.default_profile)
                    .circleCrop()
                    .into(receiverProfileView);
        }

        // Setup messages adapter
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

    private void loadChatMessages() {
        chatService.getChatMessages(threadId, new ChatService.ChatServiceCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> result) {
                messages.clear();
                messages.addAll(result);
                messagesAdapter.notifyDataSetChanged();

                // Scroll to bottom
                if (!messages.isEmpty()) {
                    messagesRecyclerView.scrollToPosition(messages.size() - 1);
                }

                // Mark messages as read
                chatService.markMessagesAsRead(threadId, currentUser.getUid(), new ChatService.ChatServiceCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // Messages marked as read
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e("ChatDetailFragment", "Failed to mark messages as read: " + error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Error loading messages: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String messageText) {
        // Fetch current user's details
        firestore.getDb().collection("agents").document(currentUser.getUid()).get()
                .addOnSuccessListener(agentSnapshot -> {
                    String senderName;
                    String senderProfileImage;

                    if (agentSnapshot.exists()) {
                        // User is an agent
                        senderName = agentSnapshot.getString("firstName") + " " +
                                agentSnapshot.getString("lastName");
                        senderProfileImage = agentSnapshot.getString("profileImageUrl");

                        createAndSendMessage(senderName, senderProfileImage, messageText);
                    } else {
                        // User is a client
                        firestore.getDb().collection("clients").document(currentUser.getUid()).get()
                                .addOnSuccessListener(clientSnapshot -> {
                                    String firstName = clientSnapshot.getString("firstName");
                                    String lastName = clientSnapshot.getString("lastName");
                                    String profileImage = clientSnapshot.getString("profileImageUrl");

                                    createAndSendMessage(
                                            firstName + " " + lastName,
                                            profileImage,
                                            messageText
                                    );
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error sending message", Toast.LENGTH_SHORT).show();
                });
    }

    private void createAndSendMessage(String senderName, String senderProfileImage, String messageText) {
        ChatMessage message = new ChatMessage(
                currentUser.getUid(),
                senderName,
                receiverId,
                messageText,
                senderProfileImage
        );

        chatService.sendMessage(threadId, message, new ChatService.ChatServiceCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Clear input and scroll to bottom
                messageInput.setText("");
                loadChatMessages();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Failed to send message: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}