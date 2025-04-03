package com.darilink.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.darilink.R;
import com.darilink.models.ChatThread;
import com.darilink.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatThreadAdapter extends RecyclerView.Adapter<ChatThreadAdapter.ChatThreadViewHolder> {
    private final Context context;
    private final List<ChatThread> chatThreads;
    private final ChatThreadListener listener;

    public ChatThreadAdapter(Context context, List<ChatThread> chatThreads, ChatThreadListener listener) {
        this.context = context;
        this.chatThreads = chatThreads;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatThreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_thread, parent, false);
        return new ChatThreadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatThreadViewHolder holder, int position) {
        ChatThread chatThread = chatThreads.get(position);

        // Determine which user's info to show (client or agent)
        String profileImage = chatThread.getAgentProfileImage();
        String userName = chatThread.getAgentName();

        // Set user name
        holder.userNameText.setText(userName);

        // Set last message
        holder.lastMessageText.setText(chatThread.getLastMessage());

        // Set profile image
        if (profileImage != null && !profileImage.isEmpty()) {
            Glide.with(context)
                    .load(profileImage)
                    .placeholder(R.drawable.default_profile)
                    .circleCrop()
                    .into(holder.profileImage);
        }

        // Set timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
        holder.timestampText.setText(dateFormat.format(new Date(chatThread.getLastMessageTimestamp())));

        // Set unread count
        if (chatThread.getUnreadCount() > 0) {
            holder.unreadCountText.setVisibility(View.VISIBLE);
            holder.unreadCountText.setText(String.valueOf(chatThread.getUnreadCount()));
        } else {
            holder.unreadCountText.setVisibility(View.GONE);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> listener.onChatThreadClick(chatThread));
    }

    @Override
    public int getItemCount() {
        return chatThreads.size();
    }

    static class ChatThreadViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView userNameText, lastMessageText, timestampText, unreadCountText;

        ChatThreadViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            userNameText = itemView.findViewById(R.id.userNameText);
            lastMessageText = itemView.findViewById(R.id.lastMessageText);
            timestampText = itemView.findViewById(R.id.timestampText);
            unreadCountText = itemView.findViewById(R.id.unreadCountText);
        }
    }

    public interface ChatThreadListener {
        void onChatThreadClick(ChatThread chatThread);
    }
}

