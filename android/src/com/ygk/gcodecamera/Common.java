package com.ygk.gcodecamera;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class Common {

	
	public static String SETTING = "SETTING";
	
	static public int wentWidth = 1280 ;
	static public int wentHeight = 960 ;
	
	static public int screenWidth = 1280 ; // 等下由其他東西蓋過
	static public int screenHeight = 960 ;
	
	static public String TAG = "Common" ;
	static public Activity activity ;
	static public Setting set ;
	static public CameraFragment cameraFragment = null ; // 給別人呼叫 以便即時更新preview內容
	
	
	static public void init(Activity activity)
	{
		Common.activity = activity ;
		
		DisplayMetrics displayMetrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE); // the results will be higher than using the activity context object or the getWindowManager() shortcut
		wm.getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth = displayMetrics.widthPixels;
		screenHeight = displayMetrics.heightPixels;
		
		if ( screenWidth < screenHeight ) {
			int t = screenWidth ;
			screenWidth = screenHeight ;
			screenHeight = t ;
		}
		
	}
	public static Bitmap getRotateBitmap(Bitmap b, float rotateDegree){
	    Matrix matrix = new Matrix();
	    matrix.postRotate((float)rotateDegree);
	    Bitmap rotaBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, false);
	    return rotaBitmap;
	}
	
	
	static public String tempTitle ;
	static public String tempMessage ;
	static public void ShowAlert(String title, String message)
	{
		tempTitle = title ;
		tempMessage = message ;
		activity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
				alertDialog.setTitle(tempTitle);
				alertDialog.setMessage(tempMessage);
				alertDialog.show();
			}
		});
	}
	
	
	

	/**
     * 備份root路徑 
     */
    static public boolean writeSharePerf(String tag, String data)
    {
        try {
            SharedPreferences pref = activity.getSharedPreferences(tag, Context.MODE_PRIVATE);
            Editor editor = pref.edit() ;
            editor.putString(tag, data ) ;
            editor.commit() ;
            editor.apply() ;
            return true; 
        }
        catch (Exception ex)
        {
            return false ;
        }
    }
    
    /**
     * 備份root路徑 
     */
    static public String readSharePerf(String tag)
    {
        SharedPreferences pref = activity.getSharedPreferences(tag, Context.MODE_PRIVATE);
        String data = pref.getString(tag, "") ;
        
        if ( data.equalsIgnoreCase(""))
            return null ;
        else
            return data ;
    }
    
    

	static public String http_get(String _url) throws IOException 
    {      
        try {
            URL url = new URL(_url) ;
            
            ByteArrayOutputStream tmpOut = new ByteArrayOutputStream();
            

            HttpURLConnection con = null ;
            InputStream is; 
            if ( _url.startsWith("https") )
            {
            	HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection() ;
            	httpsConnection.setRequestMethod("GET") ;
            	is = httpsConnection.getInputStream() ;
            }
            else
            {
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                is = con.getInputStream() ;
            }

            

            if ( is != null)
            {
                byte[] buf = new byte[512];
                int len = -1 ;
                while (true) {
                    len = is.read(buf);
                    if (len == -1) {
                        break;
                    }
                    tmpOut.write(buf, 0, len);
                }
                tmpOut.close();
                is.close();
            }
            else
            {
                Log.e(TAG, "is = null") ;
            }

            String result ;
            try {
                result = new String(tmpOut.toByteArray(), "UTF-8") ;
                Log.i(TAG, url + " raw data:" + result ) ;
                return result;
            }
            catch (Exception ex)
            {
                Log.e(TAG, "result to String error:" + ex.getMessage()) ;
                return "";
            }
            
            
        } catch (IOException e) {
            throw new IOException("網路出錯" + e.getMessage() ) ;
        }
    }
    
	
	

	static public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) 
    {
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;
	    
	    if (height > reqHeight || width > reqWidth) {
	
	        final int halfHeight = height / 2;
	        final int halfWidth = width / 2;
	
	        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
	        // height and width larger than the requested height and width.
	        while ((halfHeight / inSampleSize) > reqHeight
	                && (halfWidth / inSampleSize) > reqWidth) {
	            inSampleSize *= 2;
	        }
	    }

	    return inSampleSize;
	}
}
