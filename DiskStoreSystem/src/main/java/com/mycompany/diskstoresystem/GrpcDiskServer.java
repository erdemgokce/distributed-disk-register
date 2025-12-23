package com.mycompany.diskstoresystem;

import com.mycompany.diskstoresystem.proto.DiskServiceGrpc;
import com.mycompany.diskstoresystem.proto.StoreRequest;
import com.mycompany.diskstoresystem.proto.StoreResponse;
import com.mycompany.diskstoresystem.proto.RetrieveRequest;
import com.mycompany.diskstoresystem.proto.RetrieveResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GrpcDiskServer {

    // --- AYARLAR ---
    private static final String DATA_DIR = "messages";
    private static final String CONFIG_FILE = "tolerance.conf";
    
    // Varsayılan değerler
    private static int TOLERANCE = 1; 
    private static int MY_PORT = 50051; 

    // --- DAĞITIK YAPI DEĞİŞKENLERİ ---
    
    // Diğer sunuculara (Followers) erişmek için "Stub" listesi
    private static final List<DiskServiceGrpc.DiskServiceBlockingStub> memberStubs = new ArrayList<>();
    
    // Metadata: Hangi mesaj (Key) hangi üyelerde (List<String>) var?
    // Örnek: "key1" -> ["LIDER", "UYE_1"]
    private static final Map<String, List<String>> keyToMemberMap = new ConcurrentHashMap<>();

    // --- YARDIMCI METOTLAR ---

    // 1. Konfigürasyonu Oku
    private static void loadConfiguration() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            System.out.println(CONFIG_FILE + " bulunamadi. Varsayilan (TOLERANCE=1) olusturuluyor.");
            try (PrintWriter out = new PrintWriter(new FileWriter(configFile))) {
                out.println("TOLERANCE=1");
            } catch (IOException e) {
                System.err.println("Dosya olusturma hatasi: " + e.getMessage());
            }
        }

        try {
            List<String> lines = Files.readAllLines(configFile.toPath());
            for (String line : lines) {
                if (line.trim().startsWith("TOLERANCE=")) {
                    String val = line.split("=")[1].trim();
                    TOLERANCE = Integer.parseInt(val);
                    System.out.println("Konfigurasyon YUKLENDI: Tolerance = " + TOLERANCE);
                }
            }
        } catch (Exception e) {
            System.err.println("Konfigurasyon okuma hatasi: " + e.getMessage());
        }
    }

    // 2. Diğer Üyelere Bağlan (Sadece Lider yapar)
    private static void connectToMembers(List<Integer> otherPorts) {
        System.out.println("Diger ujelere baglanti kuruluyor...");
        for (int port : otherPorts) {
            if (port == MY_PORT) continue; // Kendimize bağlanmayalım

            try {
                // localhost üzerinde çalışan diğer portlara kanal açıyoruz
                ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", port)
                        .usePlaintext() // SSL olmadan
                        .build();
                
                DiskServiceGrpc.DiskServiceBlockingStub stub = DiskServiceGrpc.newBlockingStub(channel);
                memberStubs.add(stub);
                System.out.println(" -> Baglanti tanimlandi: localhost:" + port);
                
            } catch (Exception e) {
                System.err.println(" -> Uyeye baglanilamadi (" + port + "): " + e.getMessage());
            }
        }
    }

    // --- GRPC SERVİS MANTIĞI ---
    static class DiskServiceImpl extends DiskServiceGrpc.DiskServiceImplBase {
        
        public DiskServiceImpl() {
            // Sunucu başlarken klasörü oluştur
            File directory = new File(DATA_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }
        }

        @Override
        public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {
            String key = request.getKey();
            String data = request.getData();
            String fileName = DATA_DIR + File.separator + key + ".msg";

            boolean success = false;
            String message = "";
            StringBuilder logMessage = new StringBuilder(); 

            // --- ADIM 1: Lider (Kendisi) Diske Yazar ---
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                writer.write(data);
                success = true;
                logMessage.append("Lider yazdi. ");
                
                // Metadata: Liderde var olarak işaretle
                keyToMemberMap.computeIfAbsent(key, k -> new ArrayList<>()).add("LIDER");

            } catch (IOException e) {
                success = false;
                message = "Lider disk hatasi: " + e.getMessage();
                System.err.println(message);
            }

            // --- ADIM 2: Replikasyon (Diğer Üyelere Kopyalama) ---
            if (success) {
                int replicationCount = 0;
                
                // Tolerance sayısı kadar üyeye git
                for (DiskServiceGrpc.DiskServiceBlockingStub stub : memberStubs) {
                    if (replicationCount >= TOLERANCE) break; // Yeterli kopyaya ulaşıldı

                    try {
                        // Üyeye "Sen de bunu kaydet" diyoruz
                        StoreResponse response = stub.store(request);
                        
                        if (response.getSuccess()) {
                            replicationCount++;
                            logMessage.append("Uye(" + replicationCount + ") OK. ");
                            
                            // Hangi üyede olduğunu haritaya not et
                            keyToMemberMap.get(key).add("UYE_" + replicationCount);
                        }
                    } catch (Exception e) {
                        logMessage.append("Uye hatasi. ");
                        System.err.println("Replikasyon hatasi: " + e.getMessage());
                    }
                }
                
                message = "ISLEM SONUCU: " + logMessage.toString();
            }

            System.out.println("STORE (" + key + ") -> " + message);

            // İstemciye cevap dön
            StoreResponse response = StoreResponse.newBuilder()
                    .setSuccess(success)
                    .setMessage(message)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void retrieve(RetrieveRequest request, StreamObserver<RetrieveResponse> responseObserver) {
            String key = request.getKey();
            String fileName = DATA_DIR + File.separator + key + ".msg";
            File file = new File(fileName);

            String foundData = "";
            boolean isFound = false;
            String source = "BULUNAMADI";

            // --- ADIM 1: Lider Önce Kendi Diskine Bakar ---
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line);
                    }
                    foundData = content.toString();
                    isFound = true;
                    source = "LIDER_DISK";
                } catch (IOException e) {
                    System.err.println("Lider okuma hatasi: " + e.getMessage());
                }
            }

            // --- ADIM 2: Liderde Yoksa, Üyelere (Followers) Sorar ---
            if (!isFound) {
                // Listemdeki diğer sunuculara tek tek soruyorum
                int memberIndex = 0;
                for (DiskServiceGrpc.DiskServiceBlockingStub stub : memberStubs) {
                    memberIndex++;
                    try {
                        // Üyeye RPC çağrısı yap: "Sende var mı?"
                        RetrieveResponse response = stub.retrieve(request);

                        if (response.getFound()) {
                            foundData = response.getData();
                            isFound = true;
                            source = "UYE_" + memberIndex; // Hangi üyeden geldiğini bilelim
                            break; // Bulduk, artık diğerlerine sormaya gerek yok
                        }
                    } catch (Exception e) {
                        System.err.println("Uye iletisim hatasi (" + memberIndex + "): " + e.getMessage());
                    }
                }
            }

            // --- ADIM 3: Sonucu Raporla ve İstemciye Dön ---
            System.out.println("RETRIEVE (" + key + ") -> Durum: " + (isFound ? "BULUNDU" : "YOK") + ", Kaynak: " + source);

            RetrieveResponse response = RetrieveResponse.newBuilder()
                    .setData(foundData)
                    .setFound(isFound)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    // --- MAIN ---
    public static void main(String[] args) {
        // Port argümanı var mı? (java -jar server.jar 50052 gibi)
        if (args.length > 0) {
            MY_PORT = Integer.parseInt(args[0]);
        }

        System.out.println("=== SUNUCU BAŞLATILIYOR (Port: " + MY_PORT + ") ===");
        
        // 1. Konfigürasyonu Yükle
        loadConfiguration();

        // 2. Eğer biz Lidersek (50051), potansiyel üyelere bağlanalım
        if (MY_PORT == 50051) {
            System.out.println("Rol: LIDER (Cluster Yoneticisi)");
            // Burada sabit portlar tanımladık.
            List<Integer> clusterPorts = Arrays.asList(50052, 50053); 
            connectToMembers(clusterPorts);
        } else {
            System.out.println("Rol: UYE (Follower)");
        }

        // 3. Servisi Başlat
        try {
            Server server = ServerBuilder.forPort(MY_PORT)
                    .addService(new DiskServiceImpl())
                    .build()
                    .start();

            System.out.println("Sunucu dinleniyor...");
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}