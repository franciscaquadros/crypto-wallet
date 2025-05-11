package com.swisspost.challenge.crypto_wallet.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleIllegalArgument(final IllegalArgumentException ex, final HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(AssetNotFoundException.class)
  public ResponseEntity<ApiError> handleAssetNotFound(final AssetNotFoundException ex, final HttpServletRequest request) {
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(WalletNotFoundException.class)
  public ResponseEntity<ApiError> handleWalletNotFound(final WalletNotFoundException ex, final HttpServletRequest request) {
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(final Exception ex, final HttpServletRequest request) {
    ex.printStackTrace(); // Or use a logger
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI());
  }

  private ResponseEntity<ApiError> buildResponse(final HttpStatus status, final String message, final String path) {
    return ResponseEntity.status(status).body(
        new ApiError(
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            LocalDateTime.now()
        )
    );
  }
}
