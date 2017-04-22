package co.zync.vertx;

import co.zync.vertx.controllers.ClipboardController;
import co.zync.vertx.controllers.DeviceController;
import co.zync.vertx.controllers.PingController;
import co.zync.vertx.controllers.UserController;
import co.zync.vertx.managers.DatastoreManager;
import co.zync.vertx.managers.FirebaseManager;
import co.zync.vertx.managers.StorageManager;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
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
    
        VertxOptions options = new VertxOptions();
        options.setMaxEventLoopExecuteTime(Long.MAX_VALUE);
        
        Vertx vertx = Vertx.vertx(options);
        
        Router v0Router = Router.router(vertx);
        createV0Routes(v0Router);
        
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
    
        router.route("/*").handler(ResponseContentTypeHandler.create());
        router.route("/*").handler(context -> {
            context.response().putHeader("Access-Control-Allow-Origin", "*");
            context.next();
        });
    
        BodyHandler bodyHandler = BodyHandler.create();
        router.route("/*").handler(context -> {
            if(context.request().uri().matches("/v./clipboard/upload/.+")){
                context.next();
            }else{
                bodyHandler.handle(context);
            }
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
    
        router.post("/clipboard/upload/").handler(ClipboardController::postClipboardUploadURL);
        router.post("/clipboard/upload/:token").handler(ClipboardController::postClipboardUploadURLToken);
    
        router.get("/clipboard/:timestamp/raw").produces("text/plain").handler(ClipboardController::getClipboardTimestampRaw);
        router.get("/clipboard/:timestamp").handler(ClipboardController::getClipboardTimestamp);
        
        router.post("/user/authenticate/").handler(UserController::postAuthenticate);
        
        router.post("/device/validate/").handler(DeviceController::postValidate);
    }
    
}
