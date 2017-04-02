package co.zync.vertx.models;

import co.zync.vertx.managers.DatastoreManager;
import com.google.cloud.datastore.Entity;

public abstract class Base {
    
    private final String kind;
    protected Entity entity;
    private Entity.Builder builder;
    
    public Base(String kind, Entity entity){
        this.kind = kind;
        this.entity = entity;
        
        initialize();
    }
    
    public String getKind(){
        return kind;
    }
    
    public Entity getEntity(){
        return entity;
    }
    
    protected abstract void initialize();
    
    public void save(Entity newEntity){
        DatastoreManager.getInstance().getDatastore().update(newEntity);
        this.entity = newEntity;
        
        initialize();
    }
    
    public void save(){
        save(edit().build());
    }
    
    protected Entity.Builder edit(){
        if(builder == null){
            builder = Entity.newBuilder(entity);
        }
        
        return builder;
    }
    
    public void delete(){
        DatastoreManager.getInstance().getDatastore().delete(getEntity().getKey());
    }
    
}
