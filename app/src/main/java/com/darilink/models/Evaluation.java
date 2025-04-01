package com.darilink.models;

public class Evaluation {
    private String id;
    private String clientId;
    private String offerId;
    private int rating;
    private String comment;
    private long date;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }
}