package com.unimend.airradar;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

final class AirStore {
    final SharedPreferences prefs;
    final int widgetId;
    AirStore(Context c) { this(c,0); }
    AirStore(Context c,int widgetId) { prefs=c.getSharedPreferences("air_radar",Context.MODE_PRIVATE); this.widgetId=Math.max(0,widgetId); migrateDefault(); }
    private String key(String name) { return widgetId==0?name:name+"_"+widgetId; }
    private void migrateDefault() {
        if(widgetId==0||prefs.contains(key("lat"))||!prefs.contains("lat")) return;
        SharedPreferences.Editor e=prefs.edit();
        for(String n:new String[]{"city","lat","lon","data","error"}) if(prefs.contains(n)) e.putString(key(n),prefs.getString(n,""));
        e.apply();
    }
    boolean configured() { return prefs.contains(key("lat")); }
    String city() { return prefs.getString(key("city"),"选择城市"); }
    double lat() { return Double.parseDouble(prefs.getString(key("lat"),"30.2741")); }
    double lon() { return Double.parseDouble(prefs.getString(key("lon"),"120.1551")); }
    boolean loading() { return prefs.getBoolean(key("loading"),false); }
    void loading(boolean value) { prefs.edit().putBoolean(key("loading"),value).apply(); }
    boolean hasError() { return prefs.contains(key("error")); }
    void city(String name,double lat,double lon) {
        if(name.trim().isEmpty()||!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180) throw new IllegalArgumentException("城市坐标无效");
        prefs.edit().putString(key("city"),name).putString(key("lat"),Double.toString(lat)).putString(key("lon"),Double.toString(lon)).remove(key("data")).remove(key("error")).apply();
    }
    JSONObject data() { try { return new JSONObject(prefs.getString(key("data"),"{}")); } catch(Exception e) { return new JSONObject(); } }
    void save(JSONObject data) { prefs.edit().putString(key("data"),data.toString()).remove(key("error")).apply(); }
    void error(String value) { prefs.edit().putString(key("error"),value).apply(); }
    int page(int id) { return prefs.getInt("page_"+id,0); }
    void toggle(int id) { prefs.edit().putInt("page_"+id,1-page(id)).apply(); }
    boolean stale(JSONObject d) { long now=System.currentTimeMillis(),f=d.optLong("fetched",0),o=d.optLong("observed",0); return hasError()||now-f>90*60*1000L||now-o>3*60*60*1000L||f>now+60000; }
    void delete() {
        if(widgetId==0) return; SharedPreferences.Editor e=prefs.edit();
        for(String n:new String[]{"city","lat","lon","data","error","loading"}) e.remove(key(n));
        e.remove("page_"+widgetId).apply();
    }
}
