package com.mycompany.diskstoresystem;

import com.mycompany.diskstoresystem.proto.DiskServiceGrpc;
import io.grpc.ManagedChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry {
    private static final Map<Integer, DiskServiceGrpc.DiskServiceBlockingStub> memberStubs = new ConcurrentHashMap<>();
    private static final Map<Integer, ManagedChannel> memberChannels = new ConcurrentHashMap<>();

    // Üyeyi listeden çıkarmak için kullanılan Thread-safe metot
    public static synchronized void removeNode(String host, int port) {
        if (memberStubs.containsKey(port)) {
            memberStubs.remove(port);
            ManagedChannel channel = memberChannels.remove(port);
            if (channel != null) {
                channel.shutdown();
            }
            System.out.println("[KAYIT] Üye silindi: " + host + ":" + port);
        }
    }

    public static void addNode(int port, DiskServiceGrpc.DiskServiceBlockingStub stub, ManagedChannel channel) {
        memberStubs.put(port, stub);
        if (channel != null) {
            memberChannels.put(port, channel);
        }
    }

    public static Map<Integer, DiskServiceGrpc.DiskServiceBlockingStub> getMembers() {
        return memberStubs;
    }

    // Aktif üyeler arasında en düşük porta sahip olup olmadığımızı kontrol eder.
    // Lider seçimi (Bully Algorithm benzeri) için kullanılır.
    public static boolean isLowestPort(int myPort) {
        for (Integer port : memberStubs.keySet()) {
            if (port < myPort) {
                return false;
            }
        }
        return true;
    }
}
