package node;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface NodeInterface extends Remote {
  int getNodeId() throws RemoteException;

  String saveFile(String department, String filename, byte[] content) throws RemoteException;

  String listFiles(String department) throws RemoteException;

  byte[] readFile(String department, String filename) throws RemoteException;

  String deleteFile(String department, String filename) throws RemoteException;

  Map<String, Long> getFileTimestamps() throws RemoteException;

  void syncFrom(Map<String, Integer> fileToSource, List<String> filesToDelete) throws RemoteException;
}
