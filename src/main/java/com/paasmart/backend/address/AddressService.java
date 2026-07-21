package com.paasmart.backend.address;

import java.util.List;

public interface AddressService {
    Address addAddress(Long customerId, AddAddressRequest request);
    List<Address> getAddresses(Long customerId);
    Address updateAddress(Long customerId, Long addressId, AddAddressRequest request);
    void deleteAddress(Long customerId, Long addressId);
    Address setDefaultAddress(Long customerId, Long addressId);
}