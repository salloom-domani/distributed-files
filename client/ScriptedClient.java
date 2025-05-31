package client;

import java.rmi.Naming;
import java.nio.file.*;
import java.util.*;

import coordinator.CoordinatorInterface;

public class ScriptedClient {
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: java ScriptedClient <script.txt>");
      return;
    }

    try {
      CoordinatorInterface coordinator = (CoordinatorInterface) Naming.lookup("rmi://localhost/CoordinatorService");
      List<String> lines = Files.readAllLines(Paths.get(args[0]));
      String tokenId = null;

      for (String line : lines) {
        String[] parts = line.trim().split(" ");
        String command = parts[0];

        switch (command) {
          case "register" -> {
            String res = coordinator.registerUser(parts[1], parts[2], parts[3]);
            System.out.println("[register] " + res);
          }
          case "login" -> {
            tokenId = coordinator.login(parts[1]);
            System.out.println("[login] Token: " + tokenId);
          }
          case "upload" -> {
            if (tokenId == null)
              break;
            Path path = Paths.get(parts[1]);
            byte[] content = Files.readAllBytes(path);
            String res = coordinator.addFile(tokenId, path.getFileName().toString(), content);
            System.out.println("[upload] " + res);
          }
          case "view" -> {
            if (tokenId == null)
              break;
            byte[] content = coordinator.viewFile(tokenId, parts[1]);
            if (content != null) {
              Files.write(Paths.get("client_download_" + parts[1]), content);
              System.out.println("[view] Downloaded " + parts[1]);
            } else {
              System.out.println("[view] Not found: " + parts[1]);
            }
          }
          case "delete" -> {
            if (tokenId == null)
              break;
            String res = coordinator.deleteFile(tokenId, parts[1]);
            System.out.println("[delete] " + res);
          }
          case "sleep" -> {
            Thread.sleep(Integer.parseInt(parts[1]));
          }
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
