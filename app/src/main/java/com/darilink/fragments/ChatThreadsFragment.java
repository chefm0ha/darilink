package com.darilink.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class ChatThreadsFragment extends Fragment implements ChatThreadAdapter.ChatThreadListener {
    private RecyclerView chatThreadsRecyclerView;
    private LinearLayout emptyStateLayout;

    private ChatService chatService;
    private Firebase firebase;
    private Firestore firestore;
    private FirebaseUser currentUser;
    private boolean isAgent;

    private ChatThreadAdapter chatThreadAdapter;
    private List<ChatThread> chatThreads = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_threads, container, false);

        // Set title
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(R.string.chats);
            }
        }

        initializeComponents(view);
        return view;
    }

    private void initializeComponents(View view) {
        chatThreadsRecyclerView = view.findViewById(R.id.chatThreadsRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);

        chatService = ChatService.getInstance();
        firebase = Firebase.getInstance();
        firestore = Firestore.getInstance();
        currentUser = firebase.getCurrentUser();

        // Determine user type (agent or client)
        determineUserType();
    }

    private void determineUserType() {
        // Check if user is an agent or client
        firestore.getDb().collection("agents").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    isAgent = documentSnapshot.exists();
                    loadChatThreads();
                })
                .addOnFailureListener(e -> {
                    // Fallback to client
                    isAgent = false;
                    loadChatThreads();
                });
    }

    private void loadChatThreads() {
        chatService.getChatThreads(currentUser.getUid(), isAgent, new ChatService.ChatServiceCallback<List<ChatThread>>() {
            @Override
            public void onSuccess(List<ChatThread> result) {
                chatThreads.clear();
                chatThreads.addAll(result);
                updateUI();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Error loading chats: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (chatThreads.isEmpty()) {
            chatThreadsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            chatThreadsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);

            // Setup RecyclerView
            chatThreadAdapter = new ChatThreadAdapter(requireContext(), chatThreads, this);
            chatThreadsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            chatThreadsRecyclerView.setAdapter(chatThreadAdapter);
        }
    }

    @Override
    public void onChatThreadClick(ChatThread chatThread) {
        // Navigate to chat detail fragment
        ChatDetailFragment chatDetailFragment = ChatDetailFragment.newInstance(
                chatThread.getId(),
                isAgent ? chatThread.getClientId() : chatThread.getAgentId(),
                isAgent ? chatThread.getClientName() : chatThread.getAgentName(),
                isAgent ? chatThread.getClientProfileImage() : chatThread.getAgentProfileImage()
        );

        getParentFragmentManager().beginTransaction()
                .replace(R.id.content_frame, chatDetailFragment)
                .addToBackStack(null)
                .commit();
    }
}