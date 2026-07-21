package com.paasmart.backend.customer;

public class CustomerProfileResponse {

    private Long id;
    private String name;
    private String mobile;
    private String email;
    private String profileImage;

    public CustomerProfileResponse() {}
    public CustomerProfileResponse(Long id, String name, String mobile, String email, String profileImage) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.email = email;
        this.profileImage = profileImage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
