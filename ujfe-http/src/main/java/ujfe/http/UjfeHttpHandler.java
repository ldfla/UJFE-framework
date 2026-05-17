package ujfe.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import ujfe.core.ClientState;
import ujfe.live.LiveClientScript;
import ujfe.live.LiveDevToolsScript;
import ujfe.live.LiveHttpCodec;
import ujfe.live.LiveHttpCodecException;
import ujfe.live.LiveHttpEventPayload;
import ujfe.live.LiveHttpPaths;
import ujfe.live.LiveHttpSecurity;
import ujfe.live.LiveRenderResult;
import ujfe.live.LiveSession;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class UjfeHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final LiveSession liveSession;
    private final int maxJsonPayloadBytes;

    public UjfeHttpHandler(LiveSession liveSession) {
        this(liveSession, LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES);
    }

    public UjfeHttpHandler(LiveSession liveSession, int maxJsonPayloadBytes) {
        this.liveSession = Objects.requireNonNull(liveSession, "liveSession");
        LiveHttpCodec.requirePayloadSize(0, maxJsonPayloadBytes);
        this.maxJsonPayloadBytes = maxJsonPayloadBytes;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) {
        FullHttpResponse response = route(request);
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        if (keepAlive) {
            context.writeAndFlush(response);
        } else {
            context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private FullHttpResponse route(FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        String path = decoder.path();

        try {
            if (request.method().equals(HttpMethod.GET) && LiveHttpPaths.CLIENT_SCRIPT.equals(path)) {
                return response(HttpResponseStatus.OK, "application/javascript; charset=utf-8", LiveClientScript.script());
            }

            if (request.method().equals(HttpMethod.GET) && LiveHttpPaths.DEV_SCRIPT.equals(path)) {
                return response(HttpResponseStatus.OK, "application/javascript; charset=utf-8", LiveDevToolsScript.script());
            }

            if (request.method().equals(HttpMethod.GET) && LiveHttpPaths.CSS.equals(path)) {
                String classes = firstQueryValue(decoder, "classes").orElse("");
                return response(HttpResponseStatus.OK, "text/css; charset=utf-8", liveSession.renderCss(LiveHttpCodec.parseCssClasses(classes)));
            }

            if (request.method().equals(HttpMethod.POST) && LiveHttpPaths.EVENT.equals(path)) {
                LiveHttpEventPayload payload = LiveHttpCodec.parseEventPayload(
                        readJsonPayload(request),
                        maxJsonPayloadBytes
                );
                LiveRenderResult result = liveSession.handleEvent(payload.eventId(), payload.clientState());
                return response(HttpResponseStatus.OK, "application/json; charset=utf-8", LiveHttpCodec.livePayload(result));
            }

            if (request.method().equals(HttpMethod.POST) && LiveHttpPaths.STATE.equals(path)) {
                LiveRenderResult result = liveSession.updateClientState(
                        LiveHttpCodec.parseStatePayload(readJsonPayload(request), maxJsonPayloadBytes)
                );
                return response(HttpResponseStatus.OK, "application/json; charset=utf-8", LiveHttpCodec.livePayload(result));
            }

            if (request.method().equals(HttpMethod.GET)) {
                String cookieHeader = request.headers().get(HttpHeaderNames.COOKIE);
                String document = liveSession.renderDocument(path, ClientState.of(LiveHttpCodec.parseCookies(cookieHeader), Map.of()));
                return response(HttpResponseStatus.OK, "text/html; charset=utf-8", document);
            }

            return response(HttpResponseStatus.METHOD_NOT_ALLOWED, "text/plain; charset=utf-8", "Method not allowed");
        } catch (LiveHttpCodecException exception) {
            LiveHttpCodec.logRejectedPayload(exception, "netty", correlationId(request));
            return response(HttpResponseStatus.valueOf(exception.httpStatus()),
                    "text/plain; charset=utf-8",
                    exception.safeMessage());
        } catch (IllegalArgumentException exception) {
            return response(HttpResponseStatus.NOT_FOUND, "text/plain; charset=utf-8", exception.getMessage());
        } catch (RuntimeException exception) {
            return response(HttpResponseStatus.INTERNAL_SERVER_ERROR, "text/plain; charset=utf-8", "Internal server error");
        }
    }

    private static Optional<String> firstQueryValue(QueryStringDecoder decoder, String name) {
        List<String> values = decoder.parameters().get(name);
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(0));
    }

    private String readJsonPayload(FullHttpRequest request) {
        int readableBytes = request.content().readableBytes();
        LiveHttpCodec.requirePayloadSize(readableBytes, maxJsonPayloadBytes);
        return request.content().toString(StandardCharsets.UTF_8);
    }

    private static String correlationId(FullHttpRequest request) {
        String requestId = request.headers().get("X-Request-Id");
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        return request.headers().get("X-Correlation-Id");
    }

    private static FullHttpResponse response(HttpResponseStatus status, String contentType, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.wrappedBuffer(bytes)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        applySecurityHeaders(response);
        return response;
    }

    static void applySecurityHeaders(FullHttpResponse response) {
        LiveHttpSecurity.securityHeaders().forEach((name, value) -> response.headers().set(name, value));
    }
}
