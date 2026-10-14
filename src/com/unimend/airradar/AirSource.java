package com.unimend.airradar;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONArray;

final class AirSource {
    private static final String FIELDS="us_aqi,us_aqi_pm2_5,us_aqi_pm10,us_aqi_nitrogen_dioxide,us_aqi_ozone,us_aqi_sulphur_dioxide,pm2_5,pm10,nitrogen_dioxide,ozone,sulphur_dioxide,dust,grass_pollen";
    static JSONObject get(String url) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(7000); c.setReadTimeout(9000); c.setRequestProperty("User-Agent","NothingAirRadar/1.0 (personal Android widget)");
        long deadline=System.nanoTime()+16000000000L;
        try {
            if(c.getResponseCode()!=200) throw new IOException("HTTP "+c.getResponseCode());
            try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buf=new byte[4096]; int n;
                while((n=in.read(buf))!=-1) { if(out.size()+n>384000||System.nanoTime()>deadline||Thread.currentThread().isInterrupted()) throw new IOException("响应过大、超时或已取消"); out.write(buf,0,n); }
                return new JSONObject(out.toString("UTF-8"));
            }
        } finally { c.disconnect(); }
    }
    static JSONArray search(String name) throws Exception { return get("https://geocoding-api.open-meteo.com/v1/search?count=8&language=zh&format=json&name="+URLEncoder.encode(name,"UTF-8")).optJSONArray("results"); }
    static JSONObject fetch(double lat,double lon) throws Exception {
        String coord="latitude="+lat+"&longitude="+lon;
        JSONObject response=get("https://air-quality-api.open-meteo.com/v1/air-quality?"+coord+"&current="+FIELDS+"&hourly="+FIELDS+"&forecast_days=2&timezone=auto&timeformat=unixtime");
        JSONObject current=response.getJSONObject("current"), hourly=response.getJSONObject("hourly");
        double aqi=current.getDouble("us_aqi");
        if(!Double.isFinite(aqi)||aqi<0||aqi>600) throw new IOException("AQI无效");
        JSONObject data=new JSONObject();
        data.put("aqi",aqi).put("pm25",number(current,"pm2_5")).put("pm10",number(current,"pm10"))
            .put("no2",number(current,"nitrogen_dioxide")).put("o3",number(current,"ozone"))
            .put("so2",number(current,"sulphur_dioxide")).put("dust",number(current,"dust"))
            .put("pollen",number(current,"grass_pollen"))
            .put("pm25i",number(current,"us_aqi_pm2_5")).put("pm10i",number(current,"us_aqi_pm10"))
            .put("no2i",number(current,"us_aqi_nitrogen_dioxide")).put("o3i",number(current,"us_aqi_ozone"))
            .put("so2i",number(current,"us_aqi_sulphur_dioxide"))
            .put("observed",current.getLong("time")*1000L).put("fetched",System.currentTimeMillis()).put("timezone",response.optString("timezone","Asia/Shanghai"));
        JSONArray times=hourly.getJSONArray("time"), aqis=hourly.getJSONArray("us_aqi"), outT=new JSONArray(),outA=new JSONArray();
        long start=current.getLong("time"); double peak=-1; long peakTime=start;
        for(int i=0;i<times.length()&&outA.length()<24;i++) if(times.getLong(i)>=start&&!aqis.isNull(i)) {
            double v=aqis.getDouble(i); outT.put(times.getLong(i)); outA.put(v); if(v>peak){peak=v;peakTime=times.getLong(i);}
        }
        data.put("times",outT).put("aqis",outA).put("peak",peak).put("peakTime",peakTime*1000L);
        return data;
    }
    private static double number(JSONObject o,String key) { double v=o.optDouble(key,0); return Double.isFinite(v)&&v>=0?v:0; }
    static String level(double aqi) { if(aqi<=50)return "优"; if(aqi<=100)return "良"; if(aqi<=150)return "轻度"; if(aqi<=200)return "中度"; if(aqi<=300)return "重度"; return "严重"; }
    static String dominant(JSONObject d) {
        String[] names={"PM2.5","PM10","O₃","NO₂","SO₂"}; String[] keys={"pm25i","pm10i","o3i","no2i","so2i"};
        int best=0; for(int i=1;i<keys.length;i++) if(d.optDouble(keys[i])>d.optDouble(keys[best])) best=i; return names[best];
    }
}
