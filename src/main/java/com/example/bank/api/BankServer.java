package com.example.bank.api;

import com.example.bank.service.BankService;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * BankServer starts an embedded lightweight HTTP Server on port 8080.
 */
public class BankServer {
    private static final Logger logger = LogManager.getLogger(BankServer.class);
    private static final int DEFAULT_PORT = 8080;
    private HttpServer server;

    public void start(int port, BankService bankService) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new BankApiController(bankService));
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        logger.info("Bank REST API Server started at http://localhost:{}/api", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("Bank REST API Server stopped.");
        }
    }

    public static void main(String[] args) throws IOException {
        BankService bankService = new BankService();
        BankServer bankServer = new BankServer();
        bankServer.start(DEFAULT_PORT, bankService);
        System.out.println("==================================================");
        System.out.println(" Bank Management REST API is running!");
        System.out.println(" Endpoints base URL: http://localhost:8080/api");
        System.out.println(" Press Ctrl+C in terminal to stop server.");
        System.out.println("==================================================");
    }
}
