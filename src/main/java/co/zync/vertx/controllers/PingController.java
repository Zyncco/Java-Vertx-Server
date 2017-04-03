package co.zync.vertx.controllers;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class PingController {

	private static final String SLJ = "https://i.imgur.com/dglafLV.jpg";

	public static void ping(RoutingContext context){
		HttpMethod method = context.request().method();
		HttpServerResponse response = context.response().putHeader("X-S-L-Jackson", SLJ);

		if (method.equals(HttpMethod.HEAD)){
			response.end();
		}else if (method.equals(HttpMethod.GET)){
			response.end(String.format("<img src='%s' />", SLJ));
		}else{
			// Should be 405, but current routes will 404 on unsupported method
			context.fail(404);
		}
	}
}
