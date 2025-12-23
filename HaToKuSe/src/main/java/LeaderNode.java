import java.io.*;
import java.net.*;

public class LeaderNode {
    private static final int TCP_PORT = 8080;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            System.out.println("Lider Sunucu başlatıldı. TCP Port: " + TCP_PORT);

            while (true) {
                // Yeni bir istemci bağlandığında kabul et
                Socket clientSocket = serverSocket.accept();
                System.out.println("Yeni bir istemci bağlandı: " + clientSocket.getInetAddress());

                // Her istemciyi ayrı bir thread'de yönet (Multi-threading)
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Sunucu başlatılamadı: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("İstemciden gelen ham mesaj: " + line);

                // Mesajı parçala: SET, 100, Mesaj İçeriği
                String[] tokens = line.split(" ", 3);
                String command = tokens[0].toUpperCase();

                if (command.equals("SET") && tokens.length == 3) {
                    String id = tokens[1];
                    String message = tokens[2];

                    // TODO: Burada gRPC ile Worker Node'lara yayın yapacağız
                    System.out.println("Komut: SET, ID: " + id + ", Mesaj: " + message);
                    out.println("OK: Mesaj alindi ve dagitiliyor...");

                } else if (command.equals("GET") && tokens.length == 2) {
                    String id = tokens[1];
                    System.out.println("Komut: GET, ID: " + id);
                    out.println("OK: ID " + id + " icin veri sorgulaniyor...");

                } else {
                    out.println("ERROR: Gecersiz komut formati.");
                }
            }
        } catch (IOException e) {
            System.err.println("İstemci hatası: " + e.getMessage());
        }
    }
}