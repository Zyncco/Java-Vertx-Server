package co.zync.vertx.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BaseMulticastMessage {
    
    private final Object data;
    private final List<String> to;
    
    public BaseMulticastMessage(Object data, List<String> to){
        this.data = data;
        this.to = to;
    }
    
    @JsonProperty("data")
    public Object getData(){
        return data;
    }
    
    @JsonProperty("to")
    public List<String> getTo(){
        return to;
    }
    
}
