package com.darilink.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.darilink.R;
import com.darilink.adapters.ChatThreadAdapter;
import com.darilink.dataAccess.ChatService;
import com.darilink.dataAccess.Firebase;
import com.darilink.dataAccess.Firestore;
import com.darilink.models.ChatThread;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ChatThreadsFragment extends Fragment implements ChatThreadAdapter.ChatThreadListener {
    private RecyclerView chatThreadsRecyclerView;
    private LinearLayout emptyStateLayout;
    private SwipeRefreshLayout swipeRefresh;

    private ChatService chatService;
    private Firebase firebase;
    private Firestore firestore;
    private FirebaseUser currentUser;
    private boolean isAgent = false;

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
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        initializeComponents(view);
        setupRecyclerView();

        return view;
    }

    private void initializeComponents(View view) {
        // Initialize all view components with null safety checks
        try {
            chatThreadsRecyclerView = view.findViewById(R.id.chatThreadsRecyclerView);
            emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
            swipeRefresh = view.findViewById(R.id.swipeRefresh);

            // Set up swipe refresh if available
            if (swipeRefresh != null) {
                swipeRefresh.setOnRefreshListener(this::loadChatThreads);
            } else {
                // Log error or use a fallback
                System.out.println("SwipeRefreshLayout not found in layout");
            }

            // Initialize services
            chatService = ChatService.getInstance();
            firebase = Firebase.getInstance();
            firestore = Firestore.getInstance();
            currentUser = firebase.getCurrentUser();

            // Determine user type only if user is authenticated
            if (currentUser != null) {
                determineUserType();
            } else {
                showEmptyState(true);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Please log in to view chats", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error initializing chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupRecyclerView() {
        if (chatThreadsRecyclerView != null) {
            chatThreadAdapter = new ChatThreadAdapter(requireContext(), chatThreads, this);
            chatThreadsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            chatThreadsRecyclerView.setAdapter(chatThreadAdapter);
        }
    }

    private void determineUserType() {
        if (currentUser == null) {
            showEmptyState(true);
            return;
        }

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
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }

        if (currentUser == null) {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            showEmptyState(true);
            return;
        }

        chatService.getChatThreads(currentUser.getUid(), isAgent, new ChatService.ChatServiceCallback<List<ChatThread>>() {
            @Override
            public void onSuccess(List<ChatThread> result) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }

                chatThreads.clear();
                chatThreads.addAll(result);

                // Set up real-time listener for future updates
                setupRealtimeUpdates();

                // Update UI
                updateUI();
            }

            @Override
            public void onFailure(String error) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }

                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading chats: " + error, Toast.LENGTH_SHORT).show();
                }
                showEmptyState(true);
            }
        });
    }

    private void setupRealtimeUpdates() {
        if (currentUser == null) return;

        chatService.addThreadsListener(currentUser.getUid(), isAgent, new ChatService.ThreadsListener() {
            @Override
            public void onThreadsUpdated(List<ChatThread> threads) {
                // Update the list only if we're still attached to activity
                if (isAdded()) {
                    chatThreads.clear();
                    chatThreads.addAll(threads);
                    updateUI();
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Error updating chat list: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI() {
        if (chatThreads.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            if (chatThreadAdapter != null) {
                chatThreadAdapter.notifyDataSetChanged();
            }
        }
    }

    private void showEmptyState(boolean isEmpty) {
        if (chatThreadsRecyclerView != null && emptyStateLayout != null) {
            chatThreadsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onChatThreadClick(ChatThread chatThread) {
        // Navigate to chat detail fragment
        if (getActivity() != null) {
            // Get current user
            FirebaseUser user = firebase.getCurrentUser();
            String currentUserId = user != null ? user.getUid() : "";

            // Determine if user is the agent for this specific thread
            boolean userIsAgent = currentUserId.equals(chatThread.getAgentId());

            // Determine the receiver based on user's role in this thread
            String receiverId = userIsAgent ? chatThread.getClientId() : chatThread.getAgentId();
            String receiverName = userIsAgent ? chatThread.getClientName() : chatThread.getAgentName();
            String receiverImage = userIsAgent ? chatThread.getClientProfileImage() : chatThread.getAgentProfileImage();

            ChatDetailFragment chatDetailFragment = ChatDetailFragment.newInstance(
                    chatThread.getId(),
                    receiverId,
                    receiverName,
                    receiverImage
            );

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, chatDetailFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Remove the real-time listener when the fragment is destroyed
        if (currentUser != null) {
            chatService.removeThreadsListener(currentUser.getUid());
        }
    }
}