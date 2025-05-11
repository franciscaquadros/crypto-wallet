package com.swisspost.challenge.crypto_wallet.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Table(name = "wallet_asset")
public class WalletAsset {

  @Id
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "asset_id", nullable = false)
  private Asset asset;

  @ManyToOne
  @ToString.Exclude
  @JoinColumn(name = "wallet_id", nullable = false)
  private Wallet wallet;

  private Double quantity;

  @PrePersist
  public void generateId() {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }
  }
}
