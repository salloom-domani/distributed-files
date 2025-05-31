package node;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class MyNode extends UnicastRemoteObject implements NodeInterface {

  private final int nodeId;
  private final Path baseDir;

  private final Map<String, ReentrantReadWriteLock> fileLocks = new ConcurrentHashMap<>();

  protected MyNode(int nodeId) throws RemoteException, IOException {
    super();
    this.nodeId = nodeId;
    this.baseDir = Paths.get("storage/node_" + nodeId);
    Files.createDirectories(baseDir);
  }

  private Path getDeptPath(String department) {
    return baseDir.resolve(department);
  }

  private ReentrantReadWriteLock getLock(String relativePath) {
    return fileLocks.computeIfAbsent(relativePath, _ -> new ReentrantReadWriteLock());
  }

  @Override
  public int getNodeId() {
    return nodeId;
  }

  @Override
  public String saveFile(String department, String filename, byte[] content) {

    Path dir = getDeptPath(department);
    try {
      Files.createDirectories(dir);
    } catch (IOException e) {
      e.printStackTrace();
    }
    Path filePath = dir.resolve(filename);
    String relPath = baseDir.relativize(filePath).toString();
    ReentrantReadWriteLock.WriteLock writeLock = getLock(relPath).writeLock();
    writeLock.lock();

    try {
      Files.write(filePath, content);
      return "File saved at Node " + nodeId;
    } catch (IOException e) {
      return "Error saving file: " + e.getMessage();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public byte[] readFile(String department, String filename) {
    Path filePath = getDeptPath(department).resolve(filename);
    String relPath = baseDir.relativize(filePath).toString();
    ReentrantReadWriteLock.ReadLock readLock = getLock(relPath).readLock();
    readLock.lock();
    try {
      return Files.readAllBytes(filePath);
    } catch (IOException e) {
    } finally {
      readLock.unlock();
    }
    return null;
  }

  @Override
  public String listFiles(String department) {
    try {
      Path dir = getDeptPath(department);
      if (Files.notExists(dir)) {
        return "No files in this department.";
      }
      List<String> files = Files.walk(dir)
          .filter(Files::isRegularFile)
          .map(Path::getFileName)
          .map(Path::toString)
          .toList();
      if (files.isEmpty()) {
        return "No files in this department.";
      }
      return String.join("\n", files);
    } catch (IOException e) {
      return "Error listing files: " + e.getMessage();
    }
  }

  @Override
  public String deleteFile(String department, String filename) {
    try {
      Path filePath = getDeptPath(department).resolve(filename);
      Files.deleteIfExists(filePath);
      return "File deleted from Node " + nodeId;
    } catch (IOException e) {
      return "Error deleting file: " + e.getMessage();
    }
  }

  @Override
  public Map<String, Long> getFileTimestamps() {
    Map<String, Long> result = new HashMap<>();
    try (Stream<Path> paths = Files.walk(baseDir)) {
      paths.filter(Files::isRegularFile)
          .forEach(p -> {
            try {
              String relative = baseDir.relativize(p).toString();
              long modTime = Files.getLastModifiedTime(p).toMillis();
              result.put(relative, modTime);
            } catch (IOException ignored) {
            }
          });
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  @Override
  public void syncFrom(Map<String, Integer> fileToSource, List<String> filesToDelete) throws RemoteException {
    new Thread(() -> {
      try {
        for (Map.Entry<String, Integer> entry : fileToSource.entrySet()) {
          String filePath = entry.getKey();
          int sourceNodeId = entry.getValue();
          byte[] content = SyncClient.fetchFileViaSocket(sourceNodeId, filePath);
          if (content != null) {
            SyncClient.saveFileLocally(filePath, content, nodeId);
          }
        }

        for (String filePath : filesToDelete) {
          Path fullPath = Paths.get("storage/node_" + nodeId).resolve(filePath);
          try {
            Files.deleteIfExists(fullPath);
            System.out.println("🗑️ Deleted stale file: " + filePath);
          } catch (IOException e) {
            System.err.println("❌ Could not delete " + filePath);
          }
        }

      } catch (Exception e) {
        System.err.println("❌ Sync error: " + e.getMessage());
      }
    }).start();
  }

}
