package coordinator;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import common.Token;
import common.User;
import node.NodeInterface;

public class Coordinator extends UnicastRemoteObject implements CoordinatorInterface {

  private Map<String, User> users = new HashMap<>();
  private Map<String, Token> activeTokens = new HashMap<>();

  private List<NodeInterface> nodes = new ArrayList<>();

  // Global file log: department/filename -> FileMeta
  private Map<String, FileMeta> globalFileLog = new ConcurrentHashMap<>();

  protected Coordinator() throws RemoteException {
    super();
  }

  @Override
  public String registerUser(String username, String department, String role) throws RemoteException {
    if (users.containsKey(username))
      return "User already exists.";
    users.put(username, new User(username, department, role));
    return "User registered successfully.";
  }

  @Override
  public String login(String username) throws RemoteException {
    if (!users.containsKey(username))
      return "Invalid: User not found.";
    Token token = new Token(username);
    activeTokens.put(token.getTokenId(), token);
    return token.getTokenId();
  }

  @Override
  public String addFile(String tokenId, String filename, byte[] content) throws RemoteException {
    Token token = activeTokens.get(tokenId);
    if (token == null)
      return "Invalid token.";
    User user = users.get(token.getUsername());
    if (user == null)
      return "Invalid user.";

    String dept = user.getDepartment();
    NodeInterface node = getNodeForUser(user.getUsername());
    if (node == null)
      return "No nodes available.";

    String result = node.saveFile(dept, filename, content);

    // ✅ Update global file log
    String fullPath = dept + "/" + filename;
    globalFileLog.put(fullPath, new FileMeta(System.currentTimeMillis(), false));
    return result;
  }

  @Override
  public String listFiles(String tokenId) throws RemoteException {
    Token token = activeTokens.get(tokenId);
    if (token == null)
      return "Invalid token.";
    User user = users.get(token.getUsername());
    if (user == null)
      return "Invalid user.";

    String dept = user.getDepartment();
    NodeInterface node = getNodeForUser(user.getUsername());
    if (node == null)
      return "No nodes available.";

    return node.listFiles(dept);
  }

  @Override
  public byte[] viewFile(String tokenId, String filename) throws RemoteException {
    Token token = activeTokens.get(tokenId);
    if (token == null)
      return null;
    User user = users.get(token.getUsername());
    if (user == null)
      return null;

    String dept = user.getDepartment();
    for (NodeInterface node : nodes) {
      try {
        byte[] content = node.readFile(dept, filename);
        if (content != null)
          return content;
      } catch (Exception ignored) {
      }
    }
    return null;
  }

  @Override
  public String deleteFile(String tokenId, String filename) throws RemoteException {
    Token token = activeTokens.get(tokenId);
    if (token == null)
      return "Invalid token.";
    User user = users.get(token.getUsername());
    if (user == null)
      return "Invalid user.";

    String dept = user.getDepartment();
    NodeInterface node = getNodeForUser(user.getUsername());
    if (node == null)
      return "No nodes available.";
    String result = node.deleteFile(dept, filename);

    String fullPath = dept + "/" + filename;
    globalFileLog.put(fullPath, new FileMeta(System.currentTimeMillis(), true));
    return result;
  }

  @Override
  public void registerNode(NodeInterface node) throws RemoteException {
    nodes.add(node);
    System.out.print("Connected to a Node, New Count: " + nodes.size() + "\n> ");
  }

  @Override
  public void unregisterNode(NodeInterface node) throws RemoteException {
    nodes.remove(node);
    System.out.print("Disconnected from a Node, New Count: " + nodes.size() + "\n> ");
  }

  @Override
  public void synchronizeNodes() throws RemoteException {
    System.out.println("🔄 Starting synchronization...");

    for (NodeInterface node : nodes) {
      Map<String, Long> nodeFiles;
      try {
        nodeFiles = node.getFileTimestamps();
      } catch (Exception e) {
        System.out.println("⚠️ Failed to fetch files from Node " + node.getNodeId());
        continue;
      }

      // 1. Sync files TO node (based on global log)
      for (Map.Entry<String, FileMeta> entry : globalFileLog.entrySet()) {
        String path = entry.getKey();
        FileMeta meta = entry.getValue();

        if (meta.deleted) {
          if (nodeFiles.containsKey(path)) {
            try {
              node.deleteFileByPath(path);
              System.out.println("🗑️ Deleted " + path + " from Node " + node.getNodeId());
            } catch (Exception e) {
              System.out.println("❌ Failed to delete " + path + " on Node " + node.getNodeId());
            }
          }
          continue;
        }

        long nodeTimestamp = nodeFiles.getOrDefault(path, 0L);
        if (nodeTimestamp < meta.lastModified) {
          byte[] content = getFileFromAnyNode(path);
          if (content != null) {
            node.saveFileByPath(path, content);
            System.out.println("📤 Sent updated " + path + " to Node " + node.getNodeId());
          } else {
            System.out.println("❌ Could not find content for " + path + " in any node");
          }
        }
      }

      // 2. Sync files FROM node (new or updated files not in global log)
      for (Map.Entry<String, Long> entry : nodeFiles.entrySet()) {
        String path = entry.getKey();
        long timestamp = entry.getValue();

        FileMeta current = globalFileLog.get(path);
        if (current == null || (current.lastModified < timestamp && !current.deleted)) {
          try {
            byte[] content = node.readFileByPath(path);
            if (content != null) {
              globalFileLog.put(path, new FileMeta(timestamp, false));
              System.out.println("📥 Updated " + path + " from Node " + node.getNodeId());
            }
          } catch (Exception e) {
            System.out.println("❌ Failed to read new file " + path + " from Node " + node.getNodeId());
          }
        }
      }
    }

    System.out.println("✅ Synchronization complete.");
  }

  private byte[] getFileFromAnyNode(String path) {
    for (NodeInterface node : nodes) {
      try {
        byte[] content = node.readFileByPath(path);
        if (content != null)
          return content;
      } catch (Exception ignored) {
      }
    }
    return null;
  }

  private NodeInterface getNodeForUser(String username) {
    if (nodes.isEmpty())
      return null;
    int index = Math.abs(username.hashCode()) % nodes.size();
    return nodes.get(index);
  }

  // Nested metadata class
  private static class FileMeta {
    long lastModified;
    boolean deleted;

    FileMeta(long lastModified, boolean deleted) {
      this.lastModified = lastModified;
      this.deleted = deleted;
    }
  }
}
