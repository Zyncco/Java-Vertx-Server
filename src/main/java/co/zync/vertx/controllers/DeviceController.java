package co.zync.vertx.controllers;

import co.zync.vertx.Response;
import co.zync.vertx.Utils;
import co.zync.vertx.models.Device;
import co.zync.vertx.models.User;
import io.vertx.ext.web.RoutingContext;
import org.json.JSONObject;

public class DeviceController {
    
    public static void postValidate(RoutingContext context){
        JSONObject body;
        try{
            body = new JSONObject(context.getBodyAsString());
        }catch(Exception ignored){
            Response.REQUEST_DATA_INVALID.replyTo(context);
            return;
        }
    
        if(!Utils.validateJson(body, "postDeviceValidate")){
            Response.REQUEST_DATA_INVALID.replyTo(context);
            return;
        }
    
        User user = User.fromRequestContext(context);
        if(user == null){
            Response.INVALID_ZYNC_TOKEN.replyTo(context);
            return;
        }
        
        JSONObject data = body.getJSONObject("data");
        String deviceId = data.getString("device_id");
        String randomToken = data.getString("random_token");
        
        Device device = user.getDevice(deviceId);
        
        if(device == null){
            Response.DEVICE_NOT_FOUND.replyTo(context);
            return;
        }
        
        if(!device.getRandomToken().equals(randomToken)){
            Response.INVALID_RANDOM_TOKEN.replyTo(context);
            return;
        }
        
        device.setValidated(true);
        device.save();
    
        JSONObject response = new JSONObject();
        response.put("success", true);
    
        context.response().end(response.toString());
    }
    
}
