package com.unimend.airradar;

import android.content.Context;
import android.graphics.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class AirRenderer {
    private final Canvas canvas; private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); private final Typeface doto,zpix;
    private static final int WHITE=Color.WHITE,GREY=Color.rgb(145,145,145),DARK=Color.rgb(46,46,46),RED=Color.rgb(235,48,54),AMBER=Color.rgb(245,183,45),BLUE=Color.rgb(65,151,218);
    private AirRenderer(Context c,Bitmap b) { canvas=new Canvas(b); canvas.scale(b.getWidth()/512f,b.getHeight()/512f); doto=Typeface.createFromAsset(c.getAssets(),"doto.ttf"); zpix=Typeface.createFromAsset(c.getAssets(),"zpix.ttf"); }
    static Bitmap render(Context c,AirStore s,int page,int pixels,boolean feedback,int frame) { Bitmap b=Bitmap.createBitmap(pixels,pixels,Bitmap.Config.ARGB_8888); b.setDensity(Bitmap.DENSITY_NONE); new AirRenderer(c,b).draw(s,page,feedback,frame); return b; }
    private void text(String v,float x,float y,float size,int color,Typeface face) { paint.setTypeface(face); paint.setTextSize(size); paint.setColor(color); paint.setStyle(Paint.Style.FILL); paint.setStrokeWidth(1); Rect r=new Rect(); paint.getTextBounds(v,0,v.length(),r); canvas.drawText(v,x-(r.left+r.right)/2f,y-(r.top+r.bottom)/2f,paint); }
    private void draw(AirStore s,int page,boolean feedback,int frame) {
        JSONObject d=s.data(); boolean valid=d.has("aqi");
        if(!s.configured()) { radar(new JSONObject(),-1,"AIR",WHITE); text("点击选择城市",246,476,30,GREY,zpix); pages(page,feedback); return; }
        if(!valid) { radar(new JSONObject(),frame,"--",WHITE); text(s.city()+" · "+(s.loading()?"更新中":s.hasError()?"重试":"等待"),246,476,30,s.hasError()?RED:WHITE,zpix); pages(page,feedback); return; }
        if(page==0) home(s,d,frame); else forecast(s,d,frame);
        pages(page,feedback);
    }
    private void home(AirStore s,JSONObject d,int frame) {
        double aqi=d.optDouble("aqi"); int color=aqi<=50?Color.rgb(59,191,111):aqi<=100?AMBER:RED;
        text("AQI "+(int)Math.round(aqi),116,58,44,color,doto);
        text(s.city()+" · "+AirSource.level(aqi),332,58,32,WHITE,zpix);
        bars(d,frame);
        text("主导 "+AirSource.dominant(d),246,458,40,RED,zpix);
    }
    private void bars(JSONObject d,int frame) {
        String[] labels={"PM25","PM10","O3","NO2"};
        String[] raw={"pm25","pm10","o3","no2"};
        String[] indices={"pm25i","pm10i","o3i","no2i"};
        float[] centers={75,185,295,405}; float top=102,bottom=276,barWidth=52,gap=5; int segments=10; float segment=(bottom-top-gap*(segments-1))/segments;
        for(int i=0;i<4;i++) {
            double index=d.optDouble(indices[i]); int filled=(int)Math.ceil(Math.min(200,index)/200.0*segments);
            if(frame>=0) filled=Math.min(filled,(int)Math.ceil(filled*(frame+1)/8.0));
            int color=index<=50?Color.rgb(59,191,111):index<=100?AMBER:RED;
            for(int row=0;row<segments;row++) {
                float y=bottom-(row+1)*segment-row*gap;
                paint.setStyle(Paint.Style.FILL); paint.setColor(row<filled?color:Color.rgb(34,34,34));
                canvas.drawRoundRect(centers[i]-barWidth/2,y,centers[i]+barWidth/2,y+segment,4,4,paint);
            }
            text(labels[i],centers[i],318,34,WHITE,doto);
            double value=d.optDouble(raw[i]); String shown=value<100?String.format(Locale.US,"%.1f",value):String.format(Locale.US,"%.0f",value);
            text(shown,centers[i],366,38,color,doto);
        }
        text("μg/m³",246,411,29,GREY,zpix);
    }
    private void radar(JSONObject d,int frame,String center,int centerColor) {
        float cx=246,cy=242,r=140; paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3.5f);
        for(int ring=1;ring<=3;ring++) { paint.setColor(ring==3?Color.rgb(68,68,68):Color.rgb(43,43,43)); circleDots(cx,cy,r*ring/3f,36); }
        paint.setColor(Color.rgb(58,58,58)); paint.setStrokeWidth(3); canvas.drawLine(cx,cy-r,cx,cy+r,paint); canvas.drawLine(cx-r,cy,cx+r,cy,paint);
        String[] labels={"PM25","O3","PM10","NO2"}; double[] val={d.optDouble("pm25i"),d.optDouble("o3i"),d.optDouble("pm10i"),d.optDouble("no2i")};
        float[][] p=new float[4][2];
        for(int i=0;i<4;i++) { double a=-Math.PI/2+i*Math.PI/2; float rr=(float)(r*(.35+.65*Math.min(1,val[i]/200.0))); p[i][0]=cx+(float)Math.cos(a)*rr; p[i][1]=cy+(float)Math.sin(a)*rr; }
        Path poly=new Path(); poly.moveTo(p[0][0],p[0][1]); for(int i=1;i<4;i++) poly.lineTo(p[i][0],p[i][1]); poly.close();
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(76,235,48,54)); canvas.drawPath(poly,paint); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(5); paint.setColor(RED); canvas.drawPath(poly,paint);
        paint.setStyle(Paint.Style.FILL); for(float[] q:p) canvas.drawCircle(q[0],q[1],7,paint);
        text(labels[0],cx,56,44,WHITE,doto); text(labels[1],410,cy,44,WHITE,doto); text(labels[2],cx,425,44,WHITE,doto); text(labels[3],78,cy,44,WHITE,doto);
        if(frame>=0) { double a=-Math.PI/2+frame*(Math.PI*2/8); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(5); paint.setColor(Color.argb(190,255,255,255)); canvas.drawLine(cx,cy,cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,paint); paint.setStyle(Paint.Style.FILL); canvas.drawCircle(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,6,paint); }
        if(center!=null) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.BLACK); canvas.drawCircle(cx,cy,39,paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3); paint.setColor(Color.rgb(76,76,76)); canvas.drawCircle(cx,cy,39,paint);
            text(center,cx,cy,72,centerColor,doto);
        }
    }
    private void forecast(AirStore s,JSONObject d,int frame) {
        text("24H",82,62,48,WHITE,doto); text("FORECAST",292,62,38,GREY,doto);
        JSONArray a=d.optJSONArray("aqis"); if(a==null||a.length()<2) { text("NO DATA",246,260,48,RED,doto); return; }
        float l=50,t=108,w=382,h=216; double max=50; for(int i=0;i<a.length();i++) max=Math.max(max,a.optDouble(i)); max=Math.ceil(max/50)*50;
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2); for(int i=0;i<4;i++){float y=t+h*i/3f;paint.setColor(DARK);canvas.drawLine(l,y,l+w,y,paint);}
        Path path=new Path(); for(int i=0;i<a.length();i++){float x=l+w*i/(a.length()-1f),y=t+h-(float)(a.optDouble(i)/max*h);if(i==0)path.moveTo(x,y);else path.lineTo(x,y);} paint.setStrokeWidth(6);paint.setColor(RED);canvas.drawPath(path,paint);
        if(frame>=0){int i=Math.min(a.length()-1,frame*3);float x=l+w*i/(a.length()-1f),y=t+h-(float)(a.optDouble(i)/max*h);paint.setStyle(Paint.Style.FILL);paint.setColor(WHITE);canvas.drawCircle(x,y,7,paint);}
        text("NOW",76,350,30,GREY,doto); text("+12H",246,350,30,GREY,doto); text("+24H",404,350,30,GREY,doto);
        text(time(d.optLong("peakTime"))+" · 峰值 "+(int)Math.round(d.optDouble("peak")),246,404,34,WHITE,zpix);
        text("主导 "+AirSource.dominant(d),246,459,40,s.stale(d)?AMBER:RED,zpix);
    }
    private void circleDots(float cx,float cy,float r,int count) { paint.setStyle(Paint.Style.FILL); for(int i=0;i<count;i++){double a=i*Math.PI*2/count;canvas.drawCircle(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,3.1f,paint);} }
    private void pages(int page,boolean feedback) { for(int i=0;i<2;i++){paint.setColor(i==page?(feedback?RED:WHITE):Color.rgb(60,60,60));canvas.drawCircle(466,224+i*28,6,paint);} }
    private String time(long ms) { if(ms<=0)return "--:--"; SimpleDateFormat f=new SimpleDateFormat("HH:mm",Locale.US); return f.format(new Date(ms)); }
}
