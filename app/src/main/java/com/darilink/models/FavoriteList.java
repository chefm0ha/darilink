package com.darilink.models;

import java.util.List;

public class FavoriteList {
    private String id;
    private String clientId;
    private String label;
    private List<String> offerIds;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public List<String> getOfferIds() { return offerIds; }
    public void setOfferIds(List<String> offerIds) { this.offerIds = offerIds; }
}