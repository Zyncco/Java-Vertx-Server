package co.zync.vertx.models;

import co.zync.vertx.managers.DatastoreManager;
import com.google.cloud.datastore.*;
import io.vertx.ext.web.RoutingContext;

public class User extends Base {
    
    public User(Entity entity){
        super("user_java", entity);
    }
    
    @Override
    protected void initialize(){
    }
    
    public static User fromRequestContext(RoutingContext context){
        String token = context.request().getHeader("X-ZYNC-TOKEN");
        
        if(token == null){
            return null;
        }
    
        Query<Entity> query = Query.newEntityQueryBuilder().setKind("user_java").setFilter(StructuredQuery.PropertyFilter.eq("zync_token", token)).setLimit(1).build();
        QueryResults<Entity> result = DatastoreManager.getInstance().getDatastore().run(query);
        
        if(!result.hasNext()){
            return null;
        }
        
        return new User(result.next());
    }
    
    public static User findByEmail(String email){
        Query<Entity> query = Query.newEntityQueryBuilder().setKind("user_java").setFilter(StructuredQuery.PropertyFilter.eq("email", email)).setLimit(1).build();
        QueryResults<Entity> result = DatastoreManager.getInstance().getDatastore().run(query);
    
        if(!result.hasNext()){
            return null;
        }
    
        return new User(result.next());
    }
    
    public String getEmail(){
        return entity.getString("email");
    }
    
    public String getGoogleToken(){
        return entity.getString("google_token");
    }
    
    public String getZyncToken(){
        return entity.getString("zync_token");
    }
    
    public Clipboard getClipboard(){
        return Clipboard.fromUser(getEntity().getKey());
    }
    
    @Override
    public String toString(){
        return "User{" +
                "email=" + getEmail() +
                ", googleToken=" + getGoogleToken() +
                ", zyncToken=" + getZyncToken() +
                '}';
    }
    
    public static User create(String email, String googleToken, String zyncToken){
        Key key = DatastoreManager.getInstance().getDatastore().allocateId(DatastoreManager.getInstance().getDatastore().newKeyFactory().setKind("user_java").newKey());
    
        Entity entity = Entity.newBuilder(key)
                .set("email", email)
                .set("google_token", googleToken)
                .set("zync_token", zyncToken)
                .build();
    
        return new User(DatastoreManager.getInstance().getDatastore().put(entity));
    }
    
    public Device addDevice(String instanceId){
        Device device = Device.findByInstanceId(instanceId);
        
        if(device != null){
            return device;
        }
    
        return Device.create(instanceId, getEntity().getKey());
    }
    
    public Device getDevice(String instanceId){
        return Device.findByInstanceId(instanceId);
    }
    
    public Device.DeviceGroup getDevices(){
        return Device.findByUser(getEntity().getKey());
    }
    
}
