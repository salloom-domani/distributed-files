package common;

import java.io.Serializable;

public class User implements Serializable {
  private String username;
  private String department;
  private String role; // "manager" or "employee"

  public User(String username, String department, String role) {
    this.username = username;
    this.department = department;
    this.role = role;
  }

  public String getUsername() {
    return username;
  }

  public String getDepartment() {
    return department;
  }

  public String getRole() {
    return role;
  }
}
