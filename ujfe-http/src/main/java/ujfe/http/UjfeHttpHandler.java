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
import ujfe.live.ErrorResponseContext;
import ujfe.live.ErrorResponseRenderer;
import ujfe.live.LiveHttpCodec;
import ujfe.live.LiveHttpCodecException;
import ujfe.live.LiveHttpEventPayload;
import ujfe.live.LiveHttpPaths;
import ujfe.live.LiveHttpSecurity;
import ujfe.live.LiveRenderResult;
import ujfe.live.LiveSession;
import ujfe.live.LiveCsrfException;
import ujfe.live.LiveHttpRequestMetadata;
import ujfe.live.LiveRateLimitException;
import ujfe.live.SecurityHeadersConfig;
import ujfe.live.UjfeErrorResponse;
import ujfe.runtime.action.RuntimePhase;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
        FullHttpResponse response = route(context, request);
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

    private FullHttpResponse route(ChannelHandlerContext context, FullHttpRequest request) {
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
                LiveHttpRequestMetadata metadata = createMetadata(context, request);
                liveSession.checkInternalEndpointRateLimit(path, metadata);
                LiveHttpEventPayload payload = LiveHttpCodec.parseEventPayload(
                        readJsonPayload(request),
                        maxJsonPayloadBytes
                );
                LiveRenderResult result = liveSession.handleEvent(
                        payload.eventId(),
                        payload.value(),
                        payload.clientState(),
                        metadata
                );
                return response(HttpResponseStatus.OK, "application/json; charset=utf-8", LiveHttpCodec.livePayload(result));
            }

            if (request.method().equals(HttpMethod.POST) && LiveHttpPaths.STATE.equals(path)) {
                LiveHttpRequestMetadata metadata = createMetadata(context, request);
                liveSession.checkInternalEndpointRateLimit(path, metadata);
                LiveRenderResult result = liveSession.updateClientState(
                        LiveHttpCodec.parseStatePayload(readJsonPayload(request), maxJsonPayloadBytes),
                        metadata
                );
                return response(HttpResponseStatus.OK, "application/json; charset=utf-8", LiveHttpCodec.livePayload(result));
            }

            if (request.method().equals(HttpMethod.GET)) {
                String cookieHeader = request.headers().get(HttpHeaderNames.COOKIE);
                String document = liveSession.renderDocument(path, ClientState.of(LiveHttpCodec.parseCookies(cookieHeader), Map.of()));
                return response(HttpResponseStatus.OK, "text/html; charset=utf-8", document);
            }

            return errorResponse(errorRenderer().methodNotAllowed(errorContext(request, path)));
        } catch (LiveRateLimitException exception) {
            LiveHttpCodec.logRejectedRateLimit(exception, "netty", correlationId(request));
            return errorResponse(errorRenderer().render(exception, errorContext(request, path)));
        } catch (LiveCsrfException exception) {
            LiveHttpCodec.logRejectedCsrf(exception, "netty", correlationId(request));
            return errorResponse(errorRenderer().render(exception, errorContext(request, path)));
        } catch (LiveHttpCodecException exception) {
            LiveHttpCodec.logRejectedPayload(exception, "netty", correlationId(request));
            reportHttpError(exception, request, path);
            return errorResponse(errorRenderer().render(exception, errorContext(request, path)));
        } catch (RuntimeException exception) {
            return errorResponse(errorRenderer().render(exception, errorContext(request, path)));
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

    private ErrorResponseRenderer errorRenderer() {
        return ErrorResponseRenderer.create(liveSession.isDevelopmentErrorDetailsEnabled());
    }

    private ErrorResponseContext errorContext(FullHttpRequest request, String path) {
        return ErrorResponseContext.builder()
                .adapter("netty")
                .method(request.method().name())
                .path(path)
                .requestId(correlationId(request))
                .phase(phaseFor(request.method(), path))
                .build();
    }

    private void reportHttpError(Throwable exception, FullHttpRequest request, String path) {
        liveSession.reportHttpError(
                exception,
                phaseFor(request.method(), path),
                path,
                null,
                correlationId(request),
                Map.of("adapter", "netty", "method", request.method().name(), "path", path),
                Map.of(
                        "errorCode", "UJFE_BAD_REQUEST",
                        "httpStatus", exception instanceof LiveHttpCodecException
                                ? ((LiveHttpCodecException) exception).httpStatus()
                                : 500
                )
        );
    }

    private static RuntimePhase phaseFor(HttpMethod method, String path) {
        if (LiveHttpPaths.EVENT.equals(path)) {
            return RuntimePhase.EVENT;
        }
        if (LiveHttpPaths.STATE.equals(path)) {
            return RuntimePhase.STATE;
        }
        if (method.equals(HttpMethod.GET)) {
            return RuntimePhase.RENDER;
        }
        return RuntimePhase.ADAPTER;
    }

    private static LiveHttpRequestMetadata createMetadata(ChannelHandlerContext context, FullHttpRequest request) {
        return new LiveHttpRequestMetadata(
                request.headers().get("X-UJFE-CSRF"),
                request.headers().get(HttpHeaderNames.ORIGIN),
                request.headers().get(HttpHeaderNames.REFERER),
                request.headers().get(HttpHeaderNames.HOST),
                "http",
                remoteAddress(context),
                request.headers().get("Forwarded"),
                request.headers().get("X-Forwarded-For"),
                request.headers().get("X-Real-IP")
        );
    }

    private static String remoteAddress(ChannelHandlerContext context) {
        SocketAddress address = context.channel().remoteAddress();
        if (address instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress) address;
            if (inet.getAddress() != null) {
                return inet.getAddress().getHostAddress();
            }
            return inet.getHostString();
        }
        return address == null ? null : address.toString();
    }

    private FullHttpResponse response(HttpResponseStatus status, String contentType, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.wrappedBuffer(bytes)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        applySecurityHeaders(response, liveSession.securityHeadersConfig());
        return response;
    }

    private FullHttpResponse errorResponse(UjfeErrorResponse error) {
        FullHttpResponse response = response(
                HttpResponseStatus.valueOf(error.httpStatus()),
                UjfeErrorResponse.CONTENT_TYPE,
                error.body()
        );
        error.retryAfterSeconds().ifPresent(seconds -> response.headers().set("Retry-After", Long.toString(seconds)));
        return response;
    }

    static void applySecurityHeaders(FullHttpResponse response) {
        applySecurityHeaders(response, SecurityHeadersConfig.defaults());
    }

    static void applySecurityHeaders(FullHttpResponse response, SecurityHeadersConfig config) {
        LiveHttpSecurity.securityHeaders(config).forEach((name, value) -> response.headers().set(name, value));
    }
}
