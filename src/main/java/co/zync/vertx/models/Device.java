package co.zync.vertx.models;

import co.zync.vertx.Utils;
import co.zync.vertx.managers.DatastoreManager;
import co.zync.vertx.managers.FCMManager;
import com.google.cloud.datastore.*;
import de.bytefish.fcmjava.model.enums.ErrorCodeEnum;
import de.bytefish.fcmjava.model.options.FcmMessageOptions;
import de.bytefish.fcmjava.requests.data.DataMulticastMessage;
import de.bytefish.fcmjava.requests.data.DataUnicastMessage;
import de.bytefish.fcmjava.responses.FcmMessageResponse;

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
        FcmMessageOptions messageOptions = FcmMessageOptions.builder().build();
    
        DataUnicastMessage message = new DataUnicastMessage(messageOptions, getInstanceId(), data);
    
        FcmMessageResponse response = FCMManager.getInstance().getFcmClient().send(message);
        ErrorCodeEnum errorCode = response.getResults().get(0).getErrorCode();
        
        if(errorCode == ErrorCodeEnum.InvalidRegistration
                || errorCode == ErrorCodeEnum.MissingRegistration
                || errorCode == ErrorCodeEnum.NotRegistered){
            delete();
        }
    }
    
    public static class DeviceGroup {
        
        private final List<Device> devices;
    
        private DeviceGroup(List<Device> devices){
            this.devices = devices;
        }
    
        public void send(Object data){
            FcmMessageOptions messageOptions = FcmMessageOptions.builder().build();
    
            List<Device> validatedDevices = devices.stream().filter(Device::isValidated).collect(Collectors.toList());
            
            if(validatedDevices.size() == 0){
                return;
            }
            
            List<String> instanceIds = validatedDevices.stream().map(Device::getInstanceId).collect(Collectors.toList());
            
            DataMulticastMessage message = new DataMulticastMessage(messageOptions, instanceIds, data);
        
            FcmMessageResponse response = FCMManager.getInstance().getFcmClient().send(message);
    
            for(int i = 0; i < response.getResults().size(); i++){
                ErrorCodeEnum errorCode = response.getResults().get(i).getErrorCode();
    
                if(errorCode == ErrorCodeEnum.InvalidRegistration
                        || errorCode == ErrorCodeEnum.MissingRegistration
                        || errorCode == ErrorCodeEnum.NotRegistered){
                    validatedDevices.get(i).delete();
                }
            }
        }
        
    }
    
}
