package node;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;

public class SyncServer implements Runnable {
  private int port;
  private int nodeId;

  public SyncServer(int port, int nodeId) {
    this.port = port;
    this.nodeId = nodeId;
  }

  @Override
  public void run() {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("SyncServer running on Node " + nodeId + " (port " + port + ")");
      while (true) {
        try (Socket socket = serverSocket.accept();
            DataInputStream in = new DataInputStream(socket.getInputStream())) {

          int fileCount = in.readInt();
          for (int i = 0; i < fileCount; i++) {
            String dept = in.readUTF();
            String filename = in.readUTF();
            int size = in.readInt();
            byte[] content = new byte[size];
            in.readFully(content);

            Path targetDir = Paths.get("storage/node_" + nodeId, dept);
            Files.createDirectories(targetDir);
            Files.write(targetDir.resolve(filename), content);
          }
          System.out.println("Node " + nodeId + " synced with a peer.");
        } catch (Exception e) {
          System.err.println("Error during sync on Node " + nodeId + ": " + e.getMessage());
        }
      }
    } catch (IOException e) {
      System.err.println("SyncServer failed: " + e.getMessage());
    }
  }
}
