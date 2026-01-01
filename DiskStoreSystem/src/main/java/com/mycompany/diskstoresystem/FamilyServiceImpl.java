package com.mycompany.diskstoresystem;

import com.mycompany.diskstoresystem.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FamilyServiceImpl extends DiskServiceGrpc.DiskServiceImplBase {
    private final int myPort;
    private static int nextMemberIndex = 0;
    // private static final Map<Integer, DiskServiceGrpc.DiskServiceBlockingStub>
    // memberStubs = new ConcurrentHashMap<>();
    // Replaced by NodeRegistry

    public FamilyServiceImpl(int port) {
        this.myPort = port;
    }

    // --- 1. JOIN (Yeni Üye Kaydı) ---
    @Override
    public void join(JoinRequest request, StreamObserver<JoinResponse> responseObserver) {
        int remotePort = request.getPort();
        if (remotePort != this.myPort && !NodeRegistry.getMembers().containsKey(remotePort)) {
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", remotePort)
                    .usePlaintext()
                    .build();
            // NodeRegistry üzerinden üyeyi ekle
            NodeRegistry.addNode(remotePort, DiskServiceGrpc.newBlockingStub(channel), channel);

            System.out.println("[FAMILY] Yeni üye listeye eklendi: " + remotePort);
        }
        JoinResponse response = JoinResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Lider (" + myPort + ") seni kabul etti.")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // --- 2. STORE (Üye Veriyi Alınca Kaydeder) ---
    @Override
    public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {
        // DÜZELTME: Üye (Follower) mesajı aldığında diske yazmalı
        saveToDisk(request.getKey(), request.getData());

        StoreResponse response = StoreResponse.newBuilder()
                .setSuccess(true)
                .setMessage(myPort + " portuna kaydedildi.")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // --- 3. RETRIEVE (Veri Okuma) ---
    @Override
    public void retrieve(RetrieveRequest request, StreamObserver<RetrieveResponse> responseObserver) {
        // Üye kendi diskine bakar
        String data = readFromLocalDisk(request.getKey());

        RetrieveResponse response;
        if (data != null) {
            response = RetrieveResponse.newBuilder()
                    .setData(data)
                    .setFound(true)
                    .build();
        } else {
            response = RetrieveResponse.newBuilder()
                    .setFound(false)
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // --- 4. BROADCAST (Chat Mesajı Gelince) ---
    @Override
    public void broadcast(ChatMessage request, StreamObserver<Empty> responseObserver) {
        System.out.println("\n--------------------------------------");
        System.out.println(" 💬 YENİ MESAJ GELDİ!");
        System.out.println("  Kimden (Port): " + request.getFromPort());
        System.out.println("  Mesaj: " + request.getText());
        System.out.println("--------------------------------------");
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void ping(Empty request, StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    // --- LİDER METOTLARI (Statik) ---

    public static void broadcastToAll(String text, int fromPort) {
        ChatMessage chatMsg = ChatMessage.newBuilder()
                .setText(text)
                .setFromPort(fromPort)
                .setTimestamp(System.currentTimeMillis())
                .build();

        for (Integer targetPort : NodeRegistry.getMembers().keySet()) {
            try {
                NodeRegistry.getMembers().get(targetPort).broadcast(chatMsg);

            } catch (Exception e) {
                System.err.println("-> Mesaj iletilemedi: " + targetPort);
            }
        }
    }

    private static final Map<Integer, Integer> memberMessageCounts = new ConcurrentHashMap<>();

    public static void sendStoreToAll(String key, String value) {
        saveToDisk(key, value); // Lider yazar

        int tolerance = getToleranceValue();
        List<Integer> ports = new ArrayList<>(NodeRegistry.getMembers().keySet());

        int totalMembers = ports.size();

        if (totalMembers == 0) {
            System.out.println("[LIDER] Yedeklenecek üye yok.");
            return;
        }

        int successfulBackups = 0;
        int attempts = 0;

        // Hedef: Tolerance kadar başarılı kayıt!
        // Ama toplam üye sayısından fazla deneme yapma (sonsuz döngü olmasın)
        while (successfulBackups < tolerance && attempts < totalMembers) {
            int targetPort = ports.get(nextMemberIndex % totalMembers);
            nextMemberIndex++;
            attempts++;

            try {
                StoreRequest req = StoreRequest.newBuilder().setKey(key).setData(value).build();
                StoreResponse res = NodeRegistry.getMembers().get(targetPort).store(req);

                if (res.getSuccess()) {

                    successfulBackups++;
                    memberMessageCounts.put(targetPort, memberMessageCounts.getOrDefault(targetPort, 0) + 1);
                    System.out.println("-> [" + successfulBackups + "/" + tolerance + "] Başarıyla yedeklendi: Port "
                            + targetPort);
                }
            } catch (Exception e) {
                System.err.println("-> Port " + targetPort + " denendi ama başarısız.");
            }
        }
    }

    public static String sendRetrieveRequest(String key) {
        System.out.println("[LIDER] Okuma istegi: " + key);

        // 1. ADIM: Lider kendi diskine bakar
        String localData = readFromLocalDisk(key);
        if (localData != null)
            return "[LIDERDEN] " + localData;

        // 2. ADIM: Kendi diskinde yoksa, uyeleri sirayla sorgular
        for (Integer port : NodeRegistry.getMembers().keySet()) {
            try {
                System.out.println("-> Uye " + port + " sorgulaniyor...");
                RetrieveRequest req = RetrieveRequest.newBuilder().setKey(key).build();

                // Bu cagri sirasinda eger uye crash olmusa Exception firlatir
                RetrieveResponse res = NodeRegistry.getMembers().get(port).retrieve(req);

                if (res.getFound()) {
                    System.out.println("-> Mesaj Uye " + port + " uzerinde bulundu!");
                    return "[UYE " + port + "]: " + res.getData();
                }
            } catch (Exception e) {
                // 3. ADIM: Eger 3. uye crash olmussa buraya duser ve dongu 4. uyeye gecer
                System.err.println("-> Uye " + port + " cevap vermiyor (Crash olmus olabilir), siradakine geciliyor.");
            }
        }

        return "HATA: Mesaj hicbir yerde bulunamadi.";
    }
    // --- YARDIMCI METOTLAR ---

    // DÜZELTME: İki tane saveToDiskStatic vardı, teke indirdik ve ismini saveToDisk
    // yaptık
    private static void saveToDisk(String key, String data) {
        try {
            File folder = new File("messages");
            if (!folder.exists())
                folder.mkdirs();

            File file = new File(folder, key + ".msg");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(data);
            }
            System.out.println("[DISK] Yazma basarili: " + file.getName());
        } catch (IOException e) {
            System.err.println("[DISK] Hata: " + e.getMessage());
        }
    }

    private static int getToleranceValue() {
        File file = new File("tolerance.conf");
        try {
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.toUpperCase().startsWith("TOLERANCE=")) {
                        String valuePart = line.split("=")[1].trim();
                        return Integer.parseInt(valuePart);
                    }
                }
                scanner.close();
            }
        } catch (Exception e) {
            System.err.println("[HATA] tolerance.conf okunurken hata: " + e.getMessage());
        }
        return 1; // Dosya okunamazsa veya format yanlışsa
    }

    // FamilyServiceImpl.java içine
    public static void printMemberStats() {
        if (memberMessageCounts.isEmpty()) {
            System.out.println("  - Henüz üyelerde kayıtlı mesaj yok veya üye bağlı değil.");
            return;
        }
        for (Map.Entry<Integer, Integer> entry : memberMessageCounts.entrySet()) {
            System.out.println("  - Üye (Port: " + entry.getKey() + "): " + entry.getValue() + " mesaj");
        }
    }

    private static String readFromLocalDisk(String key) {
        try {
            // Key'i temizle (dosya adıyla eşleşmesi için)
            String cleanKey = key.trim().replaceAll("[\\p{Cntrl}]", "");
            File file = new File("messages", cleanKey + ".msg");

            if (file.exists()) {
                StringBuilder content = new StringBuilder();
                try (Scanner reader = new Scanner(file)) {
                    while (reader.hasNextLine()) {
                        content.append(reader.nextLine());
                    }
                }
                return content.toString();
            }
        } catch (IOException e) {
            System.err.println("[DISK] Okuma hatası: " + e.getMessage());
        }
        return null; // Dosya yoksa veya hata varsa null döner
    }
}