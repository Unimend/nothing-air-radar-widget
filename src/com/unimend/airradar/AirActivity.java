package com.unimend.airradar;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.*;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.concurrent.*;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.*;

public class AirActivity extends Activity {
    private final ExecutorService io=Executors.newSingleThreadExecutor(); private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView statusValue,statusMeta,updated; private final TextView[] metricValues=new TextView[4];
    private ImageView preview; private EditText city; private TextView search; private AirStore store; private int widgetId; private Typeface doto,zpix;
    private final BroadcastReceiver updates=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){show();}};
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,0); store=new AirStore(this,widgetId);
        AirWidget.schedule(this,true);
        doto=Typeface.createFromAsset(getAssets(),"doto.ttf"); zpix=Typeface.createFromAsset(getAssets(),"zpix.ttf");
        getWindow().setStatusBarColor(Color.BLACK); getWindow().setNavigationBarColor(Color.BLACK);
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20),dp(18),dp(20),dp(36)); body.setBackgroundColor(Color.BLACK);
        ScrollView scroll=new ScrollView(this); scroll.addView(body); setContentView(scroll);
        TextView eyebrow=label("NOTHING / AIR",14,Color.rgb(235,48,54),doto); eyebrow.setLetterSpacing(.13f); body.addView(eyebrow);
        TextView title=label(widgetId>0?"空气质量雷达 · 当前组件":"空气质量雷达",29,Color.WHITE,zpix); add(body,title,-1,-2,0,dp(4),0,dp(4));
        TextView intro=label("一眼读取此刻的空气状态",15,Color.rgb(135,135,135),Typeface.DEFAULT); add(body,intro,-1,-2,0,0,0,dp(18));

        LinearLayout previewCard=card(); previewCard.setPadding(dp(8),dp(8),dp(8),dp(8));
        preview=new ImageView(this); preview.setScaleType(ImageView.ScaleType.FIT_CENTER); previewCard.addView(preview,new LinearLayout.LayoutParams(-1,dp(250))); add(body,previewCard,-1,-2,0,0,0,dp(14));
        preview.setOnClickListener(v->{store.toggle(0);show();});

        LinearLayout statusCard=card(); statusCard.setPadding(dp(18),dp(16),dp(18),dp(18));
        TextView statusLabel=label("CURRENT AIR",13,Color.rgb(130,130,130),doto); statusLabel.setLetterSpacing(.1f); statusCard.addView(statusLabel);
        statusValue=label("AQI --",43,Color.WHITE,doto); add(statusCard,statusValue,-1,-2,0,dp(5),0,0);
        statusMeta=label("选择城市",18,Color.WHITE,zpix); add(statusCard,statusMeta,-1,-2,0,dp(2),0,dp(15));
        GridLayout grid=new GridLayout(this); grid.setColumnCount(2); String[] names={"PM2.5","PM10","O₃","NO₂"};
        for(int i=0;i<4;i++){ LinearLayout cell=new LinearLayout(this); cell.setOrientation(LinearLayout.VERTICAL); cell.setPadding(i%2==0?0:dp(12),dp(9),dp(8),dp(9)); TextView n=label(names[i],13,Color.rgb(125,125,125),doto); metricValues[i]=label("--",23,Color.WHITE,doto); cell.addView(n); cell.addView(metricValues[i]); GridLayout.LayoutParams gp=new GridLayout.LayoutParams(); gp.width=0; gp.columnSpec=GridLayout.spec(i%2,1f); gp.rowSpec=GridLayout.spec(i/2); grid.addView(cell,gp); }
        statusCard.addView(grid,new LinearLayout.LayoutParams(-1,-2)); updated=label("等待首次更新",13,Color.rgb(125,125,125),Typeface.DEFAULT); add(statusCard,updated,-1,-2,0,dp(7),0,0); add(body,statusCard,-1,-2,0,0,0,dp(14));

        TextView refresh=action("刷新空气数据",true); body.addView(refresh); refresh.setOnClickListener(v->refresh());
        TextView locationTitle=label("LOCATION",14,Color.rgb(150,150,150),doto); locationTitle.setLetterSpacing(.1f); add(body,locationTitle,-1,-2,0,dp(24),0,dp(9));
        city=new EditText(this); city.setSingleLine(true); city.setTextSize(16); city.setTextColor(Color.WHITE); city.setHintTextColor(Color.rgb(105,105,105)); city.setHint("搜索城市，例如 杭州 / Shanghai"); city.setPadding(dp(16),0,dp(16),0); city.setBackground(panel(Color.rgb(15,15,15),Color.rgb(55,55,55),12)); add(body,city,-1,dp(54),0,0,0,dp(9));
        search=action("搜索并选择城市",false); body.addView(search); search.setOnClickListener(v->search());
        TextView hangzhou=action("快速选择杭州",false); add(body,hangzhou,-1,dp(52),0,dp(9),0,0); hangzhou.setOnClickListener(v->choose("杭州",30.2741,120.1551));

        TextView sourceTitle=label("DATA / MODEL",14,Color.rgb(150,150,150),doto); sourceTitle.setLetterSpacing(.1f); add(body,sourceTitle,-1,-2,0,dp(26),0,dp(8));
        TextView source=label("Open-Meteo Air Quality · CAMS\nUS AQI 为模型估算，并非中国官方地面监测值。系统约每30分钟自动刷新；省电或断网时可能延后。\n\nopen-meteo.com/en/docs/air-quality-api",13,Color.rgb(120,120,120),Typeface.DEFAULT); source.setLineSpacing(dp(4),1); android.text.util.Linkify.addLinks(source,android.text.util.Linkify.WEB_URLS); source.setMovementMethod(android.text.method.LinkMovementMethod.getInstance()); body.addView(source);
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String value,float size,int color,Typeface face){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setTypeface(face);v.setIncludeFontPadding(false);return v;}
    private GradientDrawable panel(int fill,int stroke,float radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private LinearLayout card(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setBackground(panel(Color.rgb(9,9,9),Color.rgb(40,40,40),16));return v;}
    private TextView action(String value,boolean accent){TextView v=label(value,16,accent?Color.WHITE:Color.rgb(225,225,225),accent?doto:Typeface.DEFAULT_BOLD);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(52));v.setBackground(panel(accent?Color.rgb(205,37,45):Color.rgb(17,17,17),accent?Color.rgb(235,48,54):Color.rgb(55,55,55),12));v.setClickable(true);return v;}
    private void add(LinearLayout parent,View child,int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);parent.addView(child,p);}
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
        if(!store.configured()){statusValue.setText("AQI --");statusMeta.setText("请选择城市");updated.setText("每个桌面组件可设置不同城市");for(TextView v:metricValues)v.setText("--");return;}
        if(!d.has("aqi")){statusValue.setText("AQI --");statusMeta.setText(store.city()+" · "+(store.hasError()?"更新失败":"等待数据"));updated.setText(store.loading()?"正在连接空气模型…":"点击刷新后重试");for(TextView v:metricValues)v.setText("--");return;}
        SimpleDateFormat f=new SimpleDateFormat("MM-dd HH:mm",Locale.US);
        double aqi=d.optDouble("aqi"); statusValue.setText("AQI "+Math.round(aqi)); statusValue.setTextColor(aqi>100?Color.rgb(235,48,54):Color.WHITE); statusMeta.setText(store.city()+" · "+AirSource.level(aqi)+" · 主导 "+AirSource.dominant(d));
        double[] values={d.optDouble("pm25"),d.optDouble("pm10"),d.optDouble("o3"),d.optDouble("no2")};for(int i=0;i<4;i++)metricValues[i].setText(String.format(Locale.US,"%.1f μg/m³",values[i]));
        updated.setText((store.stale(d)?"数据可能已过期 · ":"模型更新时间 · ")+f.format(new Date(d.optLong("observed"))));
    }
    @Override protected void onResume(){super.onResume();registerReceiver(updates,new IntentFilter("com.unimend.airradar.UPDATED"));show();}
    @Override protected void onPause(){unregisterReceiver(updates);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
}
