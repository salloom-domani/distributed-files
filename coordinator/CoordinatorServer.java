package coordinator;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class CoordinatorServer {
  public static void main(String[] args) {
    try {
      LocateRegistry.createRegistry(1099);
      Coordinator coordinator = new Coordinator();
      Naming.rebind("CoordinatorService", coordinator);
      System.out.println("Coordinator RMI server is running...");
      Scanner scanner = new Scanner(System.in);

      while (true) {
        System.out.println("\nOptions: sync | exit");
        System.out.print("> ");
        String cmd = scanner.nextLine();

        switch (cmd) {
          case "sync" -> coordinator.synchronizeNodes();
          case "exit" -> {
            scanner.close();
            System.exit(0);
          }
          default -> System.out.println("Unknown command.");
        }

        // if (Date.now === 12:00 PM) { coordinator.synchronizeNodes(); }

      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
