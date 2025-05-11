package com.swisspost.challenge.crypto_wallet.repository;

import com.swisspost.challenge.crypto_wallet.repository.entity.Asset;
import com.swisspost.challenge.crypto_wallet.repository.entity.Wallet;
import com.swisspost.challenge.crypto_wallet.repository.entity.WalletAsset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletAssetRepository extends JpaRepository<WalletAsset, UUID> {

  List<WalletAsset> findDistinctByAssetIsNotNull();

  Optional<WalletAsset> findByWalletAndAsset(final Wallet wallet, final Asset asset);
}
