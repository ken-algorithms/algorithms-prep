package com.prep.cs.counting;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;

/**
 * Ingest that bang Netty: vai luong event loop phuc vu moi ket noi, khong luong nao ngoi cho. Handler giai ma +
 * phan loai NGAY tren event loop (vai µs), dua ban ghi vao IngestNode, va tra 202 khi Kafka xac nhan — callback
 * cua Kafka chay o luong khac, ctx.writeAndFlush tu dua viec ghi ve dung event loop cua ket noi.
 *
 *   scripts/run.sh com.prep.cs.counting.NettyIngestServer 8082 ingest localhost:19092,localhost:29092,localhost:39092
 *   scripts/run.sh com.prep.cs.counting.NettyIngestServer 8082 noop          # chi do chi phi HTTP
 */
public final class NettyIngestServer {

    public static void main(String[] a) throws Exception {
        int port = Integer.parseInt(a[0]);
        boolean noop = a[1].equals("noop");
        String bs = a.length > 2 ? a[2] : "localhost:19092";
        int loops = a.length > 3 ? Integer.parseInt(a[3]) : 2;
        AtomicLong requests = new AtomicLong();
        IngestNode node = null;
        if (!noop) {
            String topic = "netty-events";
            try (Admin admin = Admin.create(Map.of("bootstrap.servers", bs))) {
                if (!admin.listTopics().names().get().contains(topic)) {
                    admin.createTopics(List.of(new NewTopic(topic, 6, (short) 3))).all().get();
                }
            }
            List<String> keys = IntStream.range(0, 1_000_000).mapToObj(k -> "feature_" + k).toList();
            KeyRegistry reg = KeyRegistry.of(keys, new Hash128(1, 2), 1);
            node = new IngestNode(bs, topic, 6, () -> reg, 2_000, 5);
        }
        IngestNode ingest = node;
        NioEventLoopGroup boss = new NioEventLoopGroup(1), workers = new NioEventLoopGroup(loops);
        new ServerBootstrap().group(boss, workers).channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 4_096)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpServerCodec(), new HttpObjectAggregator(64 * 1024),
                                new SimpleChannelInboundHandler<FullHttpRequest>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
                                        boolean keepAlive = HttpUtil.isKeepAlive(req);
                                        if (req.uri().startsWith("/metrics")) {
                                            long cpu = ((com.sun.management.OperatingSystemMXBean) ManagementFactory
                                                    .getOperatingSystemMXBean()).getProcessCpuTime();
                                            respond(ctx, HttpResponseStatus.OK, "{\"requests\":" + requests.get()
                                                    + ",\"cpuNanos\":" + cpu + ",\"sysNanos\":" + sysNanos() + "}",
                                                    keepAlive);
                                            return;
                                        }
                                        requests.incrementAndGet();
                                        if (noop) {
                                            respond(ctx, HttpResponseStatus.ACCEPTED, "", keepAlive);
                                            return;
                                        }
                                        ByteBuf content = req.content();
                                        byte[] body = ByteBufUtil.getBytes(content);   // ~835 byte: chep re
                                        try {
                                            ingest.submit(body).whenComplete((v, e) -> respond(ctx,
                                                    e == null ? HttpResponseStatus.ACCEPTED : HttpResponseStatus.SERVICE_UNAVAILABLE,
                                                    "", keepAlive));
                                        } catch (RequestDecoder.BadRequest e) {
                                            respond(ctx, HttpResponseStatus.BAD_REQUEST,
                                                    "{\"error\":\"" + e.getMessage() + "\"}", keepAlive);
                                        }
                                    }
                                });
                    }
                }).bind(port).sync();
        System.out.printf("Netty nghe cong %d, %d event loop, che do %s%n", port, loops, noop ? "noop" : "ingest");
    }

    /** Phan CPU nam trong kernel (syscall, TCP), doc tu /proc/self/stat (Linux). -1 neu khong co. */
    static long sysNanos() {
        try {
            String[] f = java.nio.file.Files.readString(java.nio.file.Path.of("/proc/self/stat")).split("\\) ")[1].split(" ");
            return Long.parseLong(f[12]) * 10_000_000L;     // stime, don vi tick 10 ms
        } catch (Exception e) {
            return -1;
        }
    }

    static void respond(ChannelHandlerContext ctx, HttpResponseStatus status, String json, boolean keepAlive) {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(b));
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        resp.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, b.length);
        if (keepAlive) {
            resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(resp);
        } else {
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
