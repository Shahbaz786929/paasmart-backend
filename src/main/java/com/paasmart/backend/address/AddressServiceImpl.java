package com.paasmart.backend.address;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired private AddressRepository addressRepository;
    @Autowired private UserRepository userRepository;

    @Override
    public Address addAddress(Long customerId, AddAddressRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Not Found"));

        if (request.isDefaultAddress()) {
            List<Address> addresses = addressRepository.findByCustomer(customer);
            for (Address address : addresses) {
                address.setDefaultAddress(false);
            }
            addressRepository.saveAll(addresses);
        }

        Address address = new Address();
        address.setCustomer(customer);
        address.setFullName(request.getFullName());
        address.setMobileNumber(request.getMobileNumber());
        address.setHouseNo(request.getHouseNo());
        address.setArea(request.getArea());
        address.setLandmark(request.getLandmark());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setDefaultAddress(request.isDefaultAddress());

        return addressRepository.save(address);
    }

    @Override
    public List<Address> getAddresses(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Not Found"));
        return addressRepository.findByCustomer(customer);
    }

    private Address getOwnedAddress(Long customerId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address Not Found"));
        if (!address.getCustomer().getId().equals(customerId)) {
            throw new UnauthorizedException("This is not your address");
        }
        return address;
    }

    @Override
    public Address updateAddress(Long customerId, Long addressId, AddAddressRequest request) {
        Address address = getOwnedAddress(customerId, addressId);

        address.setFullName(request.getFullName());
        address.setMobileNumber(request.getMobileNumber());
        address.setHouseNo(request.getHouseNo());
        address.setArea(request.getArea());
        address.setLandmark(request.getLandmark());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setDefaultAddress(request.isDefaultAddress());

        return addressRepository.save(address);
    }

    @Override
    public void deleteAddress(Long customerId, Long addressId) {
        Address address = getOwnedAddress(customerId, addressId);
        addressRepository.delete(address);   // pehle ye line missing thi
    }

    @Override
    public Address setDefaultAddress(Long customerId, Long addressId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Not Found"));

        List<Address> addresses = addressRepository.findByCustomer(customer);
        for (Address address : addresses) {
            address.setDefaultAddress(address.getId().equals(addressId));
        }
        addressRepository.saveAll(addresses);

        return getOwnedAddress(customerId, addressId);
    }
}