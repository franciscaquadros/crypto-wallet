package com.swisspost.challenge.crypto_wallet.repository;

import com.swisspost.challenge.crypto_wallet.repository.entity.Wallet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

  Optional<Wallet> findByCustomerEmailIgnoreCase(String email);
}
