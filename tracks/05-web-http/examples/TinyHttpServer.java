import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Run: java tracks/05-web-http/examples/TinyHttpServer.java   (L1, sections 2-3)
 * A JDK-only HTTP server, and a client that talks to it over a raw socket so you SEE the bytes.
 */
public class TinyHttpServer {
	public static void main(String[] args) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/products/7", exchange -> {
			String etag = "\"v7\"";
			if (etag.equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
				exchange.getResponseHeaders().add("ETag", etag);
				exchange.sendResponseHeaders(304, -1); // no body
				exchange.close();
				return;
			}
			byte[] body = "{\"id\":7,\"name\":\"Mug\",\"priceCents\":1200}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.getResponseHeaders().add("ETag", etag);
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		int port = server.getAddress().getPort();

		raw(port, "GET /products/7 HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n");
		raw(port, "GET /products/7 HTTP/1.1\r\nHost: localhost\r\nIf-None-Match: \"v7\"\r\nConnection: close\r\n\r\n");
		raw(port, "GET /nothing-here HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n");
		server.stop(0);
	}

	static void raw(int port, String request) throws IOException {
		System.out.println(">>> sent:\n" + request.replace("\r\n", "\\r\\n\n"));
		try (Socket s = new Socket("127.0.0.1", port)) {
			s.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
			InputStream in = s.getInputStream();
			System.out.println("<<< received:\n" + new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\\r\\n\n"));
		}
	}
}
