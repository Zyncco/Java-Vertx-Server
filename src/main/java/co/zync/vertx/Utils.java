package co.zync.vertx;

import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

public class Utils {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
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
    
}
