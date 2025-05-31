package node;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.List;

public class SyncClient {
  public static byte[] fetchFileViaSocket(int sourceNodeId, String filePath) {
    try (Socket socket = new Socket("localhost", 6000 + sourceNodeId);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

      out.writeObject("READ");
      out.writeObject(filePath);

      Object response = in.readObject();
      if (response instanceof byte[]) {
        return (byte[]) response;
      }

    } catch (IOException | ClassNotFoundException e) {
      System.err.println("❌ Error fetching file: " + e.getMessage());
    }
    return null;
  }

  public static void saveFileLocally(String relativePath, byte[] content, int nodeId) {
    try {
      Path baseDir = Paths.get("storage/node_" + nodeId);
      Path fullPath = baseDir.resolve(relativePath);
      Files.createDirectories(fullPath.getParent());
      Files.write(fullPath, content);
      System.out.println("✔ Saved " + relativePath + " to node_" + nodeId);
    } catch (IOException e) {
      System.err.println("❌ Error saving file: " + e.getMessage());
    }
  }

  public static void syncFilesFromPeer(int sourceNodeId, int currentNodeId, List<String> filesToSync) {
    for (String filePath : filesToSync) {
      try {
        byte[] content = fetchFileViaSocket(sourceNodeId, filePath);
        if (content != null) {
          saveFileLocally(filePath, content, currentNodeId);
          System.out.println("✔ Synced " + filePath + " from Node " + sourceNodeId);
        } else {
          System.out.println("✘ Failed to get " + filePath + " from Node " + sourceNodeId);
        }
      } catch (Exception e) {
        System.out.println("❌ Error syncing " + filePath + ": " + e.getMessage());
      }
    }
  }
}
