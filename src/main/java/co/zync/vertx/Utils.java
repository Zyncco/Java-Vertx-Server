package co.zync.vertx;

import co.zync.vertx.managers.CredentialsManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

public class Utils {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    public static boolean validateJson(JSONObject json, String schemaName){
        try(InputStream inputStream = Utils.class.getResourceAsStream("/schemas/" + schemaName + ".json")){
            SchemaLoader.load(new JSONObject(new JSONTokener(inputStream))).validate(json);
            return true;
        }catch(Exception ignored){
        }
        
        return false;
    }
    
    public static String secureRandom(int length){
        String s;
        
        do{
            s = new BigInteger(length * 5, SECURE_RANDOM).toString(32);
        }while(s.length() != length);
        
        return s;
    }
    
    public static JSONObject sendFirebaseMessage(Object message){
        try{
            HttpResponse<JsonNode> response = Unirest.post("https://fcm.googleapis.com/fcm/send")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "key=" + CredentialsManager.getInstance().getFirebaseCloudMessagingKey())
                    .body(OBJECT_MAPPER.writeValueAsString(message))
                    .asJson();
        
            return response.getBody().getObject();
        }catch(UnirestException | JsonProcessingException e){
            e.printStackTrace();
        }
        
        return null;
    }
    
}
