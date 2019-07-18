package com.lib2life.api.application;

import com.lib2life.api.config.CorsFilter;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;

public class Main {
    // Base URI the Grizzly HTTP is listening on
    public static final String BASE_URI = "http://localhost:8080";

    private static final java.util.logging.Logger LOGGER = Grizzly.logger(HttpServer.class);

    public static HttpServer startServer() {
        // Create a resource config that scans for JAX-RS and CORS policy in com.lib2life.api.rest package
        final ResourceConfig rc = new ResourceConfig()
                .packages("com.lib2life.api.rest")
                .register(CorsFilter.class)
                .register(JacksonFeature.class)
                .register(MultiPartFeature.class);

        // Create and start a  grizzly http server exposing the Jersey App at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);

    }

    public static void main(String[] args) {
        final HttpServer server = startServer();

        LOGGER.log(Level.INFO,"[Main] App is available at " + BASE_URI + ". Hit enter to stop it...");

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.shutdownNow();
    }
}

