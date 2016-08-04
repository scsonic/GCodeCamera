package com.ygk.gcodecamera;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

public class GCodeParser {

	static public String TAG = "GCodeParser" ;
	Uri uri ;
	File file ;
	Setting set ;
	
	boolean running = true ;
	
	float maxExtrude = 0 ;
	float feedrate = 0; // �ثe��feedrate XD
	float zheight = 0 ; // �ثezHeight ~~
	// �O�� �C����e��feedrate���X�ƶq <FeedSpeed, Length (�u�⥿��)>
	Map<Float, Float> fdMap = new HashMap<Float, Float>();
	// �O�� �C����e��z height xd
	Map<Float, Float> zeMap = new HashMap<Float, Float>();
	
	ArrayList<Float> zList = new ArrayList<Float>() ; // �ثe��z �M��
	ArrayList<Float> zDiffList = new ArrayList<Float>() ; // �ثe��z �M��
	
	SettingFragment settingFragment = null ;
	
	Thread thread = null ;
	public GCodeParser(Uri uri2) throws IOException {
		uri = uri2 ;

		file = new File( uri2.getPath() ) ;

		Log.i(TAG, "GCode File Size:" + file.length() + " Name:" + file.getName() ) ;
		set = new Setting() ;
	}
	
	
	public void parseInThread()
	{
		this.thread = new Thread(){
			@Override
			public void run() {
				try {
					parse() ;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				super.run();
			}
		};
		this.thread.start();
	}
	
	/**
	 * Parse GCode���G
	 * @throws Exception 
	 */
	public void parse() throws Exception
	{
		FileInputStream fis = null;
		try {
			int maxline = -1 ;
			InputStream is ;
			
			if ( file.exists() )
			{
				Log.i(TAG, "no file, using content resolver") ;
				fis = new FileInputStream(file) ;
				//throw new IOException("�S�����ɮ�") ;
				is = fis ;
			}
			else
			{
				is = Common.activity.getContentResolver().openInputStream(uri) ;
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(is)) ;
			
			String line ;
			while ( running )
			{
				line = br.readLine() ;

				if ( line == null ) break ;
				maxline -- ;
				if ( maxline == 0 ) break ;
				
				//Log.i(TAG, "line=" + line ) ;
				
				// �ŦX�U�C��p�̸��L
				if ( line.indexOf(";") >= 0 )
				{
					//Log.i(TAG, "��" + line ) ;
				}
				
				
				if ( set.layerHight == null )
				{
					// cura?
					String temp = GCodeParser.findStringIn( line, " Layer height: ", " ") ;
					if ( temp != null && isFloat( temp ) )
					{
						set.layerHight = temp ;
						Log.i(TAG, "Parse LayerHeight:" + temp) ;
					}
					// slic3r
				    temp = GCodeParser.findStringAfter( line, "; layer_height = ") ;
					if ( temp != null && isFloat( temp ) )
					{
						set.layerHight = temp ;
						Log.i(TAG, "Parse LayerHeight:" + temp) ;
					}
					// kisslicer
					temp = findStringAfter( line, "layer_thickness_mm = " );
					if ( temp != null && isFloat( temp ) )
					{
						set.layerHight = temp ;
						Log.i(TAG, "Parse KISS LayerHeight:" + temp) ;
					}
				}
				
				if ( set.temp == null) // m109 ���X�Y
				{
					set.temp = isCmdArgStartWith(line, "M109", "S") ;
					if ( set.temp != null)
					{
						set.temp = String.format("%.0f", Float.parseFloat(set.temp)) ;
						Log.i(TAG, "Parse Temp:" + set.temp) ;
					}
				}
				// m190 bed temp
				
				// count layerHeight by gcode @@
				parseLayerHeightAndSpeed(line) ;
				
				// string find / parse value

				
			}
			

			
			if ( set.layerHight == null)
			{
				//Log.i(TAG, printZList());
				Log.i(TAG, "avgZDiff = " + getAvgZDiff() ) ;
				set.layerHight = "" +  formatFloat(getManyInArray(this.zDiffList))  ;
				Log.i(TAG, "manyZ diff = " + set.layerHight) ;
			}
			
			Log.e(TAG, "getZeMapResult = " + getzeMapResult() ) ;
			Log.e(TAG, "getLayerHeightByzeMap !!! =" + getLayerHeightByzeMap());
			Log.i(TAG, "getfdMapResult = " + getfdMapResult() ) ;
			set.speed = "" + formatFloat( getMaxFeedRate() / 60.0) ;
			Log.i(TAG, "getSetting=" + set.toJSON() ) ;
			br.close();
		}
		catch (Exception ex)
		{
			Log.e(TAG, "Parse GCode Failure" + ex.getMessage(), ex.fillInStackTrace()) ;
			throw new Exception("Parse GCode Failure" + ex.getMessage() ) ;
		}
		finally
		{
			if ( fis != null)
			{
				fis.close() ;
			}
			if ( settingFragment != null )
			{
				settingFragment.parseOK(this) ;
			}
		}
		
	}
	
	/**
	 * �˦��ۤw�n��Float
	 * @return
	 */
	public String formatFloat(float f)
	{
		return String.format("%.2f", f+0.005) ;
	}
	public String formatFloat(double d)
	{
		return String.format("%.2f", d+0.005) ;
	}
	
	
	/**
	 * �\��G
	 * �K�Wlayer height and �έp�̱`�������@��
	 */
	private void parseLayerHeightAndSpeed(String line) {
		// TODO Auto-generated method stub
		//if ( set.layerHight == null)
		{
			if ( ( line.startsWith("G0") || line.startsWith("G1") )  ) // �o��~�O������z��
			{
				String args[] = line.split(" |;|,|:");
				if (line.contains("Z"))
				{
					for ( String arg: args)
					{
						if ( arg.startsWith("Z") && arg.length() > 1)
						{
							try {
								zheight = Float.parseFloat(arg.substring(1)) ; // currentHeight XD
								zList.add( zheight );
								//Log.e(TAG, "get Z=" + arg.substring(1));
								
							}
							catch (Exception ex) 
							{
								Log.e(TAG, "�o�檺Z����Parse:" + line ) ;
							}
						}
					}	
				}
				// �p�G��F �n��sF�ƥ�
				String feedStr = getStartWithInArray( args, "F" ) ;
				if ( feedStr != null )
				{
					this.feedrate =Float.parseFloat( feedStr.substring(1) ) ;
				}
				
				/**
				 * �o�̪��t��k�O�o�ˡG�u�O�omax extrude 
				 * �p�G
				 */
				String extrudeStr = getStartWithInArray( args, "E" ) ;
				if ( extrudeStr != null ) // �n�F �n�p�����X�h��
				{
					float newE = Float.parseFloat( extrudeStr.substring(1)) ;
					
					if ( newE >= maxExtrude )
					{
						// �O�u�����X �ҥH�p�Jfeedrate map��
						if ( ! fdMap.containsKey(this.feedrate) )
						{
							fdMap.put(this.feedrate, 0f) ;
						}
						float feedlength = fdMap.get(this.feedrate) ;
						fdMap.put(this.feedrate, feedlength + (newE - maxExtrude )) ;

						// �h�p��zeMap�L�F�h��
						if ( ! zeMap.containsKey(this.zheight) )
						{
							zeMap.put(this.zheight, 0f) ;
						}
						float zelength = zeMap.get(this.zheight) ;
						zeMap.put(this.zheight, zelength + (newE - maxExtrude )) ;
						maxExtrude = newE ;
					}
					else
					{
						// �i��O�^��ɪ� ���C�J�p��
					}
					
				}
			}
		}
	}
	
	public String getfdMapResult()
	{
		String output = "";
		for(Entry<Float, Float> entry : fdMap.entrySet()) {
			Float key = entry.getKey();
			Float value = entry.getValue();
			output += "Feedrate:" + key + ", extrude:" + value + "\n";
		}
		return output ;
	}
	
	public float getMaxFeedRate()
	{
		float max = 0 ;
		float maxfd = 0 ;
		for(Entry<Float, Float> entry : fdMap.entrySet()) {
			Float key = entry.getKey();
			Float value = entry.getValue();
			if ( value > max )
			{
				max = value ;
				maxfd = key ;
				
			}
		}
		return maxfd  ;
	}
	
	public String getzeMapResult()
	{
		String output = "";
		for(Entry<Float, Float> entry : zeMap.entrySet()) {
			Float key = entry.getKey();
			Float value = entry.getValue();
			output += "zheight:" + key + ", extrude:" + value + "\n";
		}
		return output ;
	}
	
	public float getLayerHeightByzeMap()
	{
		String output = "";
		ArrayList<Float> list = new ArrayList<Float>() ;
		ArrayList<Float> diffList = new ArrayList<Float>() ;
		for(Entry<Float, Float> entry : zeMap.entrySet()) {
			Float key = entry.getKey();
			Float value = entry.getValue();
			list.add(key) ;
		}
		sortZList( list ) ;
		if ( list.size() <= 0 ) return 0 ;
		float pre = list.get(0) ;
		
		for ( int i = 1 ; i < list.size() ; i++ )
		{
			float diff = list.get(i) - list.get(i-1) ;
			
			if ( diff < 0 ) continue ; // ���Q�p�J
			
			diffList.add(diff) ;
		}
		
		return getManyInArray(diffList) ;
	}
	
	/**
	 * ���o diff ������
	 * @return
	 */
	public float getAvgZDiff()
	{
		if ( zList.size() <= 0 ) return 0 ;
		float sum = 0 ;
		float pre = zList.get(0) ;
		
		for ( int i = 1 ; i < zList.size() ; i++ )
		{
			float diff = zList.get(i) - zList.get(i-1) ;
			
			if ( diff < 0 ) continue ; // ���Q�p�J
			
			zDiffList.add(diff) ;
			
			sum += diff ;
		}
		return sum / (this.zList.size()-1) ;
	}
	
	private String getStartWithInArray( String arr[], String startWith )
	{
		for ( String a: arr)
		{
			if ( a.startsWith(startWith))
				return a ;
		}
		return null ;
	}
	
	public float getManyInArray(ArrayList<Float> array)
	{
		sortZList(array) ;
		
		float many = -1 ;
		int count = 0;
		for ( Float f: array )
		{
			if ( f >= 0.4 ) continue ; // �ܤ֤j��0.4mm���Y �����O�b��^
			if ( f == many )
			{
				count ++ ;
			}
			else 
			{
				count = 0 ;
				many = f ;
			}
		}
		
		return many ;
	}

	
	public void sortZList(ArrayList<Float> array)
	{
		//Sorting
		Collections.sort(array, new Comparator<Float>() {
		        @Override
		        public int compare(Float  f1, Float  f2)
		        {
		        	if ( f1 > f2 )
		        		return 0 ;
		        	else
		        		return 1 ; 
		        }
		    });
	}
	
	public String printZList()
	{
		String output = "zList: ";
		for ( Float f : this.zList )
		{
			output += f + "," ;
		}
		return output ;
	}
	
	/**
	 * �@��method �ΨӧP�_�O���O�Y��CMD
	 * �M��
	 * @param cmd
	 * @param argIndex
	 * @return
	 */
	public String isCmdAndArg( String line, String cmd, int argIndex)
	{
		if ( line.startsWith(cmd) )
		{
			String split[] = line.split(" |;|,|:");
			
			if ( split.length < argIndex )
			{
				return split[argIndex] ;
			}
		}
		return null ;
	}
	
	/**
	 * �@��method �ΨӧP�_�O���O�Y��CMD
	 * �M��
	 * @param cmd
	 * @param argIndex
	 * @return
	 */
	public String isCmdArgStartWith( String line, String cmd, String startWith)
	{
		if ( line.startsWith(cmd) )
		{
			String split[] = line.split(" |;|,|:");
			
			for ( String str:split)
			{
				if ( str.startsWith(startWith) )
				{
					return str.substring( startWith.length() ) ;
				}
			}
		}
		return null ;
	}

	static public boolean isFloat(String str)
	{
		try {
			float f = Float.parseFloat(str.trim()) ;
			return true ;
		}
		catch (Exception ex)
		{
		}
		return false; 
	}
	
	static public String findStringIn(String data, String start, String end)
	{
		int startAt = data.indexOf(start) ;
		int endAt = data.indexOf(end, startAt+start.length()) ;
		
		if ( startAt > 0 && endAt > 0 )
		{
			return data.substring(startAt+ start.length(), endAt) ;
		}
		return null ;
	}
	
	static public String findStringAfter(String data, String start)
	{
		int startAt = data.indexOf(start) ;
		if ( startAt == -1)
			return null;
		else 
		{
			Log.e(TAG, "find " + start + " = " + data) ;
			return data.substring(startAt + start.length(), data.length()) ;
		}
	}
}
