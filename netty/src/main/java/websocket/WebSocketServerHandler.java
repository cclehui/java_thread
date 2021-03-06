package websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import org.apache.log4j.Logger;
import io.netty.util.CharsetUtil;

import java.sql.Connection;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * Created by Administrator on 2017/4/7.
 */
public class WebSocketServerHandler extends ChannelInboundHandlerAdapter {
    public static final String WEBSOCKET_PATH = "/websocket";

    private static final String NEWLINE = "\r\n";

    protected static Logger logger = Logger.getLogger(WebSocketServerHandler.class.getName());

    private WebSocketServerHandshaker handshaker;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            logger.info("get http request....");
            handleHttpRequest(ctx, (HttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        } else {
            logger.info("未知的messageEvent类型\t" + msg);
            ctx.close();
            logger.info("未知的连接，关闭连接");
        }
        ctx.fireChannelRead(msg);
    }

    private void handleHttpRequest(final ChannelHandlerContext ctx, HttpRequest req) {
        // Allow only GET methods.
        if (req.getMethod() != GET) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }

        // Send the demo page and favicon.ico
        if ("/".equals(req.getUri())) {

            ByteBuf contentByteBuf = getIndexContent(getWebSocketLocation(req));

            DefaultFullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, contentByteBuf);
            res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");

            if (HttpHeaders.isKeepAlive(req)) {
                res.headers().set(CONNECTION, Values.KEEP_ALIVE);//keep alive
            }
            setContentLength(res, contentByteBuf.readableBytes());
            sendHttpResponse(ctx, req, res);

            return;
        }
        if ("/favicon.ico".equals(req.getUri())) {
            DefaultFullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            sendHttpResponse(ctx, req, res);
            return;
        }

        // websocke Handshake 握手

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false);
        handshaker = wsFactory.newHandshaker(req);
        if(handshaker == null) {
            wsFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            ChannelFuture handshakeFuture = handshaker.handshake(ctx.channel(), req);
            handshakeFuture.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        ctx.fireExceptionCaught(future.cause());
                    } else {
                        ctx.fireUserEventTriggered(WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE);
                    }

                }
            });
        }

        // websocke Handshake 握手 握手的动作交给 WebSocketServerProtocolHandler
//        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
//                getWebSocketLocation(req), null, false);
//        handshaker = wsFactory.newHandshaker(req);
//        if (handshaker == null) {
//            wsFactory.sendUnsupportedVersionResponse(ctx.channel());
//        } else {
//            handshaker.handshake(ctx.channel(), req);
//        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            logger.info("fram instanceof CloseWebSocketFrame ccccccccc");
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame);
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content()));
            return;
        }
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(
                    String.format("%s frame types not supported", frame.getClass().getName()));
        }

        // Send the uppercase string back.
        String request = ((TextWebSocketFrame) frame).text();
        System.out.println(String.format("rrrrrrrr Channel %s received %s", ctx.channel(), request));
        ctx.channel().writeAndFlush(new TextWebSocketFrame(request.toUpperCase()));
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, DefaultFullHttpResponse res) {
        // Generate an error page if response status code is not OK (200).
        if (res.getStatus() != HttpResponseStatus.OK) {
//            res.setContent(ChannelBuffers.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8));
//            setContentLength(res, res.getContent().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.writeAndFlush(res);
        if (!isKeepAlive(req) || res.getStatus() != HttpResponseStatus.OK) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.channel().close();
    }


    protected static ByteBuf getIndexContent(String webSocketLocation) {

        return Unpooled.copiedBuffer("<html><head><title>Web Socket Test</title></head>" + NEWLINE +
                        "<body>" + NEWLINE +
                        "<script type=\"text/javascript\">" + NEWLINE +
                        "var socket;" + NEWLINE +
                        "if (!window.WebSocket) {" + NEWLINE +
                        "  window.WebSocket = window.MozWebSocket;" + NEWLINE +
                        '}' + NEWLINE +
                        "if (window.WebSocket) {" + NEWLINE +
                        "  socket = new WebSocket(\"" + webSocketLocation + "\");" + NEWLINE +
                        "  socket.onmessage = function(event) {" + NEWLINE +
                        "    var ta = document.getElementById('responseText');" + NEWLINE +
                        "    ta.value = ta.value + '\\n' + event.data" + NEWLINE +
                        "  };" + NEWLINE +
                        "  socket.onopen = function(event) {" + NEWLINE +
                        "    var ta = document.getElementById('responseText');" + NEWLINE +
                        "    ta.value = \"Web Socket opened!\";" + NEWLINE +
                        "  };" + NEWLINE +
                        "  socket.onclose = function(event) {" + NEWLINE +
                        "    var ta = document.getElementById('responseText');" + NEWLINE +
                        "    ta.value = ta.value + " + NEWLINE + "\"Web Socket closed\"; " + NEWLINE +
                        "  };" + NEWLINE +
                        "} else {" + NEWLINE +
                        "  alert(\"Your browser does not support Web Socket.\");" + NEWLINE +
                        '}' + NEWLINE +
                        NEWLINE +
                        "function send(message) {" + NEWLINE +
                        "  if (!window.WebSocket) { return; }" + NEWLINE +
                        "  if (socket.readyState == WebSocket.OPEN) {" + NEWLINE +
                        "    socket.send(message);" + NEWLINE +
                        "  } else {" + NEWLINE +
                        "    alert(\"The socket is not open.\");" + NEWLINE +
                        "  }" + NEWLINE +
                        '}' + NEWLINE +
                        "</script>" + NEWLINE +
                        "<form onsubmit=\"return false;\">" + NEWLINE +
                        "<input type=\"text\" name=\"message\" value=\"Hello, World!\"/>" +
                        "<input type=\"button\" value=\"Send Web Socket Data\"" + NEWLINE +
                        "       onclick=\"send(this.form.message.value)\" />" + NEWLINE +
                        "<h3>Output</h3>" + NEWLINE +
                        "<textarea id=\"responseText\" style=\"width:500px;height:300px;\"></textarea>" + NEWLINE +
                        "</form>" + NEWLINE +
                        "</body>" + NEWLINE +
                        "</html>" + NEWLINE, CharsetUtil.UTF_8);
    }

    private static String getWebSocketLocation(HttpRequest req) {
        String location =  req.headers().get(HOST) + WEBSOCKET_PATH;
        if (WebSocketServer.SSL) {
            return "wss://" + location;
        } else {
            return "ws://" + location;
        }
    }
}
