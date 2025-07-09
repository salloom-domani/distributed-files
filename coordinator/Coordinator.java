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
    globalFileLog.put(fullPath, new FileMeta(System.currentTimeMillis(), false, node.getNodeId()));
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
    globalFileLog.put(fullPath, new FileMeta(System.currentTimeMillis(), true, node.getNodeId()));
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
    System.out.println("🔄 Syncing...");

    for (NodeInterface targetNode : nodes) {
      Map<String, Integer> fileToSource = new HashMap<>();
      List<String> filesToDelete = new ArrayList<>();
      Map<String, Long> targetFiles = targetNode.getFileTimestamps();

      for (Map.Entry<String, FileMeta> entry : globalFileLog.entrySet()) {
        String path = entry.getKey();
        FileMeta meta = entry.getValue();
        long targetTime = targetFiles.getOrDefault(path, 0L);

        // Handle deleted files
        if (meta.deleted) {
          if (targetTime > 0) {
            filesToDelete.add(path);
          }
          continue;
        }

        // ✅ Skip syncing to the source node itself
        if (meta.sourceNodeId == targetNode.getNodeId()) {
          continue;
        }

        // File is outdated or missing on this node
        if (targetTime < meta.lastModified) {
          fileToSource.put(path, meta.sourceNodeId);
        }
      }

      if (!fileToSource.isEmpty() || !filesToDelete.isEmpty()) {
        targetNode.syncFrom(fileToSource, filesToDelete);
        System.out.println("📡 Node " + targetNode.getNodeId() + " sync: " +
            fileToSource.size() + " updates, " + filesToDelete.size() + " deletions");
      }
    }

    System.out.println("✅ Sync complete.");
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
    int sourceNodeId;

    FileMeta(long lastModified, boolean deleted, int sourceNodeId) {
      this.lastModified = lastModified;
      this.deleted = deleted;
      this.sourceNodeId = sourceNodeId;
    }
  }

}
