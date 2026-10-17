package com.xyp.gtnotgood.client.webscreen;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

final class DevToolsConnection implements AutoCloseable {

    private static final int MAX_MESSAGE_BYTES = 48 * 1024 * 1024;
    private final Map<Integer, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final CompletableFuture<Void> handshake = new CompletableFuture<>();
    private final Consumer<JsonObject> events;
    private final Consumer<Throwable> failures;
    private final NioEventLoopGroup group = new NioEventLoopGroup(1, runnable -> {
        Thread thread = new Thread(runnable, "WebScreen-DevTools");
        thread.setDaemon(true);
        return thread;
    });
    private volatile Channel channel;

    DevToolsConnection(Consumer<JsonObject> events, Consumer<Throwable> failures) {
        this.events = events;
        this.failures = failures;
    }

    void connect(URI endpoint) throws Exception {
        if (
            !"ws".equals(endpoint.getScheme()) || !"127.0.0.1".equals(endpoint.getHost())
                || endpoint.getPort() < 1
                || endpoint.getPort() > 65535
        ) {
            throw new IOException("DevTools endpoint must use IPv4 loopback");
        }
        WebSocketClientHandshaker handshaker = new WebSocketClientHandshaker13(endpoint, WebSocketVersion.V13, null,
            false, null, MAX_MESSAGE_BYTES) {

            @Override
            protected FullHttpRequest newHandshakeRequest() {
                FullHttpRequest request = super.newHandshakeRequest();
                // A native CDP client has no web origin; Chromium rejects synthetic browser origins.
                request.headers().remove(HttpHeaders.Names.ORIGIN);
                request.headers().remove(HttpHeaders.Names.SEC_WEBSOCKET_ORIGIN);
                return request;
            }
        };
        Bootstrap bootstrap = new Bootstrap().group(group).channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000).handler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel socket) {
                    socket.pipeline().addLast(new HttpClientCodec(), new HttpObjectAggregator(65536),
                        new WebSocketFrameAggregator(MAX_MESSAGE_BYTES), new Handler(handshaker));
                }
            });
        channel = bootstrap.connect(endpoint.getHost(), endpoint.getPort()).sync().channel();
        if (closed.get()) {
            channel.close();
            throw new IOException("DevTools connection closed");
        }
        handshake.get(5, TimeUnit.SECONDS);
    }

    CompletableFuture<JsonObject> call(String method, JsonObject parameters) {
        int id = nextId.incrementAndGet();
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        if (pending.size() >= 128) {
            result.completeExceptionally(new IOException("Too many pending DevTools commands"));
            return result;
        }
        pending.put(id, result);
        result.whenComplete((value, error) -> pending.remove(id, result));
        if (!write(id, method, parameters)) {
            result.completeExceptionally(new IOException("DevTools connection unavailable"));
        }
        return result;
    }

    void send(String method, JsonObject parameters) {
        write(nextId.incrementAndGet(), method, parameters);
    }

    private boolean write(int id, String method, JsonObject parameters) {
        Channel current = channel;
        if (closed.get() || current == null || !current.isActive() || !current.isWritable()) return false;
        JsonObject message = new JsonObject();
        message.addProperty("id", id);
        message.addProperty("method", method);
        message.add("params", parameters);
        current.writeAndFlush(new TextWebSocketFrame(message.toString()))
            .addListener(future -> { if (!future.isSuccess()) fail(future.cause()); });
        return true;
    }

    private void receive(String text) {
        JsonObject message = new JsonParser().parse(text).getAsJsonObject();
        if (message.has("id")) {
            CompletableFuture<JsonObject> result = pending.remove(message.get("id").getAsInt());
            if (result != null) {
                if (message.has("error")) {
                    result.completeExceptionally(new IOException("DevTools command rejected"));
                } else {
                    result.complete(message.getAsJsonObject("result"));
                }
            }
        } else if (message.has("method")) {
            events.accept(message);
        }
    }

    private void fail(Throwable cause) {
        if (closed.get()) return;
        handshake.completeExceptionally(cause);
        failures.accept(cause);
        close();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        IOException failure = new IOException("DevTools connection closed");
        handshake.completeExceptionally(failure);
        pending.values().forEach(result -> result.completeExceptionally(failure));
        pending.clear();
        Channel current = channel;
        if (current != null) current.close();
        group.shutdownGracefully(0, 2, TimeUnit.SECONDS);
    }

    private final class Handler extends SimpleChannelInboundHandler<Object> {

        private final WebSocketClientHandshaker handshaker;

        private Handler(WebSocketClientHandshaker handshaker) {
            this.handshaker = handshaker;
        }

        @Override
        public void channelActive(ChannelHandlerContext context) {
            handshaker.handshake(context.channel())
                .addListener(future -> { if (!future.isSuccess()) fail(future.cause()); });
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, Object message) {
            if (!handshaker.isHandshakeComplete()) {
                handshaker.finishHandshake(context.channel(), (FullHttpResponse) message);
                handshake.complete(null);
            } else if (message instanceof TextWebSocketFrame) {
                receive(((TextWebSocketFrame) message).text());
            } else if (message instanceof PingWebSocketFrame) {
                context.writeAndFlush(new PongWebSocketFrame(((PingWebSocketFrame) message).content().retain()));
            } else if (message instanceof CloseWebSocketFrame) {
                context.close();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) {
            fail(new IOException("DevTools connection ended"));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            fail(cause);
            context.close();
        }
    }
}
