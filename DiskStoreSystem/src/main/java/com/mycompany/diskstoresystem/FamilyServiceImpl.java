package com.mycompany.diskstoresystem;
import com.mycompany.diskstoresystem.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FamilyServiceImpl extends DiskServiceGrpc.DiskServiceImplBase {
    private final int myPort;

    // Node çalışmaya başladığında kendi portunu buraya yazar.
    // Dosya okuma/yazma işlemleri bu değişkeni kullanarak hangi klasöre bakacağını bilir.
    private static int CURRENT_NODE_PORT;

    private static int nextMemberIndex = 0;

    public FamilyServiceImpl(int port) {
        this.myPort = port;
        CURRENT_NODE_PORT = port;
    }

    // --- 1. JOIN ---
    @Override
    public void join(JoinRequest request, StreamObserver<JoinResponse> responseObserver) {
        int remotePort = request.getPort();
        if (remotePort != this.myPort && !NodeRegistry.getMembers().containsKey(remotePort)) {
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", remotePort)
                    .usePlaintext()
                    .build();
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

    // --- 2. STORE ---
    @Override
    public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {
        // EKRANA BASMA KISMI (Üyeler gelen veriyi görsün)
        System.out.println(">>> [ÜYE - " + myPort + "] Yeni Kayıt İsteği Geldi!");
        System.out.println("    Anahtar: " + request.getKey());
        System.out.println("    Veri:    " + request.getData());

        // Üye mesajı alınca KENDİ klasörüne yazar
        saveToDisk(request.getKey(), request.getData());

        StoreResponse response = StoreResponse.newBuilder()
                .setSuccess(true)
                .setMessage(myPort + " portuna kaydedildi.")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // --- 3. RETRIEVE ---
    @Override
    public void retrieve(RetrieveRequest request, StreamObserver<RetrieveResponse> responseObserver) {
        // Üye veriyi ararken KENDİ klasörüne bakar
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

    // --- 4. BROADCAST ---
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

    // --- LİDER METOTLARI ---

    public static void broadcastToAll(String text, int fromPort) {
        ChatMessage chatMsg = ChatMessage.newBuilder()
                .setText(text)
                .setFromPort(fromPort)
                .setTimestamp(System.currentTimeMillis())
                .build();

        for (Integer targetPort : NodeRegistry.getMembers().keySet()) {
            try {
                // Null kontrolü
                DiskServiceGrpc.DiskServiceBlockingStub stub = NodeRegistry.getMembers().get(targetPort);
                if (stub != null) {
                    stub.broadcast(chatMsg);
                }
            } catch (Exception e) {
                System.err.println("-> Mesaj iletilemedi: " + targetPort);
            }
        }
    }

    private static final Map<Integer, Integer> memberMessageCounts = new ConcurrentHashMap<>();

    public static void sendStoreToAll(String key, String value) {
        // Lider kendi diskine YAZMIYOR.
        // saveToDisk(key, value); // <-- Silindi/Kapatıldı.

        System.out.println("[LIDER] Veri dağıtımı başlıyor: " + key);

        int tolerance = getToleranceValue();
        List<Integer> ports = new ArrayList<>(NodeRegistry.getMembers().keySet());
        int totalMembers = ports.size();

        if (totalMembers == 0) {
            System.out.println("[LIDER] Yedeklenecek üye yok.");
            return;
        }

        int successfulBackups = 0;
        int attempts = 0;

        while (successfulBackups < tolerance && attempts < totalMembers) {
            int targetPort = ports.get(nextMemberIndex % totalMembers);
            nextMemberIndex++;
            attempts++;

            try {
                StoreRequest req = StoreRequest.newBuilder().setKey(key).setData(value).build();

                // Güvenlik Önlemi: Stub null mı kontrol et
                DiskServiceGrpc.DiskServiceBlockingStub stub = NodeRegistry.getMembers().get(targetPort);
                if (stub == null) continue;

                StoreResponse res = stub.store(req);

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

        // LİDER KENDİ DİSKİNE BAKMIYOR. Sadece üyeleri sorguluyor.

        for (Integer port : NodeRegistry.getMembers().keySet()) {
            try {
                System.out.println("-> Uye " + port + " sorgulaniyor...");

                // Güvenlik Önlemi: Eğer üye tam bu anda düştüyse 'stub' null gelebilir.
                DiskServiceGrpc.DiskServiceBlockingStub stub = NodeRegistry.getMembers().get(port);
                if (stub == null) {
                    System.out.println("-> Uye " + port + " listede var ama bağlantı yok (Null).");
                    continue;
                }

                RetrieveRequest req = RetrieveRequest.newBuilder().setKey(key).build();
                // Üye bu isteği aldığında kendi "messages_PORT" klasörüne bakacak.
                RetrieveResponse res = stub.retrieve(req);

                if (res.getFound()) {
                    System.out.println("-> Mesaj Uye " + port + " uzerinde bulundu!");
                    return "[UYE " + port + "]: " + res.getData();
                } else {
                    System.out.println("-> Uye " + port + " cevap verdi ama dosya onda yok.");
                }
            } catch (Exception e) {
                // Üye çökmüşse buraya düşer
                System.err.println("-> Uye " + port + " cevap vermiyor, sıradakine geçiliyor.");
            }
        }

        // --- İSTEDİĞİN GİBİ SADECE MESAJ ---
        return "Dosya getirilemedi";
    }

    // --- YARDIMCI METOTLAR ---

    private static final String DISK_MODE = "UNBUFFERED_SYNC";

    private static void saveToDisk(String key, String data) {
        // DİNAMİK KLASÖR OLUŞTURMA: messages_5560 vb.
        File folder = new File("messages_" + CURRENT_NODE_PORT);

        if (!folder.exists())
            folder.mkdirs();

        File file = new File(folder, key + ".msg");

        try {
            switch (DISK_MODE) {
                case "BUFFERED":
                    writeBuffered(file, data);
                    break;
                case "UNBUFFERED_SYNC":
                    writeUnbufferedSync(file, data);
                    break;
                case "NIO":
                    try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
                         FileChannel channel = raf.getChannel()) {
                        byte[] dataBytes = data.getBytes();
                        java.nio.MappedByteBuffer mappedBuffer = channel.map(
                                FileChannel.MapMode.READ_WRITE, 0, dataBytes.length);
                        mappedBuffer.put(dataBytes);
                    }
                    break;
                default:
                    writeBuffered(file, data);
            }
        } catch (IOException e) {
            System.err.println("[DISK] Hata: " + e.getMessage());
        }
    }

    private static String readFromLocalDisk(String key) {
        try {
            String cleanKey = key.trim().replaceAll("[\\p{Cntrl}]", "");

            // Okurken de sadece kendi portuna ait klasöre bakıyor.
            File file = new File("messages_" + CURRENT_NODE_PORT, cleanKey + ".msg");

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
        return null;
    }

    private static void writeBuffered(File file, String data) throws IOException {
        try (FileWriter writer = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(data);
        }
    }

    private static void writeUnbufferedSync(File file, String data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             java.nio.channels.FileChannel channel = fos.getChannel()) {
            byte[] bytes = data.getBytes();
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
            channel.write(buffer);
            channel.force(true);
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
            System.err.println("[HATA] tolerance.conf hatası: " + e.getMessage());
        }
        return 1;
    }

    public static void printMemberStats() {
        if (memberMessageCounts.isEmpty()) {
            System.out.println("  - Henüz veri yok.");
            return;
        }
        for (Map.Entry<Integer, Integer> entry : memberMessageCounts.entrySet()) {
            System.out.println("  - Üye (Port: " + entry.getKey() + "): " + entry.getValue() + " mesaj");
        }
    }
}
