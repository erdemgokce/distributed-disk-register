package com.mycompany.diskstoresystem;
// Bu import satırı, proto'dan üretilen DiskServiceGrpc ve mesaj sınıflarını bağlar
import com.mycompany.diskstoresystem.proto.*;
import io.grpc.stub.StreamObserver;
import java.io.*;
import java.util.*;

// Proto'daki "service DiskService" ifadesi buradaki "DiskServiceGrpc" ismini belirler.
public class FamilyServiceImpl extends DiskServiceGrpc.DiskServiceImplBase {

    private final int myPort;

    public FamilyServiceImpl(int port) {
        this.myPort = port;
    }

    @Override
    public void join(JoinRequest request, StreamObserver<JoinResponse> responseObserver) {
        System.out.println("Yeni üye katıldı, Port: " + request.getPort());

        JoinResponse response = JoinResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Hosgeldin " + request.getPort())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {
        // SET komutu buraya gelecek
        String key = request.getKey();
        String data = request.getData();
        System.out.println("Kaydediliyor: " + key);

        StoreResponse response = StoreResponse.newBuilder()
                .setSuccess(true)
                .setMessage("OK")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void retrieve(RetrieveRequest request, StreamObserver<RetrieveResponse> responseObserver) {
        // GET komutu buraya gelecek
        RetrieveResponse response = RetrieveResponse.newBuilder()
                .setData("Ornek Veri")
                .setFound(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Boş bir metod eklemeyi unutma (Proto'da tanımladığın için)
    @Override
    public void broadcast(ChatMessage request, StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}