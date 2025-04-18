package com.github.searchindex.lucene.core;

import lombok.Getter;

@Getter
public enum IndexType {

  DICTIONARY("dictionary"),
  TWEETS("tweets");

  private final String name;

  IndexType(String name) {
    this.name = name;
  }

}
