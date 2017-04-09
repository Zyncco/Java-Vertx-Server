package co.zync.vertx.managers;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.auth.FirebaseCredential;
import com.google.firebase.auth.FirebaseCredentials;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CredentialsManager {
    
    private static CredentialsManager ourInstance = new CredentialsManager();
    
    public static CredentialsManager getInstance(){
        return ourInstance;
    }
    
    private final GoogleCredentials googleCredentials;
    private final FirebaseCredential firebaseCredentials;
    private final String firebaseCloudMessagingKey;
    
    private CredentialsManager(){
        try{
            this.googleCredentials = GoogleCredentials.fromStream(new FileInputStream("credentials/appengine-credentials.json"));
            this.firebaseCredentials = FirebaseCredentials.fromCertificate(new FileInputStream("credentials/firebase-credentials.json"));
            this.firebaseCloudMessagingKey = new String(Files.readAllBytes(Paths.get("credentials/firebase-cloud-messaging-key.txt"))).trim();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }
    
    public GoogleCredentials getGoogleCredentials(){
        return googleCredentials;
    }
    
    public FirebaseCredential getFirebaseCredentials(){
        return firebaseCredentials;
    }
    
    public String getFirebaseCloudMessagingKey(){
        return firebaseCloudMessagingKey;
    }
    
}
