package com.swisspost.challenge.crypto_wallet.repository.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wallet")
public class Wallet {

  @Id
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(name = "wallet_id", unique = true, nullable = false)
  private UUID walletId;

  @OneToOne
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ToString.Exclude
  @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<WalletAsset> walletAssets;

  @PrePersist
  public void generateId() {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }
  }
}
