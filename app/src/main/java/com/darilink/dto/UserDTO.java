package com.darilink.dto;

import com.darilink.models.User;

public class UserDTO {
    private String email;
    private String firstName;
    private String lastName;
    private String country;
    private String city;
    private String address;
    private String phone;

    protected static <T extends UserDTO> T fromUser(User user, Class<T> dtoClass) {
        try {
            T dto = dtoClass.getDeclaredConstructor().newInstance();
            dto.setEmail(user.getEmail());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setCountry(user.getCountry());
            dto.setCity(user.getCity());
            dto.setAddress(user.getAddress());
            dto.setPhone(user.getPhone());
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Error creating DTO", e);
        }
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
