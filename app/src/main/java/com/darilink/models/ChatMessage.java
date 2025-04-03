package com.darilink.models;

public class ChatMessage {
    private String id;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String message;
    private long timestamp;
    private boolean isRead;
    private String senderProfileImage;

    // Constructors
    public ChatMessage() {}

    public ChatMessage(String senderId, String senderName, String receiverId,
                       String message, String senderProfileImage) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.senderProfileImage = senderProfileImage;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getSenderProfileImage() { return senderProfileImage; }
    public void setSenderProfileImage(String senderProfileImage) { this.senderProfileImage = senderProfileImage; }
}