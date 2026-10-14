package com.unimend.airradar;

import android.app.job.JobParameters;
import android.app.job.JobService;
import java.util.concurrent.*;
import org.json.JSONObject;

public class AirJob extends JobService {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private final ConcurrentHashMap<Integer,Future<?>> running=new ConcurrentHashMap<>();
    @Override public boolean onStartJob(JobParameters p) {
        int[] widgetIds=AirWidget.ids(this);
        boolean configured=false; for(int id:widgetIds) if(new AirStore(this,id).configured()) configured=true;
        if(!configured) return false;
        android.net.ConnectivityManager connectivity=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        android.net.NetworkCapabilities network=connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
        if(network==null || !network.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            for(int id:widgetIds) { AirStore store=new AirStore(this,id); if(store.configured()) { store.error("当前离线"); store.loading(false); } }
            AirWidget.publish(this);
            sendBroadcast(new android.content.Intent("com.unimend.airradar.UPDATED").setPackage(getPackageName()));
            return false;
        }
        for(int id:widgetIds) { AirStore store=new AirStore(this,id); if(store.configured()) store.loading(true); }
        AirWidget.publish(this);
        Future<?> future=IO.submit(()-> {
            boolean retry=false;
            for(int id:widgetIds) {
                if(Thread.currentThread().isInterrupted()) break;
                AirStore store=new AirStore(this,id);
                if(!store.configured()) continue;
                final double lat=store.lat(),lon=store.lon();
                try {
                    JSONObject data=AirSource.fetch(lat,lon);
                    if(!Thread.currentThread().isInterrupted() && store.lat()==lat && store.lon()==lon) {
                        store.save(data); android.util.Log.i("AirJob","组件"+id+"空气质量更新成功");
                    }
                } catch(Exception e) {
                    if(!Thread.currentThread().isInterrupted() && store.lat()==lat && store.lon()==lon) { store.error("网络或数据暂不可用"); retry=true; }
                    android.util.Log.w("AirJob","组件"+id+"空气质量更新失败",e);
                } finally {
                    store.loading(false);
                }
            }
            boolean cancelled=Thread.currentThread().isInterrupted();
            if(!cancelled) {
                AirWidget.publishAnimated(this);
                sendBroadcast(new android.content.Intent("com.unimend.airradar.UPDATED").setPackage(getPackageName()));
                jobFinished(p,retry);
            }
            running.remove(p.getJobId());
        });
        running.put(p.getJobId(),future); return true;
    }
    @Override public boolean onStopJob(JobParameters p) {
        Future<?> f=running.remove(p.getJobId()); if(f!=null) f.cancel(true);
        for(int id:AirWidget.ids(this)) new AirStore(this,id).loading(false);
        return AirWidget.ids(this).length>0;
    }
}
