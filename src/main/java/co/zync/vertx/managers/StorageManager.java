package co.zync.vertx.managers;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class StorageManager {
    
    private static StorageManager ourInstance = new StorageManager();
    
    public static StorageManager getInstance(){
        return ourInstance;
    }
    
    private final Storage storage;
    private final Bucket bucket;
    
    private StorageManager(){
        this.storage = StorageOptions.newBuilder()
                .setProjectId(CredentialsManager.getInstance().getProjectId())
                .setCredentials(CredentialsManager.getInstance().getGoogleCredentials())
                .build()
                .getService();
        
        this.bucket = this.storage.get(CredentialsManager.getInstance().getBucketName());
    }
    
    public Storage getStorage(){
        return storage;
    }
    
    public Bucket getBucket(){
        return bucket;
    }
    
}
