package com.github.searchindex.exception;

public class IndexingException extends RuntimeException {

  public IndexingException(String message) {
    super(message);
  }

  public IndexingException(String message, Throwable cause) {
    super(message, cause);
  }

}
