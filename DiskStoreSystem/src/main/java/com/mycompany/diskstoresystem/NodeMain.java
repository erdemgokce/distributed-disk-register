package com.mycompany.diskstoresystem;

import com.mycompany.diskstoresystem.proto.DiskServiceGrpc;
import com.mycompany.diskstoresystem.proto.JoinRequest;
import com.mycompany.diskstoresystem.proto.JoinResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.mycompany.diskstoresystem.proto.Empty;


public class NodeMain {

    private static int MY_PORT;
    private static final int START_PORT = 5555;
    private static final int TCP_GATEWAY_PORT = 6666;
    private static boolean isLeader = false;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Logger logger = Logger.getLogger(com.mycompany.diskstoresystem.NodeMain.class.getName());

    public static void main(String[] args) {
        System.out.println("=== Distributed Disk Register System ===");

        // 1. ADIM: Portu bul ve gRPC Sunucuyu başlat
        startServerOnAvailablePort();

        // 2. ADIM: Role göre TCP Gateway veya Join başlat
        if (isLeader) {
            System.out.println("[ROL] LIDER: TCP Port 6666 dinleniyor...");
            startTcpGateway();
        } else {
            System.out.println("[ROL] UYE: Lider'e katilma istegi gonderiliyor...");
            joinFamily();
        }

        // 3. ADIM: Durum raporlama
        startFamilyReportingThread();

        // 4. ADIM: Heartbeat baslat
        startHeartbeat();
        // Konsoldan (Klavyeden) komut girmeyi sağlayan döngü
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n>>> KONSOL MODU AKTİF! Komut girebilirsin (Örn: SET k1 v1 veya GET k1)");

        while (true) {
            try {
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] parts = line.trim().split("\\s+");

                    if (parts.length == 0) continue;

                    String command = parts[0].toUpperCase();

                    if (command.equals("SET") && parts.length >= 3) {
                        // SET komutunu işle (Value boşluk içeriyorsa birleştir)
                        String key = parts[1];
                        String value = line.substring(line.indexOf(parts[2]));
                        FamilyServiceImpl.sendStoreToAll(key, value);
                        System.out.println("[KONSOL] Kayıt işlemi tetiklendi.");

                    } else if (command.equals("GET") && parts.length == 2) {
                        // GET komutunu işle
                        String result = FamilyServiceImpl.sendRetrieveRequest(parts[1]);
                        System.out.println("[KONSOL SONUÇ]: " + result);

                    } else if (command.equalsIgnoreCase("EXIT")) {
                        System.out.println("Çıkılıyor...");
                        System.exit(0);
                    } else {
                        System.out.println("HATA: Eksik veya yanlış komut. (SET key value / GET key)");
                    }
                }
            } catch (Exception e) {
                System.err.println("Komut işlenirken hata: " + e.getMessage());
            }
        }
    }

    private static void startServerOnAvailablePort() {
        int port = START_PORT;
        boolean started = false;

        while (!started && port < 5600) {
            try {
                Server server = ServerBuilder.forPort(port)
                        .addService(new FamilyServiceImpl(port))
                        .build()
                        .start();

                MY_PORT = port;
                isLeader = (MY_PORT == START_PORT);
                started = true;
                System.out.println("Node basariyla baslatildi. Port: " + MY_PORT);

                Thread serverThread = new Thread(() -> {
                    try {
                        server.awaitTermination();
                    } catch (InterruptedException e) {
                        System.err.println("Sunucu durdu.");
                    }
                });
                serverThread.start();

            } catch (IOException e) {
                port++;
            }
        }
    }

    private static void startTcpGateway() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(TCP_GATEWAY_PORT)) {
                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> {
                        String clientIp = socket.getInetAddress().getHostAddress();
                        try (Scanner in = new Scanner(socket.getInputStream());
                             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                            out.println("--- Distributed Disk Register Gateway'e Hosgeldiniz ---");

                            while (in.hasNextLine()) {
                                String line = in.nextLine();
                                // Gelen komutu log dosyasına kaydetme
                                logTcpRequest(clientIp, line);
                                if (line.equalsIgnoreCase("EXIT"))
                                    break;

                                String[] parts = line.split(" ", 3);
                                String command = parts[0].toUpperCase();

                                if (command.equals("SET") && parts.length == 3) {
                                    FamilyServiceImpl.sendStoreToAll(parts[1], parts[2]);
                                    out.println("OK - Kayit baslatildi.");
                                } else if (command.equals("GET") && parts.length == 2) {
                                    // Metot artik bize String donuyor, bunu istemciye yazdiralim
                                    String result = FamilyServiceImpl.sendRetrieveRequest(parts[1]);
                                    out.println(result);
                                } else {
                                    out.println("HATA: Gecersiz komut.");
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("TCP Baglantisi koptu.");
                        }
                    }).start();
                }
            } catch (IOException e) {
                System.err.println("TCP Gateway hatasi: " + e.getMessage());
            }
        }).start();
    }

    private static void logTcpRequest(String clientIp, String command) {
        File logFile = new File("tcp_requests.log");

        // Çoklu thread çakışmasını önlemek için synchronized kullanıyoruz
        synchronized (com.mycompany.diskstoresystem.NodeMain.class) {
            // Appened modunda(ture dediğimiz için appened modu oldu) FileWriter kullanıyoruz
            try (FileWriter fw = new FileWriter(logFile, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {

                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                // Dosyaya yazdırıyoruz
                out.println("[" + timeStamp + "] [" + clientIp + "] " + command);

            } catch (IOException e) {
                System.err.println("[LOG HATA] Dosyaya yazılamadı: " + e.getMessage());
            }
        }
    }

    private static void joinFamily() {
        new Thread(() -> {
            while (!isLeader) { // Lider olmadığım sürece bir lidere bağlanmaya çalış
                int leaderPort = START_PORT;
                boolean connected = false;

                // En küçük porttan başlayarak aktif lideri bul (Kendi portuna kadar)
                for (int p = START_PORT; p < MY_PORT; p++) {
                    if (isPortActive(p)) {
                        leaderPort = p;
                        connected = true;
                        break;
                    }
                }

                if (connected) {
                    try {
                        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", leaderPort)
                                .usePlaintext().build();
                        DiskServiceGrpc.DiskServiceBlockingStub stub = DiskServiceGrpc.newBlockingStub(channel);

                        // Yeni lidere kendini tanıtır
                        JoinResponse res = stub.join(JoinRequest.newBuilder().setPort(MY_PORT).build());
                        if (res.getSuccess()) {
                            System.out.println("[JOIN] Yeni Lidere (Port " + leaderPort + ") baglandim.");

                            // Lider düşene kadar burada bekle (Heartbeat)
                            while (isPortActive(leaderPort) && !isLeader) {
                                Thread.sleep(3000);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[JOIN] Baglanti koptu, yeniden deneniyor...");
                    }
                }
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
            }
        }).start();
    }

    private static void startFamilyReportingThread() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(20000); // 20 saniyede bir rapor ver
                    if (isLeader) {
                        System.out.println("\n========= SISTEM RAPORU (LIDER) =========");

                        // Liderin kendi mesajlarını say (messages klasöründeki dosya sayısı)
                        File folder = new File("messages");
                        int localCount = folder.exists() ? folder.list().length : 0;
                        System.out.println("Liderdeki Toplam Mesaj: " + localCount);

                        // Üyelerdeki mesajları yazdır
                        System.out.println("Üyelerin Mesaj Dağılımı:");
                        FamilyServiceImpl.printMemberStats(); // Bunu aşağıda tanımlayacağız
                        System.out.println("========================================\n");
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
    private static void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 1. ÜYE TAKİBİ: Bildiğimiz üyeler hala ayakta mı?
                for (Integer port : NodeRegistry.getMembers().keySet()) {
                    try {
                        DiskServiceGrpc.DiskServiceBlockingStub stub = NodeRegistry.getMembers().get(port);
                        stub.ping(Empty.newBuilder().build());
                    } catch (Exception e) {
                        System.err.println("Üye [port: " + port + "] ulaşılamıyor. Kaldırılıyor...");
                        NodeRegistry.removeNode("localhost", port);
                    }
                }

                // 2. LİDERLİK KONTROLÜ (Election Check)
                if (!isLeader) {
                    // Kural: Benden daha düşük (kıdemli) bir port aktif mi?
                    boolean smallerPortExists = false;
                    for (int p = START_PORT; p < MY_PORT; p++) {
                        if (isPortActive(p)) {
                            smallerPortExists = true;
                            break; // Benden kıdemli biri var, liderlik için beklemeye devam.
                        }
                    }

                    // Eğer benden küçük portlu kimse yoksa, liderliği devralma vaktim gelmiştir.
                    if (!smallerPortExists) {
                        promoteToLeader();
                    }
                }

            } catch (Exception e) {
                System.err.println("[HEARTBEAT] Hata: " + e.getMessage());
            }
        }, 0, 3, TimeUnit.SECONDS); // 3 saniyede bir kontrol eder
    }

    private static void promoteToLeader() {
        if (isLeader) return; // Zaten lidersem tekrar başlatma

        try {
            System.out.println("[LİDER ELEMESİ] En kıdemli benim, liderlik devralınıyor...");
            isLeader = true;

            // TCP Gateway'i başlatmadan önce çok kısa bekle (Portun tam serbest kalması için)
            Thread.sleep(500);
            startTcpGateway();

            System.out.println("[LİDER ARANIYOR] Yeni LIDER: " + MY_PORT);
        } catch (Exception e) {
            isLeader = false; // Hata alırsak liderliği geri bırak ki bir sonraki döngüde tekrar denesin
            System.err.println("[LİDER ARANIYOR] Hata: " + e.getMessage());
        }
    }

    private static boolean isPortActive(int port) {
        try (java.net.Socket s = new java.net.Socket("localhost", port)) {
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }
}
