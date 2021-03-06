package co.zync.vertx.messages;

import co.zync.vertx.models.Clipboard;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;

public class ClipDataMessage {
    
    private final long timestamp;
    private final HashMap<String, String> encryption;
    private final String payloadType;
    
    public ClipDataMessage(Clipboard.Clip clip){
        this.timestamp = clip.getTimestamp();
        this.encryption = clip.getEncryption();
        this.payloadType = clip.getPayloadType();
    }
    
    @JsonProperty("timestamp")
    public long getTimestamp(){
        return timestamp;
    }
    
    @JsonProperty("encryption")
    public HashMap<String, String> getEncryption(){
        return encryption;
    }
    
    @JsonProperty("payload-type")
    public String getPayloadType(){
        return payloadType;
    }
    
}