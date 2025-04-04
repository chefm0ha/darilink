package com.darilink.models;

public class ChatThread {
    private String id;
    private String propertyId;
    private String clientId;
    private String agentId;
    private String clientName;
    private String agentName;
    private String clientProfileImage;
    private String agentProfileImage;
    private String lastMessage;
    private long lastMessageTimestamp;
    private int clientUnreadCount;
    private int agentUnreadCount;
    private String propertyTitle; // To show property context in the chat

    // Constructors
    public ChatThread() {}

    public ChatThread(String propertyId, String clientId, String agentId,
                      String clientName, String agentName,
                      String clientProfileImage, String agentProfileImage) {
        this.propertyId = propertyId;
        this.clientId = clientId;
        this.agentId = agentId;
        this.clientName = clientName;
        this.agentName = agentName;
        this.clientProfileImage = clientProfileImage;
        this.agentProfileImage = agentProfileImage;
        this.lastMessageTimestamp = System.currentTimeMillis();
        this.clientUnreadCount = 0;
        this.agentUnreadCount = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getClientProfileImage() { return clientProfileImage; }
    public void setClientProfileImage(String clientProfileImage) { this.clientProfileImage = clientProfileImage; }

    public String getAgentProfileImage() { return agentProfileImage; }
    public void setAgentProfileImage(String agentProfileImage) { this.agentProfileImage = agentProfileImage; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public int getClientUnreadCount() { return clientUnreadCount; }
    public void setClientUnreadCount(int clientUnreadCount) { this.clientUnreadCount = clientUnreadCount; }

    public int getAgentUnreadCount() { return agentUnreadCount; }
    public void setAgentUnreadCount(int agentUnreadCount) { this.agentUnreadCount = agentUnreadCount; }

    public String getPropertyTitle() { return propertyTitle; }
    public void setPropertyTitle(String propertyTitle) { this.propertyTitle = propertyTitle; }
}