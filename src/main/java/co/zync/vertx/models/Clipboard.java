package co.zync.vertx.models;

import co.zync.vertx.managers.DatastoreManager;
import co.zync.vertx.managers.StorageManager;
import com.google.cloud.datastore.*;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Clipboard extends Base {
    
    private HashMap<Long, Clip> clips;
    
    public Clipboard(Entity entity){
        super("clipboard_java", entity);
    }
    
    protected void initialize(){
        this.clips = new HashMap<>();
    
        entity.getList("clips").forEach(clip -> {
            FullEntity clipEntity = (FullEntity) clip.get();
            this.clips.put(clipEntity.getLong("timestamp"), new Clip(clipEntity));
        });
    }
    
    public static Clipboard fromUser(Key userKey){
        Query<Entity> query = Query.newEntityQueryBuilder().setKind("clipboard_java").setFilter(StructuredQuery.PropertyFilter.eq("user", userKey)).setLimit(1).build();
        QueryResults<Entity> result = DatastoreManager.getInstance().getDatastore().run(query);
        
        if(!result.hasNext()){
            return create(userKey);
        }
        
        return new Clipboard(result.next());
    }
    
    protected static Clipboard create(Key userKey) {
        Key key = DatastoreManager.getInstance().getDatastore().allocateId(DatastoreManager.getInstance().getDatastore().newKeyFactory().setKind("clipboard_java").newKey());
    
        Entity entity = Entity.newBuilder(key)
                .set("clip_count", (long) 0)
                .set("clips", Collections.emptyList())
                .set("latest", (long) -1)
                .set("user", userKey)
                .build();
    
        return new Clipboard(DatastoreManager.getInstance().getDatastore().put(entity));
    }
    
    public static Clipboard findByID(Key key){
        Entity result = DatastoreManager.getInstance().getDatastore().get(key);
    
        if(result == null){
            return null;
        }
    
        return new Clipboard(result);
    }
    
    public long getClipCount(){
        return entity.getLong("clip_count");
    }
    
    public long getLatest(){
        try{
            return entity.getLong("latest");
        }catch(Exception ignored){
        }
        
        return (long) entity.getDouble("latest");
    }
    
    public Key getUser(){
        return entity.getKey("user");
    }
    
    public HashMap<Long, Clip> getClips(){
        return clips;
    }
    
    @Override
    public String toString(){
        return "Clipboard{" +
                "clipCount=" + getClipCount() +
                ", latest=" + getLatest() +
                ", user=" + getUser() +
                ", clips=" + clips +
                '}';
    }
    
    public Clip newClip(FullEntity<IncompleteKey> entity){
        Clip ignored = null;
        
        if(clips.size() + 1 > 10){
            ignored = clips.get(clips.keySet().stream().sorted().findFirst().get());
            ignored.deleteFromStorage();
        }
        
        Clip finalIgnored = ignored;
        ListValue.Builder builder = ListValue.newBuilder();
        this.entity.getList("clips").stream().filter(clip -> finalIgnored == null || clip.equals(finalIgnored.entity)).forEach(clip -> builder.addValue(clip));
        
        builder.addValue(entity);
        
        edit().set("clips", builder.build()).set("latest", entity.getLong("timestamp")).set("clip_count", getClipCount() + 1);
    
        save();
        
        return this.clips.get(entity.getLong("timestamp"));
    }
    
    public Clip newClip(String payloadType, String payload, long timestamp, Map<String, Object> encryption){
        Clip clip = newClip(payloadType, timestamp, encryption);
        clip.writeToStorage(payload);
        return clip;
    }
    
    public Clip newClip(String payloadType, long timestamp, Map<String, Object> encryption){
        Clip ignored = null;
    
        if(clips.size() + 1 > 10){
            ignored = clips.get(clips.keySet().stream().sorted().findFirst().get());
            ignored.deleteFromStorage();
        }
    
        Clip finalIgnored = ignored;
        ListValue.Builder builder = ListValue.newBuilder();
        this.entity.getList("clips").stream().filter(clip -> finalIgnored == null || clip.equals(finalIgnored.entity)).forEach(clip -> builder.addValue(clip));
        
        FullEntity.Builder<IncompleteKey> encryptionEntity = FullEntity.newBuilder();
        encryption.forEach((key, value) -> encryptionEntity.set(key, (String) value));
        
        FullEntity<IncompleteKey> clipEntity = FullEntity.newBuilder()
                .set("payload-type", payloadType)
                .set("timestamp", timestamp)
                .set("encryption", encryptionEntity.build())
                .build();
        
        builder.addValue(clipEntity);
        
        edit().set("clips", builder.build()).set("latest", timestamp).set("clip_count", getClipCount() + 1);
        
        save();
        
        return this.clips.get(timestamp);
    }
    
    public class Clip implements Comparable<Clip> {
        
        private FullEntity entity;
        
        private Clip(FullEntity entity){
            this.entity = entity;
        }
        
        public String getPayloadType(){
            return entity.getString("payload-type");
        }
        
        public long getTimestamp(){
            try{
                return entity.getLong("timestamp");
            }catch(Exception ignored){
            }
            
            return (long) entity.getDouble("timestamp");
        }
        
        public HashMap<String, String> getEncryption(){
            HashMap<String, String> encryption = new HashMap<>();
    
            FullEntity encryptionEntity = entity.getEntity("encryption");
            encryptionEntity.getNames().forEach(name -> encryption.put((String) name, encryptionEntity.getString((String) name)));
    
            return encryption;
        }
        
        @Override
        public String toString(){
            return "Clip{" +
                    "payloadType=" + getPayloadType() +
                    ", timestamp=" + getTimestamp() +
                    ", encryption=" + getEncryption() +
                    '}';
        }
        
        @Override
        public int compareTo(Clip clip){
            return Long.compare(clip.getTimestamp(), getTimestamp());
        }
    
        public String getHexPath(){
            String hex = Long.toHexString(getEntity().getKey().getId());
            
            return "/data/clipboards/"
                    + Joiner.on("/").join(Splitter.fixedLength(2).split(hex))
                    + "/" + getTimestamp();
        }
        
        public JSONObject toJson(boolean payload){
            JSONObject data = new JSONObject();
            data.put("timestamp", getTimestamp());
            data.put("payload-type", getPayloadType());
            data.put("encryption", getEncryption());
            
            if(payload){
                try{
                    data.put("payload", new String(StorageManager.getInstance().getBucket().get(getHexPath()).getContent()));
                }catch(Exception ignored){
                    data.put("payload", "null");
                }
            }
            
            return data;
        }
    
        public void writeToStorage(String payload){
            StorageManager.getInstance().getBucket().create(getHexPath(), payload.getBytes());
        }
    
        public void writeToStorage(InputStream stream){
            StorageManager.getInstance().getBucket().create(getHexPath(), stream);
        }
    
        public void deleteFromStorage(){
            try{
                StorageManager.getInstance().getBucket().get(getHexPath()).delete();
            }catch(Exception ignored){
            }
        }
    
        public byte[] getPayload(){
            return StorageManager.getInstance().getBucket().get(getHexPath()).getContent();
        }
        
    }
    
}
