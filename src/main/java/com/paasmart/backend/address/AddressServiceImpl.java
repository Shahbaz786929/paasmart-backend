package com.paasmart.backend.address;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.order.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired private AddressRepository addressRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;

    @Override
    public Address addAddress(Long customerId, AddAddressRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Not Found"));

        List<Address> existing = addressRepository.findByCustomer(customer);
        boolean makeDefault = request.isDefaultAddress() || existing.isEmpty();
        if (makeDefault) {
            for (Address a : existing) {
                a.setDefaultAddress(false);
            }
            addressRepository.saveAll(existing);
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
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setDefaultAddress(makeDefault);

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
    @Transactional
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
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        if (request.isDefaultAddress()) {
            List<Address> others = addressRepository.findByCustomer(address.getCustomer());
            for (Address other : others) {
                if (!other.getId().equals(addressId)) {
                    other.setDefaultAddress(false);
                }
            }
            addressRepository.saveAll(others);
        }
        address.setDefaultAddress(request.isDefaultAddress());

        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long customerId, Long addressId) {
        Address address = getOwnedAddress(customerId, addressId);

        // Purane order isi address se jude hote hain; delete karne par DB error aata tha
        if (orderRepository.existsByAddress_Id(addressId)) {
            throw new BadRequestExceprion(
                    "This address is linked to a past order and can't be deleted. You can edit it instead.");
        }

        boolean wasDefault = address.isDefaultAddress();
        User customer = address.getCustomer();
        addressRepository.delete(address);

        // Default hat gaya to koi aur address default bana do
        if (wasDefault) {
            addressRepository.findByCustomer(customer).stream().findFirst().ifPresent(a -> {
                a.setDefaultAddress(true);
                addressRepository.save(a);
            });
        }
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