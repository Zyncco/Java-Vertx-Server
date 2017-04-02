package co.zync.vertx.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RandomTokenDataMessage {
    
    private final String randomToken;
    private final String zyncToken;
    
    public RandomTokenDataMessage(String randomToken, String zyncToken){
        this.randomToken = randomToken;
        this.zyncToken = zyncToken;
    }
    
    @JsonProperty("random-token")
    public String getRandomToken(){
        return randomToken;
    }
    
    @JsonProperty("zync-token")
    public String getZyncToken(){
        return zyncToken;
    }
    
}