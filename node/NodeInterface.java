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

  void synchronizeWithPeers(List<Integer> ids) throws RemoteException;

  Map<String, Long> getFileTimestamps() throws RemoteException;

  byte[] readFileByPath(String path) throws RemoteException;

  String saveFileByPath(String path, byte[] content) throws RemoteException;

  String deleteFileByPath(String path) throws RemoteException;
}
