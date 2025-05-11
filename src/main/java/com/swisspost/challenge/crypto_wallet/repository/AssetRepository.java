package com.swisspost.challenge.crypto_wallet.repository;

import com.swisspost.challenge.crypto_wallet.repository.entity.Asset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {

  Optional<Asset> findBySymbolIgnoreCase(String symbol);
}
