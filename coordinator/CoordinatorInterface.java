package coordinator;

import java.rmi.Remote;
import java.rmi.RemoteException;

import node.NodeInterface;

public interface CoordinatorInterface extends Remote {
  // Node registration
  void registerNode(NodeInterface node) throws RemoteException;

  void unregisterNode(NodeInterface node) throws RemoteException;

  void synchronizeNodes() throws RemoteException;

  // Login/Register
  String registerUser(String username, String department, String role) throws RemoteException;

  String login(String username) throws RemoteException;

  // File operations (we’ll implement these later)
  String listFiles(String tokenId) throws RemoteException;

  String addFile(String tokenId, String filename, byte[] content) throws RemoteException;

  byte[] viewFile(String tokenId, String filename) throws RemoteException;

  String deleteFile(String tokenId, String filename) throws RemoteException;
}
