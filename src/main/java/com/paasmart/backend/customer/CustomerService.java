package com.paasmart.backend.customer;

public interface CustomerService {

    CustomerProfileResponse getProfile(Long customerId);

    CustomerProfileResponse updateProfile(
            Long customerId,
            UpdateCustomerRequest request
    );

}
