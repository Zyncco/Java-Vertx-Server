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
        
        String firebaseToken = data.getString("firebase_token");
        String deviceInstanceId = data.getString("device_id");
    
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
            responseData.put("first_device", first);
    
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("data", responseData);
    
            context.response().end(response.toString());
            
            device.send(new RandomTokenDataMessage(device.getRandomToken(), user.getZyncToken()));
        });
    }
    
}
