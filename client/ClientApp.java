package client;

import java.rmi.Naming;
import java.util.Scanner;

import coordinator.CoordinatorInterface;

import java.nio.file.*;

public class ClientApp {
  private static CoordinatorInterface coordinator;
  private static String tokenId = null;

  public static void main(String[] args) {
    try {
      coordinator = (CoordinatorInterface) Naming.lookup("rmi://localhost/CoordinatorService");
      Scanner scanner = new Scanner(System.in);

      while (true) {
        if (tokenId == null) {
          System.out.println("\nOptions: register | login | exit");
        } else {
          System.out.println("\nOptions: logout | upload | list | view | delete | exit");
        }
        System.out.print("> ");
        String line = scanner.nextLine().trim();
        String[] params = line.split("\\s+", 2); // split into command and the rest

        String cmd = params[0];

        switch (cmd) {
          case "register" -> register(scanner);
          case "login" -> login(scanner);
          case "logout" -> logout(scanner);
          case "upload" -> upload(scanner, params.length > 1 ? params[1] : null);
          case "list" -> list(scanner);
          case "view" -> view(scanner, params.length > 1 ? params[1] : null);
          case "delete" -> delete(scanner, params.length > 1 ? params[1] : null);
          case "exit" -> System.exit(0);
          default -> System.out.println("Unknown command.");
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void register(Scanner scanner) throws Exception {
    System.out.print("Username: ");
    String username = scanner.nextLine();
    System.out.print("Department: ");
    String department = scanner.nextLine();
    System.out.print("Role (manager/employee): ");
    String role = scanner.nextLine();

    String result = coordinator.registerUser(username, department, role);
    System.out.println(result);
  }

  private static void login(Scanner scanner) throws Exception {
    System.out.print("Username: ");
    String username = scanner.nextLine();
    tokenId = coordinator.login(username);
    if (tokenId.startsWith("Invalid")) {
      System.out.println("Login failed.");
      tokenId = null;
    } else {
      System.out.println("Logged in. Token: " + tokenId);
    }
  }

  private static void logout(Scanner scanner) throws Exception {
    if (tokenId == null) {
      System.out.println("Please login first.");
      return;
    }
    tokenId = null;
  }

  private static void upload(Scanner scanner, String fileArg) throws Exception {
    if (tokenId == null) {
      System.out.println("Please login first.");
      return;
    }

    String filePath;
    if (fileArg != null) {
      filePath = fileArg;
    } else {
      System.out.print("File path: ");
      filePath = scanner.nextLine().trim();
    }

    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      System.out.println("File not found.");
      return;
    }

    byte[] content = Files.readAllBytes(path);
    String result = coordinator.addFile(tokenId, path.getFileName().toString(), content);
    System.out.println(result);
  }

  private static void list(Scanner scanner) throws Exception {
    if (tokenId == null) {
      System.out.println("Please login first.");
      return;
    }
    String result = coordinator.listFiles(tokenId);
    System.out.println(result);
  }

  private static void view(Scanner scanner, String filename) throws Exception {
    if (tokenId == null) {
      System.out.println("Please login first.");
      return;
    }
    if (filename == null) {
      System.out.print("Filename to view: ");
      filename = scanner.nextLine().trim();
    }
    byte[] content = coordinator.viewFile(tokenId, filename);
    if (content != null) {
      System.out.print(new String(content));
    } else {
      System.out.println("File not found.");
    }
  }

  private static void delete(Scanner scanner, String filename) throws Exception {
    if (tokenId == null) {
      System.out.println("Please login first.");
      return;
    }
    if (filename == null) {
      System.out.print("Filename to delete: ");
      filename = scanner.nextLine().trim();
    }
    String result = coordinator.deleteFile(tokenId, filename);
    System.out.println(result);
  }
}
