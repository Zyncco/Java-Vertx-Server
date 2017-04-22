package co.zync.vertx.models;

import co.zync.vertx.managers.DatastoreManager;
import com.google.cloud.datastore.*;

import java.util.Map;

public class UploadURL extends Base {
    
    public UploadURL(Entity entity){
        super("upload_url_java", entity);
    }
    
    @Override
    protected void initialize(){
    }
    
    public static UploadURL findByToken(String token){
        Query<Entity> query = Query.newEntityQueryBuilder().setKind("upload_url_java").setFilter(StructuredQuery.PropertyFilter.eq("token", token)).setLimit(1).build();
        QueryResults<Entity> result = DatastoreManager.getInstance().getDatastore().run(query);
    
        if(!result.hasNext()){
            return null;
        }
    
        return new UploadURL(result.next());
    }
    
    public String getToken(){
        return entity.getString("token");
    }
    
    public FullEntity<IncompleteKey> getData(){
        return entity.getEntity("data");
    }
    
    public Clipboard getClipboard(){
        return Clipboard.findByID(getEntity().getKey("clipboard"));
    }
    
    @Override
    public String toString(){
        return "UploadURL{" +
                "token=" + getToken() +
                '}';
    }
    
    public static UploadURL create(String token, String payloadType, long timestamp, Map<String, Object> hash, Map<String, Object> encryption){
        Key key = DatastoreManager.getInstance().getDatastore().allocateId(DatastoreManager.getInstance().getDatastore().newKeyFactory().setKind("upload_url_java").newKey());
    
        FullEntity.Builder<IncompleteKey> hashEntity = FullEntity.newBuilder();
        hash.forEach((k, value) -> hashEntity.set(k, (String) value));
    
        FullEntity.Builder<IncompleteKey> encryptionEntity = FullEntity.newBuilder();
        encryption.forEach((k, value) -> encryptionEntity.set(k, (String) value));
    
        FullEntity<IncompleteKey> clipEntity = FullEntity.newBuilder()
                .set("payload-type", payloadType)
                .set("timestamp", timestamp)
                .set("hash", hashEntity.build())
                .set("encryption", encryptionEntity.build())
                .build();
    
        Entity entity = Entity.newBuilder(key)
                .set("token", token)
                .set("data", clipEntity)
                .build();
    
        return new UploadURL(DatastoreManager.getInstance().getDatastore().put(entity));
    }
    
}
