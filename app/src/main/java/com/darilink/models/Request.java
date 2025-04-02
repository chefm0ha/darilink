package com.darilink.models;

public class Request {
    private String id;
    private String clientId;
    private String offerId;
    private String message;
    private double rentProposal;
    private String employmentStatus;
    private String maritalStatus;
    private int numChildren;
    private int duration; // in months
    private long createdAt;
    private String status; // pending, accepted, rejected
    private String agentReply; // Added field for agent's response

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public double getRentProposal() { return rentProposal; }
    public void setRentProposal(double rentProposal) { this.rentProposal = rentProposal; }

    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }

    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

    public int getNumChildren() { return numChildren; }
    public void setNumChildren(int numChildren) { this.numChildren = numChildren; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAgentReply() { return agentReply; }
    public void setAgentReply(String agentReply) { this.agentReply = agentReply; }
}