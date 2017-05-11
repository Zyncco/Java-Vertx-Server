package co.zync.vertx.managers;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

public class FirebaseManager {
    
    private static FirebaseManager ourInstance = new FirebaseManager();
    
    public static FirebaseManager getInstance(){
        return ourInstance;
    }
    
    private final FirebaseApp firebaseApp;
    private final FirebaseAuth firebaseAuth;
    
    private FirebaseManager(){
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredential(CredentialsManager.getInstance().getFirebaseCredentials())
                .setDatabaseUrl("https://" + CredentialsManager.getInstance().getProjectId() + ".firebaseio.com/")
                .build();

        this.firebaseApp = FirebaseApp.initializeApp(options);
        this.firebaseAuth = FirebaseAuth.getInstance(firebaseApp);
    }
    
    public FirebaseApp getFirebaseApp(){
        return firebaseApp;
    }
    
    public FirebaseAuth getFirebaseAuth(){
        return firebaseAuth;
    }
}
