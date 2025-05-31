package node;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class SyncServer {

  private final int nodeId;
  private final int port;
  private final Path baseDir;

  public SyncServer(int nodeId) {
    this.nodeId = nodeId;
    this.port = 6000 + nodeId; // e.g., Node 1 → 6001
    this.baseDir = Paths.get("storage/node_" + nodeId);
  }

  public void start() {
    Executors.newSingleThreadExecutor().submit(() -> {
      try (ServerSocket serverSocket = new ServerSocket(port)) {
        System.out.println("📡 SyncServer started on port " + port);
        while (true) {
          Socket clientSocket = serverSocket.accept();
          new Thread(() -> handleClient(clientSocket)).start();
        }
      } catch (IOException e) {
        System.err.println("❌ Failed to start SyncServer: " + e.getMessage());
      }
    });
  }

  private void handleClient(Socket socket) {
    try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

      String command = (String) in.readObject();

      if ("READ".equals(command)) {
        String relPath = (String) in.readObject();
        Path fullPath = baseDir.resolve(relPath);
        if (Files.exists(fullPath)) {
          byte[] content = Files.readAllBytes(fullPath);
          out.writeObject(content);
          System.out.println("📤 Sent file " + relPath + " to peer");
        } else {
          out.writeObject(null);
          System.out.println("⚠️ File not found: " + relPath);
        }
      }

    } catch (IOException | ClassNotFoundException e) {
      System.err.println("❌ Error handling sync request: " + e.getMessage());
    }
  }
}
