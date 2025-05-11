package com.swisspost.challenge.crypto_wallet.repository;

import com.swisspost.challenge.crypto_wallet.repository.entity.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  Optional<Customer> findByEmail(String email);
}
