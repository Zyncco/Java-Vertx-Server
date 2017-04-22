package co.zync.vertx;

import io.vertx.ext.web.RoutingContext;
import org.json.JSONObject;

public enum Response {
    
    /*
     * 000 Block related to system
     */
    EXCEPTION_THROWN(false, 500, 1, null),
    REQUEST_DATA_INVALID(false, 400, 2, "Request Data Invalid"),
    
    /*
     * 100 Block related to user
     */
    INVALID_ZYNC_TOKEN(false, 401, 100, "Invalid X-ZYNC-TOKEN"),
    INVALID_TOKEN(false, 401, 101, "Invalid Token"),
    
    /*
     * 100 Block related to device
     */
    DEVICE_NOT_FOUND(false, 404, 200, "Device not found"),
    INVALID_RANDOM_TOKEN(false, 404, 201, "Invalid random token"),
    
    /*
     * 300 Block related to clipboard
     */
    CLIPBOARD_EMPTY(false, 200, 300, "Clipboard Empty"),
    CLIPBOARD_OUTDATED(false, 400, 301, "Clipboard Outdated"),
    CLIPBOARD_LATE(false, 400, 302, "Clipboard Late"),
    CLIPBOARD_IDENTICAL(false, 400, 303, "Clipboard Identical"),
    CLIPBOARD_TIME_TRAVEL(false, 400, 304, "Clipboard is time traveling"),
    CLIPBOARD_NOT_FOUND(false, 404, 305, "Clipboard not found"),
    CLIPBOARDS_NOT_FOUND(false, 404, 306, "One or more of requested clipboards not found"),
    CANNOT_RETRIEVE_MULTIPLE_RAW(false, 400, 307, "Invalid upload token"),
    INVALID_UPLOAD_TOKEN(false, 400, 308, "Cannot retrieve multiple raw clipboards");
    
    private boolean success;
    private final int status;
    private int code;
    private String message;
    
    Response(boolean success, int status, int code, String message){
        this.success = success;
        this.status = status;
        this.code = code;
        this.message = message;
    }
    
    public boolean isSuccessful(){
        return success;
    }
    
    public int getCode(){
        return code;
    }
    
    public String getMessage(){
        return message;
    }
    
    public void replyToWithMessage(RoutingContext context, String message){
        JSONObject json = new JSONObject();
        json.put("success", success);
    
        JSONObject error = new JSONObject();
        error.put("code", code);
        error.put("message", message);
    
        json.put("error", error);
    
        context.response().setStatusCode(status);
        context.response().end(json.toString());
    }
    
    public void replyTo(RoutingContext context){
        replyToWithMessage(context, message);
    }
    
}
