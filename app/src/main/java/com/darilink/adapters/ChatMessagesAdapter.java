package com.darilink.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.darilink.R;
import com.darilink.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final Context context;
    private final List<ChatMessage> messages;
    private final String currentUserId;

    public ChatMessagesAdapter(Context context, List<ChatMessage> messages, String currentUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return message.getSenderId().equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder instanceof SentMessageViewHolder) {
            SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
            sentHolder.messageText.setText(message.getMessage());
            sentHolder.timestampText.setText(formatTimestamp(message.getTimestamp()));
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
            receivedHolder.messageText.setText(message.getMessage());
            receivedHolder.senderNameText.setText(message.getSenderName());
            receivedHolder.timestampText.setText(formatTimestamp(message.getTimestamp()));

            // Set sender profile image
            if (message.getSenderProfileImage() != null && !message.getSenderProfileImage().isEmpty()) {
                Glide.with(context)
                        .load(message.getSenderProfileImage())
                        .placeholder(R.drawable.default_profile)
                        .circleCrop()
                        .into(receivedHolder.senderProfileImage);
            } else {
                receivedHolder.senderProfileImage.setImageResource(R.drawable.default_profile);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timestampText;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.sentMessageText);
            timestampText = itemView.findViewById(R.id.sentTimestampText);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, senderNameText, timestampText;
        CircleImageView senderProfileImage;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.receivedMessageText);
            senderNameText = itemView.findViewById(R.id.senderNameText);
            timestampText = itemView.findViewById(R.id.receivedTimestampText);
            senderProfileImage = itemView.findViewById(R.id.senderProfileImage);
        }
    }
}