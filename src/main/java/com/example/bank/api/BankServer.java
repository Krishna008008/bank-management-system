package com.example.bank.api;

import com.example.bank.service.BankService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * BankServer starts an embedded lightweight HTTP Server on port 8080
 * serving both the REST API and the Web Frontend.
 */
public class BankServer {
    private static final Logger logger = LogManager.getLogger(BankServer.class);
    private static final int DEFAULT_PORT = 8080;
    private HttpServer server;

    public void start(int port, BankService bankService) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        // API Route
        server.createContext("/api", new BankApiController(bankService));
        // Static Web UI Route
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        logger.info("Bank REST API & Web Server started at http://localhost:{}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("Bank Server stopped.");
        }
    }

    /**
     * Serves static HTML, CSS, JS files from resources/web directory.
     */
    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            String resourcePath = "/web" + path;
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    // 404 Not Found
                    String notFound = "404 Not Found";
                    exchange.sendResponseHeaders(404, notFound.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFound.getBytes());
                    }
                    return;
                }

                String contentType = "text/plain";
                if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
                else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
                else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";

                byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        BankService bankService = new BankService();
        BankServer bankServer = new BankServer();
        bankServer.start(DEFAULT_PORT, bankService);
        System.out.println("==================================================");
        System.out.println(" Apex Bank Management System Web Portal is LIVE!");
        System.out.println(" Open in browser: http://localhost:8080/");
        System.out.println(" REST API Base:   http://localhost:8080/api");
        System.out.println(" Press Ctrl+C in terminal to stop server.");
        System.out.println("==================================================");
    }
}
