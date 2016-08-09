package net.hawkengine.core;

import net.hawkengine.core.materialhandler.MaterialTracker;
import net.hawkengine.core.pipelinescheduler.JobAssigner;
import net.hawkengine.core.pipelinescheduler.PipelinePreparer;
import net.hawkengine.core.utilities.EndpointFinder;
import net.hawkengine.db.redis.RedisManager;
import net.hawkengine.model.User;
import net.hawkengine.model.enums.PermissionScope;
import net.hawkengine.model.enums.PermissionType;
import net.hawkengine.model.payload.Permission;
import net.hawkengine.services.UserService;
import net.hawkengine.services.interfaces.IUserService;
import net.hawkengine.ws.WsServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.ArrayList;
import java.util.List;

public class HawkServer {
    private Server server;
    private IUserService userService;
    private Thread pipelinePreparer;
    private Thread jobAssigner;
    private Thread materialTracker;
    private EndpointFinder endpointFinder;

    public HawkServer() {
        RedisManager.connect();

        this.server = new Server();
        this.userService = new UserService();
        this.pipelinePreparer = new Thread(new PipelinePreparer());
        this.jobAssigner = new Thread(new JobAssigner());
        this.materialTracker = new Thread(new MaterialTracker());
        this.endpointFinder = new EndpointFinder();
    }

    public void configureJetty() {
        // HTTP connector
        ServerConnector connector = new ServerConnector(this.server);
        int port = ServerConfiguration.getConfiguration().getServerPort();
        connector.setPort(port);
        this.server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
//        resourceHandler.setResourceBase(this.getClass().getResource("/dist").toExternalForm());

        // REST

        ServletHolder restServlet = context.addServlet(ServletContainer.class, "/*");
        restServlet.setInitOrder(0);

        // Tells the Jersey Servlet which REST service/class to load.
        String classes = this.endpointFinder.getClasses("net.hawkengine.http");
        restServlet.setInitParameter("jersey.config.server.provider.classnames", classes);

        // localhost:8080/ws/v1

        // WebSockets

        context.addServlet(WsServlet.class, "/ws/v1");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{resourceHandler, context, new DefaultHandler()});

        this.server.setHandler(handlers);
    }

    public void start() throws Exception {
        this.server.start();
        this.addAdminUser();
        this.pipelinePreparer.start();
        this.jobAssigner.start();
        this.materialTracker.start();
        this.server.join();
    }

    public void stop() {
        RedisManager.disconnect();
    }

    private void addAdminUser() {
        User adminUser = new User();
        adminUser.setEmail("admin@admin.com");
        adminUser.setPassword("admin");
        Permission adminUserPermission = new Permission();
        adminUserPermission.setPermittedEntityId("server");
        adminUserPermission.setPermissionType(PermissionType.ADMIN);
        adminUserPermission.setPermissionScope(PermissionScope.SERVER);
        List<Permission> permissions = new ArrayList<>();
        permissions.add(adminUserPermission);

        adminUser.setPermissions(permissions);

        List<User> users = (List<User>) this.userService.getAll().getObject();
        boolean isPresent = false;

        for (User user : users) {
            if (user.getEmail().equals(adminUser.getEmail())) {
                isPresent = true;
            }
        }
        if (!isPresent) {
            this.userService.add(adminUser);
        }
    }
}