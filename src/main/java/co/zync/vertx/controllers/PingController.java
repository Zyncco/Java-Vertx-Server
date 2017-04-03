package co.zync.vertx.controllers;

import io.vertx.ext.web.RoutingContext;

public class PingController {
    
    private static final String SLJ = "https://i.imgur.com/dglafLV.jpg";
    
    public static void ping(RoutingContext context){
        context.response().putHeader("X-S-L-Jackson", SLJ).end(String.format("<img src='%s' />", SLJ));
    }
    
}
