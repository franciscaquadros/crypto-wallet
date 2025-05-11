package com.swisspost.challenge.crypto_wallet.exception;

public class WalletNotFoundException extends IllegalArgumentException {
  public WalletNotFoundException(final String message) {
    super(message);
  }
}
