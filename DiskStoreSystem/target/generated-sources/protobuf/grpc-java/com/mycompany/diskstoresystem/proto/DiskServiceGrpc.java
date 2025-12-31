package com.mycompany.diskstoresystem.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * --- SERVİS TANIMLARI ---
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.58.0)",
    comments = "Source: proto/schema.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class DiskServiceGrpc {

  private DiskServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "family.DiskService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.mycompany.diskstoresystem.proto.JoinRequest,
      com.mycompany.diskstoresystem.proto.JoinResponse> getJoinMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Join",
      requestType = com.mycompany.diskstoresystem.proto.JoinRequest.class,
      responseType = com.mycompany.diskstoresystem.proto.JoinResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mycompany.diskstoresystem.proto.JoinRequest,
      com.mycompany.diskstoresystem.proto.JoinResponse> getJoinMethod() {
    io.grpc.MethodDescriptor<com.mycompany.diskstoresystem.proto.JoinRequest, com.mycompany.diskstoresystem.proto.JoinResponse> getJoinMethod;
    if ((getJoinMethod = DiskServiceGrpc.getJoinMethod) == null) {
      synchronized (DiskServiceGrpc.class) {
        if ((getJoinMethod = DiskServiceGrpc.getJoinMethod) == null) {
          DiskServiceGrpc.getJoinMethod = getJoinMethod =
              io.grpc.MethodDescriptor.<com.mycompany.diskstoresystem.proto.JoinRequest, com.mycompany.diskstoresystem.proto.JoinResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Join"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mycompany.diskstoresystem.proto.JoinRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mycompany.diskstoresystem.proto.JoinResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DiskServiceMethodDescriptorSupplier("Join"))
              .build();
        }
      }
    }
    return getJoinMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mycompany.diskstoresystem.proto.StoreRequest,
      com.mycompany.diskstoresystem.proto.StoreResponse> getStoreMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Store",
      requestType = com.mycompany.diskstoresystem.proto.StoreRequest.class,
      responseType = com.mycompany.diskstoresystem.proto.StoreResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mycompany.diskstoresystem.proto.StoreRequest,
      com.mycompany.diskstoresystem.proto.StoreResponse> getStoreMethod() {
    io.grpc.MethodDescriptor<com.mycompany.diskstoresystem.proto.StoreRequest, com.mycompany.diskstoresystem.proto.StoreResponse> getStoreMethod;
    if ((getStoreMethod = DiskServiceGrpc.getStoreMethod) == null) {
      synchronized (DiskServiceGrpc.class) {
        if ((getStoreMethod = DiskServiceGrpc.getStoreMethod) == null) {
          DiskServiceGrpc.getStoreMethod = getStoreMethod =
              io.grpc.MethodDescriptor.<com.mycompany.diskstoresystem.proto.StoreRequest, com.mycompany.diskstoresystem.proto.StoreResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Store"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mycompany.diskstoresystem.proto.StoreRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mycompany.diskstoresystem.proto.StoreResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DiskServiceMethodDescriptorSupplier("Store"))
              .build();
        }
      }
    }
    return getStoreMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mycompany.diskstoresystem.proto.RetrieveRequest,
      com.mycompany.diskstoresystem.proto.RetrieveResponse> getRetrieveMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Retrieve",
      requestType = com.mycompany.diskstoresystem.proto.RetrieveRequest.class,
      responseType = com.mycompany.diskstoresystem.proto.RetrieveResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mycompany.diskstoresystem.proto.RetrieveRequest,
      com.mycompany.diskstoresystem.proto.RetrieveResponse> getRetrieveMethod() {
    io.grpc.MethodDescriptor<com.mycompany.diskstoresystem.proto.RetrieveRequest, com.mycompany.diskstoresystem.proto.RetrieveResponse> getRetrieveMethod;
    if ((getRetrieveMethod = DiskServiceGrpc.getRetrieveMethod) == null) {
      synchronized (DiskServiceGrpc.class) {
        if ((getRetrieveMethod = DiskServiceGrpc.getRetrieveMethod) == null) {
          DiskServiceGrpc.getRetrieveMethod = getRetrieveMethod =
              io.grpc.MethodDescriptor.<com.mycompany.diskstoresystem.proto.RetrieveRequest, com.mycompany.diskstoresystem.proto.RetrieveResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Retrieve"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mycompany.diskstoresystem.proto.RetrieveRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mycompany.diskstoresystem.proto.RetrieveResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DiskServiceMethodDescriptorSupplier("Retrieve"))
              .build();
        }
      }
    }
    return getRetrieveMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mycompany.diskstoresystem.proto.ChatMessage,
      com.mycompany.diskstoresystem.proto.Empty> getBroadcastMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Broadcast",
      requestType = com.mycompany.diskstoresystem.proto.ChatMessage.class,
      responseType = com.mycompany.diskstoresystem.proto.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mycompany.diskstoresystem.proto.ChatMessage,
      com.mycompany.diskstoresystem.proto.Empty> getBroadcastMethod() {
    io.grpc.MethodDescriptor<com.mycompany.diskstoresystem.proto.ChatMessage, com.mycompany.diskstoresystem.proto.Empty> getBroadcastMethod;
    if ((getBroadcastMethod = DiskServiceGrpc.getBroadcastMethod) == null) {
      synchronized (DiskServiceGrpc.class) {
        if ((getBroadcastMethod = DiskServiceGrpc.getBroadcastMethod) == null) {
          DiskServiceGrpc.getBroadcastMethod = getBroadcastMethod =
              io.grpc.MethodDescriptor.<com.mycompany.diskstoresystem.proto.ChatMessage, com.mycompany.diskstoresystem.proto.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Broadcast"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mycompany.diskstoresystem.proto.ChatMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mycompany.diskstoresystem.proto.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new DiskServiceMethodDescriptorSupplier("Broadcast"))
              .build();
        }
      }
    }
    return getBroadcastMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DiskServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DiskServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DiskServiceStub>() {
        @java.lang.Override
        public DiskServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DiskServiceStub(channel, callOptions);
        }
      };
    return DiskServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DiskServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DiskServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DiskServiceBlockingStub>() {
        @java.lang.Override
        public DiskServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DiskServiceBlockingStub(channel, callOptions);
        }
      };
    return DiskServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static DiskServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DiskServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DiskServiceFutureStub>() {
        @java.lang.Override
        public DiskServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DiskServiceFutureStub(channel, callOptions);
        }
      };
    return DiskServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * --- SERVİS TANIMLARI ---
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Aileye katılma (Join)
     * </pre>
     */
    default void join(com.mycompany.diskstoresystem.proto.JoinRequest request,
        io.grpc.stub.StreamObserver<com.mycompany.diskstoresystem.proto.JoinResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getJoinMethod(), responseObserver);
    }

    /**
     * <pre>
     * Dağıtık veri saklama (SET/GET)
     * </pre>
     */
    default void store(com.mycompany.diskstoresystem.proto.StoreRequest request,
        io.grpc.stub.StreamObserver<com.mycompany.diskstoresystem.proto.StoreResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getStoreMethod(), responseObserver);
    }

    /**
     */
    default void retrieve(com.mycompany.diskstoresystem.proto.RetrieveRequest request,
        io.grpc.stub.StreamObserver<com.mycompany.diskstoresystem.proto.RetrieveResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRetrieveMethod(), responseObserver);
    }

    /**
     * <pre>
     * Hocanın istediği Chat/Broadcast mesajlaşması
     * </pre>
     */
    default void broadcast(com.mycompany.diskstoresystem.proto.ChatMessage request,
        io.grpc.stub.StreamObserver<com.mycompany.diskstoresystem.proto.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBroadcastMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service DiskService.
   * <pre>
   * --- SERVİS TANIMLARI ---
   * </pre>
   */
  public static abstract class DiskServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return DiskServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service DiskService.
   * <pre>
   * --- SERVİS TANIMLARI ---
   * </pre>
   */
  public static final class DiskServiceStub
      extends io.grpc.stub.AbstractAsyncStub<DiskServiceStub> {
    private DiskServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DiskServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DiskServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Aileye katılma (Join)
     * </pre>
     */
    public void join(com.mycompany.diskstoresystem.proto.JoinRequest request,
        io.grpc.stub.StreamObserver<com.mycompany.diskstoresystem.proto.JoinResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getJoinMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Dağıtık veri saklama (SET/GET)
     * </pre>
     */
    public void store(com.mycompany.diskstoresystem.proto.StoreRequest request,
        io.grpc.stub.StreamObserver<com.mycompany.diskstoresystem.proto.StoreResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getStoreMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void retrieve(com.mycompany.diskstoresystem.proto.RetrieveRequest request,
        io.grpc.stub.StreamObserver<com.mycompany.diskstoresystem.proto.RetrieveResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRetrieveMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Hocanın istediği Chat/Broadcast mesajlaşması
     * </pre>
     */
    public void broadcast(com.mycompany.diskstoresystem.proto.ChatMessage request,
        io.grpc.stub.StreamObserver<com.mycompany.diskstoresystem.proto.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBroadcastMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service DiskService.
   * <pre>
   * --- SERVİS TANIMLARI ---
   * </pre>
   */
  public static final class DiskServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<DiskServiceBlockingStub> {
    private DiskServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DiskServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DiskServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Aileye katılma (Join)
     * </pre>
     */
    public com.mycompany.diskstoresystem.proto.JoinResponse join(com.mycompany.diskstoresystem.proto.JoinRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getJoinMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Dağıtık veri saklama (SET/GET)
     * </pre>
     */
    public com.mycompany.diskstoresystem.proto.StoreResponse store(com.mycompany.diskstoresystem.proto.StoreRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getStoreMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mycompany.diskstoresystem.proto.RetrieveResponse retrieve(com.mycompany.diskstoresystem.proto.RetrieveRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRetrieveMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Hocanın istediği Chat/Broadcast mesajlaşması
     * </pre>
     */
    public com.mycompany.diskstoresystem.proto.Empty broadcast(com.mycompany.diskstoresystem.proto.ChatMessage request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBroadcastMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service DiskService.
   * <pre>
   * --- SERVİS TANIMLARI ---
   * </pre>
   */
  public static final class DiskServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<DiskServiceFutureStub> {
    private DiskServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DiskServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DiskServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Aileye katılma (Join)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mycompany.diskstoresystem.proto.JoinResponse> join(
        com.mycompany.diskstoresystem.proto.JoinRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getJoinMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Dağıtık veri saklama (SET/GET)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mycompany.diskstoresystem.proto.StoreResponse> store(
        com.mycompany.diskstoresystem.proto.StoreRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getStoreMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mycompany.diskstoresystem.proto.RetrieveResponse> retrieve(
        com.mycompany.diskstoresystem.proto.RetrieveRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRetrieveMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Hocanın istediği Chat/Broadcast mesajlaşması
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mycompany.diskstoresystem.proto.Empty> broadcast(
        com.mycompany.diskstoresystem.proto.ChatMessage request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBroadcastMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_JOIN = 0;
  private static final int METHODID_STORE = 1;
  private static final int METHODID_RETRIEVE = 2;
  private static final int METHODID_BROADCAST = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_JOIN:
          serviceImpl.join((com.mycompany.diskstoresystem.proto.JoinRequest) request,
              (io.grpc.stub.StreamObserver<com.mycompany.diskstoresystem.proto.JoinResponse>) responseObserver);
          break;
        case METHODID_STORE:
          serviceImpl.store((com.mycompany.diskstoresystem.proto.StoreRequest) request,
              (io.grpc.stub.StreamObserver<com.mycompany.diskstoresystem.proto.StoreResponse>) responseObserver);
          break;
        case METHODID_RETRIEVE:
          serviceImpl.retrieve((com.mycompany.diskstoresystem.proto.RetrieveRequest) request,
              (io.grpc.stub.StreamObserver<com.mycompany.diskstoresystem.proto.RetrieveResponse>) responseObserver);
          break;
        case METHODID_BROADCAST:
          serviceImpl.broadcast((com.mycompany.diskstoresystem.proto.ChatMessage) request,
              (io.grpc.stub.StreamObserver<com.mycompany.diskstoresystem.proto.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getJoinMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mycompany.diskstoresystem.proto.JoinRequest,
              com.mycompany.diskstoresystem.proto.JoinResponse>(
                service, METHODID_JOIN)))
        .addMethod(
          getStoreMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mycompany.diskstoresystem.proto.StoreRequest,
              com.mycompany.diskstoresystem.proto.StoreResponse>(
                service, METHODID_STORE)))
        .addMethod(
          getRetrieveMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mycompany.diskstoresystem.proto.RetrieveRequest,
              com.mycompany.diskstoresystem.proto.RetrieveResponse>(
                service, METHODID_RETRIEVE)))
        .addMethod(
          getBroadcastMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mycompany.diskstoresystem.proto.ChatMessage,
              com.mycompany.diskstoresystem.proto.Empty>(
                service, METHODID_BROADCAST)))
        .build();
  }

  private static abstract class DiskServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    DiskServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.mycompany.diskstoresystem.proto.FamilyProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("DiskService");
    }
  }

  private static final class DiskServiceFileDescriptorSupplier
      extends DiskServiceBaseDescriptorSupplier {
    DiskServiceFileDescriptorSupplier() {}
  }

  private static final class DiskServiceMethodDescriptorSupplier
      extends DiskServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    DiskServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (DiskServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new DiskServiceFileDescriptorSupplier())
              .addMethod(getJoinMethod())
              .addMethod(getStoreMethod())
              .addMethod(getRetrieveMethod())
              .addMethod(getBroadcastMethod())
              .build();
        }
      }
    }
    return result;
  }
}
