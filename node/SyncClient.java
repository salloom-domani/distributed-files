package node;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.List;

public class SyncClient {
  public static void syncWith(int nodeId) {
    try (Socket socket = new Socket("localhost", 3000 + nodeId);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

      Path root = Paths.get("storage/node_" + nodeId);
      List<Path> files = Files.walk(root)
          .filter(Files::isRegularFile)
          .toList();

      out.writeInt(files.size());

      for (Path file : files) {
        Path deptPath = root.relativize(file.getParent());
        String dept = deptPath.toString();
        byte[] content = Files.readAllBytes(file);

        out.writeUTF(dept);
        out.writeUTF(file.getFileName().toString());
        out.writeInt(content.length);
        out.write(content);
      }

      System.out.println("Node " + nodeId + " synced to " + nodeId);
    } catch (IOException e) {
      System.err.println("Sync failed from Node " + nodeId + ": " + e.getMessage());
    }
  }
}
