package co.zync.vertx.controllers;

import co.zync.vertx.Response;
import co.zync.vertx.Utils;
import co.zync.vertx.managers.FirebaseManager;
import co.zync.vertx.messages.RandomTokenDataMessage;
import co.zync.vertx.models.Device;
import co.zync.vertx.models.User;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.tasks.Task;
import io.vertx.ext.web.RoutingContext;
import org.json.JSONObject;

public class UserController {
    
    public static void postAuthenticate(RoutingContext context){
        JSONObject body;
        try{
            body = new JSONObject(context.getBodyAsString());
        }catch(Exception ignored){
            Response.REQUEST_DATA_INVALID.replyTo(context);
            return;
        }
    
        if(!Utils.validateJson(body, "postUserAuthenticate")){
            Response.REQUEST_DATA_INVALID.replyTo(context);
            return;
        }
    
        JSONObject data = body.getJSONObject("data");
        
        String firebaseToken = data.getString("firebase-token");
        String deviceInstanceId = data.getString("device-id");
    
        Task<FirebaseToken> task = FirebaseManager.getInstance().getFirebaseAuth().verifyIdToken(firebaseToken);
    
        task.addOnCompleteListener(task1 -> {
            FirebaseToken result;
            try{
                result = task.getResult();
            }catch(Exception ignored){
                Response.INVALID_TOKEN.replyTo(context);
                return;
            }
    
            User user = User.findByEmail(result.getEmail());
            boolean first = user == null;
    
            if(first){
                user = User.create(result.getEmail(), firebaseToken, Utils.secureRandom(32));
            }
    
            Device device = user.addDevice(deviceInstanceId);
    
            JSONObject responseData = new JSONObject();
            responseData.put("first-device", first);
    
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("data", responseData);
    
            context.response().end(response.toString());
            
            device.send(new RandomTokenDataMessage(device.getRandomToken(), user.getZyncToken()));
        });
    }

    public static void getValidate(RoutingContext context){
        User user = User.fromRequestContext(context);
        if(user == null){
            Response.INVALID_ZYNC_TOKEN.replyTo(context);
            return;
        }

        Response.GENERIC_SUCCESS.replyTo(context);
    }

    public static void getLimits(RoutingContext context){
        User user = User.fromRequestContext(context);
        if(user == null){
            Response.INVALID_ZYNC_TOKEN.replyTo(context);
            return;
        }

        JSONObject clipboard = new JSONObject();
        clipboard.put("size-limit", user.getClipboard().getSizeLimit());
        clipboard.put("count-limit", user.getClipboard().getHistoryCountLimit());

        JSONObject responseData = new JSONObject();
        responseData.put("clipboard", clipboard);

        JSONObject response = new JSONObject();
        response.put("success", true);
        response.put("data", responseData);

        context.response().end(response.toString());
    }

    public static void getInfo(RoutingContext context){
        User user = User.fromRequestContext(context);
        if(user == null){
            Response.INVALID_ZYNC_TOKEN.replyTo(context);
            return;
        }

        JSONObject responseData = new JSONObject();
        responseData.put("register-date", user.getRegisterDate());
        responseData.put("clip-count", user.getClipboard().getClipCount());

        JSONObject response = new JSONObject();
        response.put("success", true);
        response.put("data", responseData);

        context.response().end(response.toString());
    }

}
