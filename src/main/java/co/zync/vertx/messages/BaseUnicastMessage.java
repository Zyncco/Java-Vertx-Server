package co.zync.vertx.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BaseUnicastMessage {
    
    private final Object data;
    private final String to;
    
    public BaseUnicastMessage(Object data, String to){
        this.data = data;
        this.to = to;
    }
    
    @JsonProperty("data")
    public Object getData(){
        return data;
    }
    
    @JsonProperty("to")
    public String getTo(){
        return to;
    }
    
}
