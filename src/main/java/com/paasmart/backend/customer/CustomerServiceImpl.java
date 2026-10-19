package com.paasmart.backend.customer;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerServiceImpl implements CustomerService{

    @Autowired private UserRepository userRepository;

    @Override
    public CustomerProfileResponse getProfile(Long customerId) {

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(" Customer not found"));

        return new CustomerProfileResponse(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getProfileImage()
        );
    }

    @Override
    public CustomerProfileResponse updateProfile(Long customerId, UpdateCustomerRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            customer.setName(request.getFullName().trim());
        }
        if (request.getProfileImage() != null) {
            customer.setProfileImage(request.getProfileImage());
        }

        User saved = userRepository.save(customer);

        return new CustomerProfileResponse(
                saved.getId(),
                saved.getName(),
                saved.getPhone(),   
                saved.getEmail(),
                saved.getProfileImage()
        );
    }
}
