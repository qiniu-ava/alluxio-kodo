package com.aliyun.oss;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
implements MarkerCache to record marker
*/
public class MarkerCache {

    private static Map<String, String> markers = new ConcurrentHashMap<String, String>();

    private MarkerCache() {}

    public static boolean containsKey(String key){
        return markers.containsKey(key);
    }

    public static void removeKey(String key){
        markers.remove(key);
    }

    public static void clear(){
        if (markers.size() > 0)
            markers.clear();
    }

    public static void setMarker(String path, String marker){
        markers.put(path, marker);
    }
  
    public static String getMarker(String path){
        return markers.get(path);
    }
    
}
