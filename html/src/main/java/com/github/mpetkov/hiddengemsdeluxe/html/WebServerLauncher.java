package com.github.mpetkov.hiddengemsdeluxe.html;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

/**
 * Serves the pre-built TeaVM webapp bundled inside the JAR.
 * <p>
 * Usage: {@code java -jar hidden-gems-deluxe-web-1.0.0.jar [port]}
 * Default port: 8080, or {@code PORT} environment variable.
 */
public final class WebServerLauncher {

    private WebServerLauncher() {
    }

    public static void main(String[] args) throws Exception {
        int port = resolvePort(args);

        Resource webapp = Resource.newClassPathResource("/webapp");
        if (webapp == null || !webapp.exists()) {
            System.err.println("Missing /webapp inside JAR. Build with: gradlew html:serverJar");
            System.exit(1);
            return;
        }

        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.setBaseResource(webapp);
        context.setWelcomeFiles(new String[] { "index.html" });

        ServletHolder servlet = context.addServlet(DefaultServlet.class, "/*");
        servlet.setInitParameter("dirAllowed", "false");
        servlet.setInitParameter("acceptRanges", "true");
        servlet.setInitParameter("welcomeServlets", "true");
        servlet.setInitParameter("gzip", "true");

        server.setHandler(context);
        server.start();

        System.out.println("Hidden Gems Deluxe (web) running at http://localhost:" + port + "/");
        System.out.println("Press Ctrl+C to stop.");
        server.join();
    }

    private static int resolvePort(String[] args) {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            return Integer.parseInt(portEnv.trim());
        }
        if (args.length > 0 && !args[0].isBlank()) {
            return Integer.parseInt(args[0].trim());
        }
        return 8080;
    }
}
