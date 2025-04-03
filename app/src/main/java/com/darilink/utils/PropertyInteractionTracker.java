package com.darilink.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class to track user interactions with properties
 * Used to determine when to prompt for reviews
 */
public class PropertyInteractionTracker {
    private static final String PREFS_NAME = "property_interaction_prefs";
    private static final String KEY_VIEWED_PROPERTIES = "viewed_properties";
    private static final String KEY_CONTACTED_AGENTS = "contacted_agents";
    private static final String KEY_REQUESTED_PROPERTIES = "requested_properties";

    private final SharedPreferences prefs;

    public PropertyInteractionTracker(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Records that a user has viewed a property
     * @param propertyId The ID of the property viewed
     */
    public void recordPropertyView(String propertyId) {
        Set<String> viewedProperties = getViewedProperties();
        viewedProperties.add(propertyId);
        prefs.edit().putStringSet(KEY_VIEWED_PROPERTIES, viewedProperties).apply();
    }

    /**
     * Records that a user has contacted an agent about a property
     * @param propertyId The ID of the property
     */
    public void recordAgentContact(String propertyId) {
        Set<String> contactedAgents = getContactedAgents();
        contactedAgents.add(propertyId);
        prefs.edit().putStringSet(KEY_CONTACTED_AGENTS, contactedAgents).apply();
    }

    /**
     * Records that a user has made a request for a property
     * @param propertyId The ID of the property
     */
    public void recordPropertyRequest(String propertyId) {
        Set<String> requestedProperties = getRequestedProperties();
        requestedProperties.add(propertyId);
        prefs.edit().putStringSet(KEY_REQUESTED_PROPERTIES, requestedProperties).apply();
    }

    /**
     * Gets all properties viewed by the user
     * @return A set of property IDs
     */
    public Set<String> getViewedProperties() {
        return new HashSet<>(prefs.getStringSet(KEY_VIEWED_PROPERTIES, new HashSet<>()));
    }

    /**
     * Gets all properties for which the user has contacted an agent
     * @return A set of property IDs
     */
    public Set<String> getContactedAgents() {
        return new HashSet<>(prefs.getStringSet(KEY_CONTACTED_AGENTS, new HashSet<>()));
    }

    /**
     * Gets all properties for which the user has made a request
     * @return A set of property IDs
     */
    public Set<String> getRequestedProperties() {
        return new HashSet<>(prefs.getStringSet(KEY_REQUESTED_PROPERTIES, new HashSet<>()));
    }

    /**
     * Checks if a user should be prompted to review a property
     * The logic is to prompt if they've either:
     * 1. Viewed the property multiple times
     * 2. Contacted an agent about it
     * 3. Made a request for it
     *
     * @param propertyId The ID of the property
     * @return True if the user should be prompted to review
     */
    public boolean shouldPromptForReview(String propertyId) {
        // If the user has contacted an agent or made a request, they should be prompted
        if (getContactedAgents().contains(propertyId) ||
                getRequestedProperties().contains(propertyId)) {
            return true;
        }

        // Count how many times the user has viewed the property
        // This is an approximation since we're using a Set which deduplicates entries
        // For a more accurate count, we'd need to use a different data structure
        return getViewedProperties().contains(propertyId) &&
                countPropertyViews(propertyId) >= 3;
    }

    /**
     * Count how many times a property has been viewed
     * This is a placeholder implementation since we're using Sets
     * which don't store duplicates. In a real implementation,
     * you might use a database to track interaction counts.
     */
    private int countPropertyViews(String propertyId) {
        // This is a placeholder - in a real implementation, you would
        // query a database or other storage that keeps track of actual view counts
        return getViewedProperties().contains(propertyId) ? 3 : 0;
    }

    /**
     * Clears all tracked interactions
     */
    public void clearAllInteractions() {
        prefs.edit()
                .remove(KEY_VIEWED_PROPERTIES)
                .remove(KEY_CONTACTED_AGENTS)
                .remove(KEY_REQUESTED_PROPERTIES)
                .apply();
    }

    /**
     * Clears interactions for a specific property
     * @param propertyId The ID of the property
     */
    public void clearPropertyInteractions(String propertyId) {
        // Remove from viewed properties
        Set<String> viewedProperties = getViewedProperties();
        viewedProperties.remove(propertyId);
        prefs.edit().putStringSet(KEY_VIEWED_PROPERTIES, viewedProperties).apply();

        // Remove from contacted agents
        Set<String> contactedAgents = getContactedAgents();
        contactedAgents.remove(propertyId);
        prefs.edit().putStringSet(KEY_CONTACTED_AGENTS, contactedAgents).apply();

        // Remove from requested properties
        Set<String> requestedProperties = getRequestedProperties();
        requestedProperties.remove(propertyId);
        prefs.edit().putStringSet(KEY_REQUESTED_PROPERTIES, requestedProperties).apply();
    }
}