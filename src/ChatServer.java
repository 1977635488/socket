import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static Map<String, PrintWriter> clientMap = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 获取并广播用户名
                out.println("Enter your username: ");
                username = in.readLine();

                synchronized (clientWriters) {
                    clientWriters.add(out);
                    clientMap.put(username, out);
                }

                broadcast("Server", username + " has joined the chat!");
                sendUserList();

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println(username + ": " + message);
                    broadcast(username, message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {//清理与该客户端相关的资源。
                if (out != null) {
                    synchronized (clientWriters) {
                        clientWriters.remove(out);
                        clientMap.remove(username);
                    }
                }
                broadcast("Server", username + " has left the chat.");
                sendUserList();
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcast(String sender, String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(sender + ": " + message);
                }
            }
        }

        private void sendUserList() {
            StringBuilder userList = new StringBuilder("Server: Current users: ");
            synchronized (clientWriters) {
                for (String user : clientMap.keySet()) {
                    userList.append(user).append(", ");
                }
                if (userList.length() > 2) {
                    userList.setLength(userList.length() - 2); // 移除末尾多余的逗号
                }
            }
            broadcast("Server", userList.toString());
        }
    }
}
