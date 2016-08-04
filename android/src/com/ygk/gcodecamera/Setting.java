package com.ygk.gcodecamera;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View.MeasureSpec;
import android.widget.TextView;


/**
 * 要寫 from json/to json
 * @author 1310081
 *
 */
public class Setting {
	
	static public String TAG = "Setting" ;
	String printerName ;
	String layerHight ;
	String speed ;
	String material ;
	String temp ; // 溫度
		
	// text setting
	String textColor ;
	String textSize ;
	String textPosition ;
	
	String signature ;
	
	
	
	public String toJSON()
	{
		try {
			JSONObject obj = new JSONObject() ;
			
			obj.put("printerName", printerName) ;
			obj.put("layerHight", layerHight) ;
			obj.put("speed", speed) ;
			obj.put("material", material) ;
			obj.put("textColor", textColor) ;
			obj.put("textSize", textSize) ;
			obj.put("textPosition", textPosition) ;
			obj.put("signature", signature);
			obj.put("temp", temp) ;
			
			return obj.toString() ;
		}
		catch (Exception ex)
		{
			return null ;
		}
		
	}
	
	public void toTextView( TextView textview)
	{

		if ( textview == null ) return ;
		
		

		
		
		try {
			int textSizeInt = (int) Float.parseFloat(this.textSize) ;
			if ( textSizeInt > 120) { 
				textSizeInt = 120 ;
				this.textSize = "120" ;
			}
			
			textview.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInt);
			textview.setTextColor( StringToColorInt(this.textColor)) ;
		}
		catch (Exception ex)
		{
			textview.setTextSize(TypedValue.COMPLEX_UNIT_PX, 40);
			textview.setTextColor( Color.RED ) ;
		}
		//textview.setTextSize(TypedValue.COMPLEX_UNIT_PX, 36); // 固定的啦
		
		
		String output = "" ; 		
		if ( this.printerName != null && this.printerName.length() != 0 )
		{
			output += this.printerName + "\n" ;	
		}
		if (this.layerHight != null && this.layerHight.length() != 0 )
		{
			output += "Layer Height: " + this.layerHight + "\n" ;
		}
		if ( this.material != null && this.material.length() != 0 )
		{
			output += "Material: " + this.material + "\n" ;
		}
		if ( this.temp != null && this.temp.length() != 0 )
		{
			output += "Temp: " + this.temp + "\n" ;
		}
		if ( this.speed != null && this.speed.length() != 0 )
		{
			output += "Speed: " + this.speed + " mm/s\n" ;
		}
		if ( this.signature != null && this.signature.length() != 0 )
		{
			output += this.signature ;
		}
		textview.setText(output) ;
	}
	

	
	
	public TextView makeTextView( )
	{
		TextView view= new TextView(Common.activity) ;
		view.setDrawingCacheEnabled(true);  
		toTextView(view);
		view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));   
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());  

		return view;  
	}
	
	
	static public String COLOR_LIST [] = { 
		"White"	, 
		"Silver",
		"Gray"	, 
		"Black"	, 
		"Red"		, 
		"Maroon"	,
		"Yellow"	,
		"Olive"	,
		"Lime"	, 
		"Green"	, 
		"Aqua"	,
		"Teal"	, 
		"Blue"	,
		"Navy"	, 
		"Fuchsia"	, 
		"Purple"
	};
	
	static public String SIZE_LIST [] = { 
		"10"	, 
		"20"	,
		"30"	, 
		"40"	, 
		"50"	, 
		"60"	,
		"70"	,
		"80"
	};
	
	static public Map<String,String> colorMap = new HashMap<String, String>() ;
	
	
	static public String MATERIAL_LIST[] = {
		"ABS",
		"PVA",
		"PLA",
		"Wood",
		"Nylon",
		""		
	} ;
	
	
	// not use now
	static public String POSITION_LIST[] = {
		"Top-Left",
		"Top-Right",
		"Bottom-Left",
		"Bottom-Right"
	} ;
	
	// should from web list
	static private ArrayList<String> printList = new ArrayList<String>() ;
	static private String PRINTER_LIST_JSON = "PRINTER_LIST_JSON" ;
	
	/**
	 * 每次都讀到ram中
	 * 若ram有了 就再也不讀了 直到下次重開程式
	 * @return
	 */
	public ArrayList<String> getPrinterList()
	{
		if ( printList.size() == 0 )
		{
			// 新增原本的printList with out int
			try {
				String raw = Common.http_get("http://128.199.211.104/gcodecamera/printerlist.php") ;
				JSONArray arr = new JSONArray(raw) ;
				
				for ( int i = 0 ; i < arr.length() ; i++ )
				{
					printList.add(arr.getString(i));
				}
			}
			catch (Exception ex)
			{
				Log.e(TAG, "getPrintList error:" + ex.getMessage(), ex) ;
				// 讀取fail local 的
				printList.add("Internet Error") ;
			}
		}
		//			
		return printList ;
	}

	/**
	 * 字串轉成Color
	 * @param str
	 * @return
	 */
	public int StringToColorInt(String str)
	{
		try {
			
			return Color.parseColor(colorMap.get(str).toString()) ;
		}
		catch (Exception ex)
		{
			Log.e(TAG, "Color Parse Failure:" + str + "," + ex.getMessage() ) ;
			return Color.WHITE ;
		}
		
	}
	
	static public Setting fromJSON(String json)
	{
		try 
		{
			JSONObject obj = new JSONObject(json) ;
			Setting set = new Setting() ;
			
			set.printerName = obj.getString("printerName") ;
			set.layerHight = obj.getString("layerHight") ;
			set.speed = obj.getString("speed") ;
			set.material = obj.getString("material") ;
			set.textColor = obj.getString("textColor") ;
			set.textSize = obj.getString("textSize") ;
			set.textPosition = obj.getString("textPosition") ;
			set.signature = obj.getString("signature") ;
			set.temp = obj.getString("temp") ;
						
			return set;
		}
		catch (Exception ex)
		{
			return null ;
		}
	}
	
	static public Setting getDefaultSetting()
	{
		Setting set = new Setting();
		set.printerName = "My3DPrint" ;
		set.layerHight = "0.2" ;
		set.speed = "40" ;
		set.material = "PLA" ;
		set.textColor = "Yellow" ;
		set.textSize = "50" ;
		set.temp = ""; 
		set.signature = "GooglePlay: GCode Camera" ;
		return set ;
	}

	/**
	 * 把自已 跟 傳進來的比較
	 * 如果傳進來的有值 就蓋過 不然就不理它
	 * @param set
	 */
	public void overrideSet(Setting set) {
		if ( set.layerHight != null )
			this.layerHight = set.layerHight ;
		if ( set.speed != null )
			this.speed = set.speed ;
		if ( set.temp != null)
			this.temp = set.temp ;
	}
}
