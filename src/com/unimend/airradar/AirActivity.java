package com.unimend.airradar;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.widget.*;
import java.util.concurrent.*;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.*;

public class AirActivity extends Activity {
    private final ExecutorService io=Executors.newSingleThreadExecutor(); private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView details; private ImageView preview; private EditText city; private Button search; private AirStore store; private int widgetId;
    private final BroadcastReceiver updates=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){show();}};
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,0); store=new AirStore(this,widgetId);
        getWindow().setStatusBarColor(Color.BLACK); getWindow().setNavigationBarColor(Color.BLACK);
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(24),dp(20),dp(24),dp(28)); body.setBackgroundColor(Color.BLACK);
        ScrollView scroll=new ScrollView(this); scroll.addView(body); setContentView(scroll);
        TextView title=new TextView(this); title.setText(widgetId>0?"空气质量雷达 · 当前组件":"空气质量雷达"); title.setTextSize(27); title.setTextColor(Color.WHITE); body.addView(title);
        preview=new ImageView(this); preview.setScaleType(ImageView.ScaleType.FIT_CENTER); body.addView(preview,new LinearLayout.LayoutParams(-1,dp(240))); preview.setOnClickListener(v->{store.toggle(0);show();});
        details=new TextView(this); details.setTextSize(16); details.setTextColor(Color.LTGRAY); details.setLineSpacing(dp(5),1); body.addView(details);
        Button refresh=new Button(this); refresh.setText("刷新空气数据"); body.addView(refresh); refresh.setOnClickListener(v->refresh());
        city=new EditText(this); city.setSingleLine(true); city.setTextColor(Color.WHITE); city.setHintTextColor(Color.GRAY); city.setHint("搜索城市，例如 杭州 / Shanghai"); body.addView(city);
        search=new Button(this); search.setText("搜索并选择城市"); body.addView(search); search.setOnClickListener(v->search());
        Button hangzhou=new Button(this); hangzhou.setText("选择杭州"); body.addView(hangzhou); hangzhou.setOnClickListener(v->choose("杭州",30.2741,120.1551));
        TextView source=new TextView(this); source.setTextSize(13); source.setTextColor(Color.GRAY); source.setText("数据：Open-Meteo Air Quality / CAMS 模型\nUS AQI 使用美国标准，并非中国官方 AQI。MODEL 表示模型估算，不冒充地面监测站实测。\n约30分钟后台刷新；右侧点击切换雷达与24小时趋势。\n\nhttps://open-meteo.com/en/docs/air-quality-api"); android.text.util.Linkify.addLinks(source,android.text.util.Linkify.WEB_URLS); source.setMovementMethod(android.text.method.LinkMovementMethod.getInstance()); body.addView(source);
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void choose(String name,double lat,double lon){store.city(name,lat,lon);store.loading(false);if(widgetId>0)AirWidget.draw(this,widgetId,false);else AirWidget.publish(this);show();Toast.makeText(this,"已为当前组件选择"+name,Toast.LENGTH_SHORT).show();refresh();}
    private void refresh(){
        if(!store.configured()){Toast.makeText(this,"请先选择城市",Toast.LENGTH_SHORT).show();return;} store.loading(true); show(); if(widgetId>0)AirWidget.draw(this,widgetId,false);
        final double lat=store.lat(),lon=store.lon(); io.execute(()->{try{JSONObject data=AirSource.fetch(lat,lon);if(store.lat()==lat&&store.lon()==lon)store.save(data);}catch(Exception e){store.error("网络或数据暂不可用");android.util.Log.w("AirActivity","空气质量更新失败",e);}finally{store.loading(false);handler.post(()->{if(isFinishing()||isDestroyed())return;show();if(widgetId>0)AirWidget.animate(this,widgetId);});}});
    }
    private void search(){
        String name=city.getText().toString().trim();if(name.length()<2){city.setError("请输入至少两个字符");return;}search.setEnabled(false);search.setText("搜索中…");
        io.execute(()->{try{JSONArray results=AirSource.search(name);handler.post(()->{if(isFinishing()||isDestroyed())return;search.setEnabled(true);search.setText("搜索并选择城市");if(results==null||results.length()==0){Toast.makeText(this,"没有找到，可尝试英文城市名",Toast.LENGTH_LONG).show();return;}String[] labels=new String[results.length()];for(int i=0;i<labels.length;i++){JSONObject r=results.optJSONObject(i);labels[i]=r.optString("name")+" · "+r.optString("admin1")+" · "+r.optString("country");}new AlertDialog.Builder(this).setTitle("选择城市").setItems(labels,(dialog,index)->{JSONObject r=results.optJSONObject(index);choose(r.optString("name"),r.optDouble("latitude"),r.optDouble("longitude"));}).show();});}catch(Exception e){handler.post(()->{if(isFinishing()||isDestroyed())return;search.setEnabled(true);search.setText("搜索并选择城市");Toast.makeText(this,"城市搜索失败",Toast.LENGTH_LONG).show();});}});
    }
    private void show(){
        preview.setImageBitmap(AirRenderer.render(this,store,store.page(0),Math.min(1000,dp(240)),false,-1)); JSONObject d=store.data();
        if(!store.configured()){details.setText("先选择城市，再将1×1“空气质量雷达”放到桌面。每个组件实例可设置不同城市。");return;}
        if(!d.has("aqi")){details.setText(store.city()+"\n"+(store.hasError()?"请求失败，请联网后刷新":"等待空气质量数据…"));return;}
        SimpleDateFormat f=new SimpleDateFormat("MM-dd HH:mm",Locale.US);
        details.setText(String.format(Locale.US,"%s · US AQI %.0f · %s\n主导污染物 %s\nPM2.5 %.1f · PM10 %.1f μg/m³\nO₃ %.1f · NO₂ %.1f · SO₂ %.1f μg/m³\n沙尘 %.1f μg/m³ · 草花粉 %.1f grains/m³\n模型数据时间 %s%s",store.city(),d.optDouble("aqi"),AirSource.level(d.optDouble("aqi")),AirSource.dominant(d),d.optDouble("pm25"),d.optDouble("pm10"),d.optDouble("o3"),d.optDouble("no2"),d.optDouble("so2"),d.optDouble("dust"),d.optDouble("pollen"),f.format(new Date(d.optLong("observed"))),store.stale(d)?"\n缓存已过期或刷新失败":""));
    }
    @Override protected void onResume(){super.onResume();registerReceiver(updates,new IntentFilter("com.unimend.airradar.UPDATED"));show();}
    @Override protected void onPause(){unregisterReceiver(updates);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
}
