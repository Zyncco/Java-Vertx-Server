package co.zync.vertx;

import co.zync.vertx.controllers.ClipboardController;
import co.zync.vertx.controllers.DeviceController;
import co.zync.vertx.controllers.PingController;
import co.zync.vertx.controllers.UserController;
import co.zync.vertx.managers.DatastoreManager;
import co.zync.vertx.managers.FirebaseManager;
import co.zync.vertx.managers.StorageManager;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    
    private final static int LISTENING_PORT = 8080;
    
    public static void main(String[] args) throws IOException{
        Logger.getLogger("com.google.datastore.v1.client.RemoteRpc").setLevel(Level.ALL);
        
        System.out.print("Initializing Datastore...");
        System.out.println(" initialized: " + DatastoreManager.getInstance().getDatastore().getOptions().getProjectId());
        
        System.out.print("Initializing Storage...");
        System.out.println(" initialized: " + StorageManager.getInstance().getBucket().getName());
        
        System.out.print("Initializing Firebase...");
        System.out.println(" initialized: " + FirebaseManager.getInstance().getFirebaseApp().getOptions().getDatabaseUrl());
        
        Vertx vertx = Vertx.vertx();
        
        Router v0Router = Router.router(vertx);
        createV0Routes(v0Router);
        
        BodyHandler bodyHandler = BodyHandler.create();
        ResponseContentTypeHandler responseContentTypeHandler = ResponseContentTypeHandler.create();
        
        Router router = Router.router(vertx);
        
        router.route("/*")
                .produces("application/json")
                .handler((LoggerHandler) context -> {
                    long start = System.currentTimeMillis();
                    
                    context.response().endHandler(aVoid -> {
                        System.out.printf("[%s][%s][%s][%s][%s] %s\n",
                                System.currentTimeMillis() / 1000,
                                (System.currentTimeMillis() - start) + "ms",
                                context.request().method().name(),
                                context.response().getStatusCode(),
                                context.request().connection().remoteAddress().host(),
                                context.request().uri().split("\\?")[0]);
                    });
                    
                    context.next();
                })
                .failureHandler((ResponseContentTypeHandler) context -> {
                    String message = context.failure().getMessage();
                    
                    if(message == null){
                        message = "java.lang.NullPointerException";
                    }
                    
                    context.failure().printStackTrace();
                    
                    Response.EXCEPTION_THROWN.replyToWithMessage(context, message);
                });
        
        router.route("/*").handler(BodyHandler.create());
        router.route("/*").handler(ResponseContentTypeHandler.create());
        router.route("/*").handler(context -> {
            context.response().putHeader("Access-Control-Allow-Origin", "*");
            context.next();
        });
        
        router.mountSubRouter("/v0", v0Router);
        
        vertx.createHttpServer().requestHandler(router::accept).listen(LISTENING_PORT);
        
        System.out.println();
        System.out.println("HTTP Server Started on port " + LISTENING_PORT);
        System.out.println();
    }
    
    public static void createV0Routes(Router router){
        router.get("/ping/").handler(PingController::ping);
        router.head("/ping/").handler(PingController::ping);
        
        router.get("/clipboard/").handler(ClipboardController::getClipboard);
        router.post("/clipboard/").handler(ClipboardController::postClipboard);
        router.delete("/clipboard/").handler(ClipboardController::deleteClipboard);
        router.get("/clipboard/history/").handler(ClipboardController::getClipboardHistory);
        router.get("/clipboard/raw/").produces("text/plain").handler(ClipboardController::getClipboardRaw);
        
        router.get("/clipboard/:timestamp/").handler(ClipboardController::getClipboardTimestamp);
        router.get("/clipboard/:timestamp/raw").produces("text/plain").handler(ClipboardController::getClipboardTimestampRaw);
        
        router.post("/user/authenticate/").handler(UserController::postAuthenticate);
        
        router.post("/device/validate/").handler(DeviceController::postValidate);
    }
    
}
