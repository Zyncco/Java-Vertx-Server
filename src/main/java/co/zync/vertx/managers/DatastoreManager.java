package co.zync.vertx.managers;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;

public class DatastoreManager {
    
    private static DatastoreManager ourInstance = new DatastoreManager();
    
    public static DatastoreManager getInstance(){
        return ourInstance;
    }
    
    private final Datastore datastore;
    
    private DatastoreManager(){
        this.datastore = DatastoreOptions.newBuilder()
                .setProjectId("zync-b3bce")
                .setCredentials(CredentialsManager.getInstance().getGoogleCredentials())
                .build()
                .getService();
    }
    
    public Datastore getDatastore(){
        return datastore;
    }
    
}
