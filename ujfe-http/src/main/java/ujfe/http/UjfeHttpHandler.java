package ujfe.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import ujfe.core.ClientState;
import ujfe.live.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class UjfeHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final LiveSession liveSession;

    public UjfeHttpHandler(LiveSession liveSession) {
        this.liveSession = Objects.requireNonNull(liveSession, "liveSession");
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
                return response(HttpResponseStatus.OK, "application/javascript; charset=utf-8", ujfe.live.LiveDevToolsScript.script());
            }

            if (request.method().equals(HttpMethod.GET) && LiveHttpPaths.CSS.equals(path)) {
                String classes = firstQueryValue(decoder, "classes").orElse("");
                return response(HttpResponseStatus.OK, "text/css; charset=utf-8", liveSession.renderCss(LiveHttpCodec.parseCssClasses(classes)));
            }

            if (request.method().equals(HttpMethod.POST) && LiveHttpPaths.EVENT.equals(path)) {
                String body = request.content().toString(StandardCharsets.UTF_8);
                String eventId = extractEventId(body);
                ClientState clientState = extractClientState(body);
                LiveRenderResult result = liveSession.handleEvent(eventId, clientState);
                return response(HttpResponseStatus.OK, "application/json; charset=utf-8", livePayload(result));
            }

            if (request.method().equals(HttpMethod.POST) && LiveHttpPaths.STATE.equals(path)) {
                String body = request.content().toString(StandardCharsets.UTF_8);
                LiveRenderResult result = liveSession.updateClientState(extractClientState(body));
                return response(HttpResponseStatus.OK, "application/json; charset=utf-8", livePayload(result));
            }

            if (request.method().equals(HttpMethod.GET)) {
                String cookieHeader = request.headers().get(HttpHeaderNames.COOKIE);
                String document = liveSession.renderDocument(path, ClientState.of(parseCookies(cookieHeader), Map.of()));
                return response(HttpResponseStatus.OK, "text/html; charset=utf-8", document);
            }

            return response(HttpResponseStatus.METHOD_NOT_ALLOWED, "text/plain; charset=utf-8", "Method not allowed");
        } catch (IllegalArgumentException exception) {
            return response(HttpResponseStatus.NOT_FOUND, "text/plain; charset=utf-8", exception.getMessage());
        } catch (RuntimeException exception) {
            return response(HttpResponseStatus.INTERNAL_SERVER_ERROR, "text/plain; charset=utf-8", exception.getMessage());
        }
    }

    static String livePayload(LiveRenderResult result) {
        return LiveHttpCodec.livePayload(result);
    }

    static Set<String> parseCssClasses(String classes) {
        return LiveHttpCodec.parseCssClasses(classes);
    }

    private static Optional<String> firstQueryValue(QueryStringDecoder decoder, String name) {
        List<String> values = decoder.parameters().get(name);
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(0));
    }

    static String extractEventId(String json) {
        return LiveHttpCodec.extractEventId(json);
    }

    static ClientState extractClientState(String json) {
        return LiveHttpCodec.extractClientState(json);
    }

    static Map<String, String> parseCookies(String cookieHeader) {
        return LiveHttpCodec.parseCookies(cookieHeader);
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
