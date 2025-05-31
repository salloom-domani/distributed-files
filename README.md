# How to run

- Compile java code with:

  ```sh
  javac **/*.java
  ```

- Start coordinator server by:

  ```sh
  java coordinator.CoordinatorServer
  ```

- Run as many nodes as you want by:

  ```sh
  java node.NodeServer <nodeId>
  ```

  The nodes will automatically regiseter themeselfs to the coordinaotr
- Run client app by:

  ```sh
  java client.ClientApp
  ```

- Voilà. you can start by registering then logging in.
- Upload files with thier relative path and go the storage directory to see the file being uploaded.
- Now you can sync the files through coordiantor server.
- See the file being syned across nodes.
