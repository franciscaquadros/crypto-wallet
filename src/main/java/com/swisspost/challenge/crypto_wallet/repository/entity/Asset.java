package com.swisspost.challenge.crypto_wallet.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "asset")
public class Asset {

  @Id
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(unique = true, nullable = false)
  private String symbol;

  @Column(unique = true, nullable = false)
  private BigDecimal price;

  @PrePersist
  public void generateId() {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }
  }
}
