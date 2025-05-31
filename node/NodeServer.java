package node;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import coordinator.CoordinatorInterface;

public class NodeServer {
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: java NodeLauncher <NodeId>");
      return;
    }
    try {
      int nodeId = Integer.parseInt(args[0]);
      MyNode node = new MyNode(nodeId);
      Naming.rebind("Node" + nodeId, node);
      System.out.println("Node " + nodeId + " is running.");

      CoordinatorInterface coordinator = (CoordinatorInterface) Naming.lookup("//localhost:1099/CoordinatorService");
      coordinator.registerNode(node);

      Runtime.getRuntime().addShutdownHook(new ShutDownHookThread(node));

      // At the bottom of NodeLauncher.java after binding the node
      int syncPort = 3000 + nodeId;
      new Thread(new SyncServer(syncPort, nodeId)).start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

class ShutDownHookThread extends Thread {
  MyNode node;

  ShutDownHookThread(MyNode node) {
    this.node = node;
  }

  @Override
  public void run() {
    // ***write your code here to handle any shutdown request

    CoordinatorInterface coordinator;
    try {
      coordinator = (CoordinatorInterface) Naming.lookup("//localhost:1099/CoordinatorService");
      coordinator.unregisterNode(node);
    } catch (RemoteException | MalformedURLException | NotBoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    System.out.println("Shut Down Node");
    super.run();
  }
}
