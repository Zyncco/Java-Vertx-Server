package co.zync.vertx.models;

import co.zync.vertx.Utils;
import co.zync.vertx.managers.CredentialsManager;
import co.zync.vertx.managers.DatastoreManager;
import co.zync.vertx.messages.BaseMulticastMessage;
import co.zync.vertx.messages.BaseUnicastMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.datastore.*;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

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
        BaseUnicastMessage message = new BaseUnicastMessage(data, getInstanceId());
    
        try{
            String mappedMessage = new ObjectMapper().writeValueAsString(message);
            HttpResponse<String> response = Unirest.post("https://fcm.googleapis.com/fcm/send")
                    .header("Content-Type", "application/json")
                    .header("Content-length", String.valueOf(mappedMessage.length()))
                    .header("Authorization", "key=" + CredentialsManager.getInstance().getFirebaseCloudMessagingKey())
                    .body(mappedMessage)
                    .asString();
    
            System.out.println(response.getBody());
        }catch(UnirestException | JsonProcessingException e){
            e.printStackTrace();
        }
        
        /*
        if(errorCode == ErrorCodeEnum.InvalidRegistration
                || errorCode == ErrorCodeEnum.MissingRegistration
                || errorCode == ErrorCodeEnum.NotRegistered){
            delete();
        }
        */
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
    
            BaseMulticastMessage message = new BaseMulticastMessage(data, instanceIds);
    
            try{
                String mappedMessage = new ObjectMapper().writeValueAsString(message);
                HttpResponse<String> response = Unirest.post("https://fcm.googleapis.com/fcm/send")
                        .header("Content-Type", "application/json")
                        .header("Content-length", String.valueOf(mappedMessage.length()))
                        .header("Authorization", "key=" + CredentialsManager.getInstance().getFirebaseCloudMessagingKey())
                        .body(mappedMessage)
                        .asString();
    
                System.out.println(response.getBody());
            }catch(UnirestException | JsonProcessingException e){
                e.printStackTrace();
            }
            
            /*
            for(int i = 0; i < response.getResults().size(); i++){
                ErrorCodeEnum errorCode = response.getResults().get(i).getErrorCode();
    
                if(errorCode == ErrorCodeEnum.InvalidRegistration
                        || errorCode == ErrorCodeEnum.MissingRegistration
                        || errorCode == ErrorCodeEnum.NotRegistered){
                    validatedDevices.get(i).delete();
                }
            }
            */
        }
        
    }
    
}
