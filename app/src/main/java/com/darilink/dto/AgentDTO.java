package com.darilink.dto;

import com.darilink.models.Agent;

public class AgentDTO extends UserDTO {
    private String agencyName;
    private String agencyAddress;
    private String agencyCountry;
    private String agencyCity;
    private String agencyEmail;
    private String agencyPhone;

    public static AgentDTO fromAgent(Agent agent) {
        AgentDTO dto = fromUser(agent, AgentDTO.class);
        dto.setAgencyName(agent.getAgencyName());
        dto.setAgencyAddress(agent.getAgencyAddress());
        dto.setAgencyCountry(agent.getAgencyCountry());
        dto.setAgencyCity(agent.getAgencyCity());
        dto.setAgencyEmail(agent.getAgencyEmail());
        dto.setAgencyPhone(agent.getAgencyPhone());
        return dto;
    }

    // Getters and Setters for agent-specific fields
    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }
    public String getAgencyAddress() { return agencyAddress; }
    public void setAgencyAddress(String agencyAddress) { this.agencyAddress = agencyAddress; }
    public String getAgencyCountry() { return agencyCountry; }
    public void setAgencyCountry(String agencyCountry) { this.agencyCountry = agencyCountry; }
    public String getAgencyCity() { return agencyCity; }
    public void setAgencyCity(String agencyCity) { this.agencyCity = agencyCity; }
    public String getAgencyEmail() { return agencyEmail; }
    public void setAgencyEmail(String agencyEmail) { this.agencyEmail = agencyEmail; }
    public String getAgencyPhone() { return agencyPhone; }
    public void setAgencyPhone(String agencyPhone) { this.agencyPhone = agencyPhone; }
}
