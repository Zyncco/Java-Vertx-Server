package co.zync.vertx.managers;

import de.bytefish.fcmjava.client.FcmClient;
import de.bytefish.fcmjava.client.settings.PropertiesBasedSettings;

import java.util.Properties;

public class FCMManager {
    
    private static FCMManager ourInstance = new FCMManager();
    
    public static FCMManager getInstance(){
        return ourInstance;
    }
    
    private final FcmClient fcmClient;
    
    private FCMManager(){
        Properties fcmProperties = new Properties();
        fcmProperties.setProperty("fcm.api.url", "https://fcm.googleapis.com/fcm/send");
        fcmProperties.setProperty("fcm.api.key", CredentialsManager.getInstance().getFirebaseCloudMessagingKey());
        fcmClient = new FcmClient(PropertiesBasedSettings.createFromProperties(fcmProperties));
    }
    
    public FcmClient getFcmClient(){
        return fcmClient;
    }
    
}
