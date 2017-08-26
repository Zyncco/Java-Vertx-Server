package co.zync.vertx.models;

import co.zync.vertx.Utils;
import co.zync.vertx.managers.DatastoreManager;
import co.zync.vertx.messages.BaseMulticastMessage;
import co.zync.vertx.messages.BaseUnicastMessage;
import com.google.cloud.datastore.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Device extends Base {
    
    public Device(Entity entity){
        super("device_java", entity);
    }
    
    @Override
    protected void initialize(){
    }
    
    public static DeviceGroup findByUser(Key userKey){
        Query<Entity> query = Query.newEntityQueryBuilder().setKind("device_java").setFilter(StructuredQuery.PropertyFilter.eq("user", userKey)).build();
        QueryResults<Entity> result = DatastoreManager.getInstance().getDatastore().run(query);
        
        List<Device> devices = new ArrayList<>();
        
        while(result.hasNext()){
            devices.add(new Device(result.next()));
        }
        
        return new DeviceGroup(devices);
    }
    
    public static Device findByInstanceId(String instanceId){
        Query<Entity> query = Query.newEntityQueryBuilder().setKind("device_java").setFilter(StructuredQuery.PropertyFilter.eq("instance_id", instanceId)).build();
        QueryResults<Entity> result = DatastoreManager.getInstance().getDatastore().run(query);
        
        if(!result.hasNext()){
            return null;
        }
        
        return new Device(result.next());
    }
    
    public String getInstanceId(){
        return entity.getString("instance_id");
    }
    
    public Key getUser(){
        return entity.getKey("user");
    }
    
    public String getRandomToken(){
        return entity.getString("random_token");
    }
    
    public boolean isValidated(){
        return entity.getBoolean("validated");
    }
    
    public void setValidated(boolean validated){
        edit().set("validated", validated);
    }
    
    @Override
    public String toString(){
        return "User{" +
                "instanceId=" + getInstanceId() +
                ", user=" + getUser() +
                ", randomToken=" + getRandomToken() +
                ", validated=" + isValidated() +
                '}';
    }
    
    public static Device create(String instanceId, Key userKey){
        Key key = DatastoreManager.getInstance().getDatastore().allocateId(DatastoreManager.getInstance().getDatastore().newKeyFactory().setKind("device_java").newKey());
        
        Entity entity = Entity.newBuilder(key)
                .set("instance_id", instanceId)
                .set("user", userKey)
                .set("random_token", Utils.secureRandom(16))
                .set("validated", false)
                .build();
        
        return new Device(DatastoreManager.getInstance().getDatastore().put(entity));
    }
    
    public void send(Object data){
        JSONObject response = Utils.sendFirebaseMessage(new BaseUnicastMessage(data, getInstanceId()));
        
        if(response == null){
            // TODO Custom Error
            throw new NullPointerException();
        }
        
        if(response.getInt("failure") > 0){
            JSONArray results = response.getJSONArray("results");
            for(int i = 0; i < results.length(); i++){
                JSONObject device = results.getJSONObject(i);
                if(device.has("error")){
                    String error = device.getString("error");
                    if(error.equalsIgnoreCase("NotRegistered")
                            || error.equalsIgnoreCase("InvalidRegistration")
                            || error.equalsIgnoreCase("MissingRegistration")){
                        delete();
                    }
                }
            }
        }
    }
    
    public static class DeviceGroup {
        
        private final List<Device> devices;
    
        private DeviceGroup(List<Device> devices){
            this.devices = devices;
        }
    
        public void send(Object data){
            List<Device> validatedDevices = devices.stream().filter(Device::isValidated).collect(Collectors.toList());
            
            if(validatedDevices.size() == 0){
                return;
            }
            
            List<String> instanceIds = validatedDevices.stream().map(Device::getInstanceId).collect(Collectors.toList());
    
            JSONObject response = Utils.sendFirebaseMessage(new BaseMulticastMessage(data, instanceIds));
    
            if(response == null){
                // TODO Custom Error
                throw new NullPointerException();
            }
    
            if(response.getInt("failure") > 0){
                JSONArray results = response.getJSONArray("results");
                for(int i = 0; i < results.length(); i++){
                    JSONObject device = results.getJSONObject(i);
                    if(device.has("error")){
                        String error = device.getString("error");
                        if(error.equalsIgnoreCase("NotRegistered")
                                || error.equalsIgnoreCase("InvalidRegistration")
                                || error.equalsIgnoreCase("MissingRegistration")){
                            validatedDevices.get(i).delete();
                        }
                    }
                }
            }
        }
        
    }
    
}
