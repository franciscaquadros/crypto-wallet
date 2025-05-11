package com.swisspost.challenge.crypto_wallet.exception;

public class AssetNotFoundException extends IllegalArgumentException {
  public AssetNotFoundException(final String message) {
    super(message);
  }
}
