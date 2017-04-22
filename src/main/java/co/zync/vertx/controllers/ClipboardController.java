package co.zync.vertx.controllers;

import co.zync.vertx.Response;
import co.zync.vertx.Utils;
import co.zync.vertx.messages.ClipDataMessage;
import co.zync.vertx.models.Clipboard;
import co.zync.vertx.models.UploadURL;
import co.zync.vertx.models.User;
import io.vertx.ext.web.RoutingContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClipboardController {
    
    private static final long CLIP_EXPIRY_SMALL = 60 * 1000;
    private static final long CLIP_EXPIRY_BIG = 5 * 60 * 1000;
    private static final long CLIP_FUTURE = 5 * 1000;
    
    public static void getClipboard(RoutingContext context){
        User user = User.fromRequestContext(context);
        if(user == null){
            Response.INVALID_ZYNC_TOKEN.replyTo(context);
            return;
        }
        
        Clipboard clipboard = user.getClipboard();
        if(clipboard.getClips().size() == 0){
            Response.CLIPBOARD_EMPTY.replyTo(context);
            return;
        }
        
        Clipboard.Clip clip = clipboard.getClips().get(clipboard.getLatest());
        
        if(clip == null){
            Response.CLIPBOARD_NOT_FOUND.replyTo(context);
            return;
        }
        
        JSONObject response = new JSONObject();
        response.put("success", true);
        response.put("data", clip.toJson(true));
        
        context.response().end(response.toString());
    }
    
    public static void getClipboardRaw(RoutingContext context){
        User user = User.fromRequestContext(context);
        if(user == null){
            Response.INVALID_ZYNC_TOKEN.replyTo(context);
            return;
        }
        
        Clipboard clipboard = user.getClipboard();
        if(clipboard.getClips().size() == 0){
            Response.CLIPBOARD_EMPTY.replyTo(context);
            return;
        }
        
        Clipboard.Clip clip = clipboard.getClips().get(clipboard.getLatest());
        
        if(clip == null){
            Response.CLIPBOARD_NOT_FOUND.replyTo(context);
            return;
        }
        
        context.response().end(clip.getPayload());
    }
    
    public static void getClipboardTimestamp(RoutingContext context){
        User user = User.fromRequestContext(context);
        if(user == null){
            Response.INVALID_ZYNC_TOKEN.replyTo(context);
            return;
        }
        
        Clipboard clipboard = user.getClipboard();
        if(clipboard.getClips().size() == 0){
            Response.CLIPBOARD_EMPTY.replyTo(context);
            return;
        }
        
        String[] timestamps = context.request().getParam("timestamp").split(",");
        
        if(timestamps.length == 1){
            try{
                Clipboard.Clip clip = clipboard.getClips().get(Long.valueOf(timestamps[0]));
                
                if(clip == null){
                    Response.CLIPBOARD_NOT_FOUND.replyTo(context);
                    return;
                }
                
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("data", clip.toJson(true));
                
                context.response().end(response.toString());
            }catch(Exception ignored){
                Response.CLIPBOARD_NOT_FOUND.replyTo(context);
            }
        }else{
            try{
                JSONArray clipboards = new JSONArray();
                
                for(String timestamp : timestamps){
                    Clipboard.Clip clip = clipboard.getClips().get(Long.valueOf(timestamp));
                    
                    if(clip == null){
                        Response.CLIPBOARDS_NOT_FOUND.replyTo(context);
                        return;
                    }
                    
                    clipboards.put(clip.toJson(true));
                }
                
                JSONObject data = new JSONObject();
                data.put("clipboards", clipboards);
                
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("data", data);
                
                context.response().end(response.toString());
            }catch(Exception e){
                Response.EXCEPTION_THROWN.replyToWithMessage(context, e.getMessage());
            }
        }
    }
    
    public static void getClipboardTimestampRaw(RoutingContext context){
        User user = User.fromRequestContext(context);
        if(user == null){
            Response.INVALID_ZYNC_TOKEN.replyTo(context);
            return;
        }
        
        Clipboard clipboard = user.getClipboard();
        if(clipboard.getClips().size() == 0){
            Response.CLIPBOARD_EMPTY.replyTo(context);
            return;
        }
        
        String[] timestamps = context.request().getParam("timestamp").split(",");
        
        if(timestamps.length == 1){
            try{
                Clipboard.Clip clip = clipboard.getClips().get(Long.valueOf(timestamps[0]));
                
                if(clip == null){
                    Response.CLIPBOARD_NOT_FOUND.replyTo(context);
                    return;
                }
                
                context.response().end(clip.getPayload());
            }catch(Exception ignored){
                Response.CLIPBOARD_NOT_FOUND.replyTo(context);
            }
        }else{
            Response.CANNOT_RETRIEVE_MULTIPLE_RAW.replyTo(context);
        }
    }
    
    public static void postClipboard(RoutingContext context){
        JSONObject body;
        try{
            body = new JSONObject(context.getBodyAsString());
        }catch(Exception ignored){
            Response.REQUEST_DATA_INVALID.replyTo(context);
            return;
        }
        
        if(!Utils.validateJson(body, "postClipboard")){
            Response.REQUEST_DATA_INVALID.replyTo(context);
            return;
        }
        
        JSONObject data = body.getJSONObject("data");
        
        long timestamp = data.getLong("timestamp");
        String payload = data.getString("payload");
        
        if(payload.length() > 10000000){
            if(timestamp < System.currentTimeMillis() - CLIP_EXPIRY_BIG){
                Response.CLIPBOARD_LATE.replyTo(context);
                return;
            }
        }else{
            if(timestamp < System.currentTimeMillis() - CLIP_EXPIRY_SMALL){
                Response.CLIPBOARD_LATE.replyTo(context);
                return;
            }
        }
        
        if(timestamp > System.currentTimeMillis() + CLIP_FUTURE) {
            Response.CLIPBOARD_TIME_TRAVEL.replyTo(context);
            return;
        }
    
        User user = User.fromRequestContext(context);
        if(user == null){
            Response.INVALID_ZYNC_TOKEN.replyTo(context);
            return;
        }
    
        Clipboard clipboard = user.getClipboard();
    
        Map<String, Object> hash = data.getJSONObject("hash").toMap();
        if(clipboard.getClips().size() > 0){
            if(timestamp < clipboard.getLatest()){
                Response.CLIPBOARD_OUTDATED.replyTo(context);
                return;
            }
            
            if(clipboard.clipExists(hash)){
                Response.CLIPBOARD_IDENTICAL.replyTo(context);
                return;
            }
        }
    
        Clipboard.Clip clip = clipboard.newClip(data.getString("payload-type"), payload, timestamp, hash, data.getJSONObject("encryption").toMap());
    
        JSONObject response = new JSONObject();
        response.put("success", true);
        
        context.response().end(response.toString());
        
        user.getDevices().send(new ClipDataMessage(clip));
    }
    
    public static void deleteClipboard(RoutingContext context){
    
    }
    
    public static void getClipboardHistory(RoutingContext context){
        User user = User.fromRequestContext(context);
        if(user == null){
            Response.INVALID_ZYNC_TOKEN.replyTo(context);
            return;
        }
        
        Clipboard clipboard = user.getClipboard();
        if(clipboard.getClips().size() == 0){
            Response.CLIPBOARD_EMPTY.replyTo(context);
            return;
        }
        
        JSONArray history = new JSONArray();
        clipboard.getClips().forEach((timestamp, clip) -> history.put(clip.toJson(false)));
        
        JSONObject data = new JSONObject();
        data.put("history", history);
        
        JSONObject response = new JSONObject();
        response.put("success", true);
        response.put("data", data);
        
        context.response().end(response.toString());
    }
    
    public static void postClipboardUploadURL(RoutingContext context){
        JSONObject body;
        try{
            body = new JSONObject(context.getBodyAsString());
        }catch(Exception ignored){
            Response.REQUEST_DATA_INVALID.replyTo(context);
            return;
        }
    
        if(!Utils.validateJson(body, "postClipboardWithoutPayload")){
            Response.REQUEST_DATA_INVALID.replyTo(context);
            return;
        }
    
        JSONObject data = body.getJSONObject("data");
    
        long timestamp = data.getLong("timestamp");
        if(timestamp > System.currentTimeMillis() + CLIP_FUTURE) {
            Response.CLIPBOARD_TIME_TRAVEL.replyTo(context);
            return;
        }
        
        User user = User.fromRequestContext(context);
        if(user == null){
            Response.INVALID_ZYNC_TOKEN.replyTo(context);
            return;
        }
    
        Clipboard clipboard = user.getClipboard();
    
        Map<String, Object> hash = data.getJSONObject("hash").toMap();
        if(clipboard.getClips().size() > 0){
            if(timestamp < clipboard.getLatest()){
                Response.CLIPBOARD_OUTDATED.replyTo(context);
                return;
            }
        
            if(clipboard.clipExists(hash)){
                Response.CLIPBOARD_IDENTICAL.replyTo(context);
                return;
            }
        }
    
        String token = Utils.secureRandom(32);
    
        UploadURL.create(token, data.getString("payload-type"), timestamp, hash, data.getJSONObject("encryption").toMap());
    
        data = new JSONObject();
        data.put("token", token);
    
        JSONObject response = new JSONObject();
        response.put("success", true);
        response.put("data", data);
    
        context.response().end(response.toString());
    }
    
    private final static ExecutorService executor = Executors.newFixedThreadPool(32);
    
    public static void postClipboardUploadURLToken(RoutingContext context){
        User user = User.fromRequestContext(context);
        if(user == null){
            Response.INVALID_ZYNC_TOKEN.replyTo(context);
            return;
        }
    
        Clipboard clipboard = user.getClipboard();
        String token = context.request().getParam("token");
        UploadURL uploadURL = UploadURL.findByToken(token);
    
        if(uploadURL == null){
            Response.INVALID_UPLOAD_TOKEN.replyTo(context);
            return;
        }
    
        Clipboard.Clip clip = clipboard.newClip(uploadURL.getData());
    
        PipedInputStream inputStream = new PipedInputStream();
        PipedOutputStream outputStream;
    
        try{
            outputStream = new PipedOutputStream(inputStream);
        }catch(IOException e){
            Response.EXCEPTION_THROWN.replyToWithMessage(context, e.getMessage());
            return;
        }
    
        context.request().handler(buffer -> {
            try{
                outputStream.write(buffer.getBytes());
            }catch(IOException e){
                e.printStackTrace();
            }
        });
    
        context.request().endHandler(aVoid -> {
            try{
                outputStream.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        });
    
        executor.execute(() -> {
            clip.writeToStorage(inputStream);
            uploadURL.delete();
            context.response().end(new JSONObject().put("success", true).toString());
        });
    }
    
}
