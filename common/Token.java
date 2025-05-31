package common;

import java.io.Serializable;
import java.util.UUID;

public class Token implements Serializable {
  private String tokenId;
  private String username;

  public Token(String username) {
    this.username = username;
    this.tokenId = UUID.randomUUID().toString();
  }

  public String getTokenId() {
    return tokenId;
  }

  public String getUsername() {
    return username;
  }
}
