package com.ygk.gcodecamera;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class SettingFragment extends Fragment {
	
	static public int SELECT_GCODE_RESULT = 12345 ;
	static public String TAG = "SettingFragment";

	EditText etPrinterName;
	Button btnSelectPrinterName;
	EditText etLayerHeight ;
	
	EditText etMaterialType;
	Button btnSelectMaterial;
	
	EditText etSpeed;
	EditText etTextSize ;
	Button btnSelectSize; 
	
	EditText etTextColor;
	Button btnSelectColor ;
	
	EditText etTextPosition ;
	Button btnSelectTextPosistion ;
	
	
	EditText etTemp ;
	
	ImageView ivHideSetting ;
	Button btnOK ;
	Button btnCancel ;
	Button btnRestoreDefault ;
	
	Button ivFromGCode ;
	
	EditText etSignature ;
	ArrayList<String> printList = new ArrayList<String>() ;
	
	ProgressDialog pdLoading ;
	
	/*
	 * 記錄一下要什麼 Global for one TextView:
	 * 
	 * TextColor = select or rgb 
	 * Bold = true false
	 * TextFont => 內建三樣可選 Sans、Serif 及 Monospace
	 * 
	 * 設定
	 * Machine Name
	 * Layer Height
	 * Metrial: PLA/ABS,other
	 * Speed: 00
	 * Temp: 溫度
	 */
	public SettingFragment() {
		// 
	}
	

	long lastSelectPrinterNameTime = 0 ;
	private AlertDialog.Builder selectPrintAlert ;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_setting, container, false);
        
		
		ivFromGCode = (Button) rootView.findViewById(R.id.ivFromGCode) ;
		etPrinterName = (EditText) rootView.findViewById(R.id.etPrinterName) ;
		btnSelectPrinterName = (Button) rootView.findViewById(R.id.btnSelectPrinterName);
		etLayerHeight  = (EditText) rootView.findViewById(R.id.etLayerHeight);
		
		etMaterialType = (EditText) rootView.findViewById(R.id.etMaterialType);
		btnSelectMaterial = (Button) rootView.findViewById(R.id.btnSelectMaterial);
		etSpeed = (EditText) rootView.findViewById(R.id.etSpeed);
		
		etTemp = (EditText) rootView.findViewById(R.id.etTemp) ;
		// need parse 上面的
		
		// 以下的不用parse
		
		etTextSize  = (EditText) rootView.findViewById(R.id.etTextSize);
		btnSelectSize = (Button) rootView.findViewById(R.id.btnSelectSize); 
		
		etTextColor = (EditText) rootView.findViewById(R.id.etTextColor);
		btnSelectColor  = (Button) rootView.findViewById(R.id.btnSelectColor);
		
		etTextPosition = (EditText) rootView.findViewById(R.id.etTextPosition) ;
		btnSelectTextPosistion = (Button) rootView.findViewById(R.id.btnSelectTextPosistion) ;
		
		etSignature = (EditText) rootView.findViewById(R.id.etSignature) ;
		
		ivHideSetting = (ImageView) rootView.findViewById(R.id.ivHideSetting);
		btnOK = (Button) rootView.findViewById(R.id.btnOK);
		btnCancel = (Button) rootView.findViewById(R.id.btnCancel);
		btnRestoreDefault = (Button) rootView.findViewById(R.id.btnRestoreDefault) ;
		
		
        ivFromGCode.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final PackageManager packageManager = getActivity().getPackageManager();
				final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("text/plain");
				List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES);
				
				Log.e(TAG, "resolve info size=" + list.size()) ;
				startActivityForResult(Intent.createChooser(intent, "Select GCode"), SELECT_GCODE_RESULT);
			}
		}) ;
        
        
		btnOK.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				saveSetting() ;
				getActivity().getFragmentManager().popBackStack();
			}
		}) ;
		
		btnCancel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				getActivity().getFragmentManager().popBackStack();
			}
		}) ;
		
		btnRestoreDefault.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Common.set = Setting.getDefaultSetting() ;
				setBySetting(Common.set) ;
			}
		}) ;
		
		ivHideSetting.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				saveSetting();
				getActivity().getFragmentManager().popBackStack();
			}
		});
		
		setBySetting(Common.set) ;
		
		btnSelectColor.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				DialogInterface.OnClickListener di = new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int which) {
	                	etTextColor.setText(Setting.COLOR_LIST[which]);
	                    dialog.cancel();
	                    Log.i(TAG, "you select item:" + which ) ;
	                }
	            };
				showSelect( etTextColor, Setting.COLOR_LIST, di) ;
			}
		}) ;
		
		btnSelectSize.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				DialogInterface.OnClickListener di = new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int which) {
	                	etTextSize.setText(Setting.SIZE_LIST[which]);
	                    dialog.cancel();
	                    Log.i(TAG, "you select item:" + which ) ;
	                }
	            };
				showSelect( etTextSize, Setting.SIZE_LIST, di) ;
			}
		}) ;
		
		btnSelectMaterial.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				DialogInterface.OnClickListener di = new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int which) {
	                	etMaterialType.setText(Setting.MATERIAL_LIST[which]);
	                    dialog.cancel();
	                    Log.i(TAG, "you select item:" + which ) ;
	                }
	            };
				showSelect( etMaterialType, Setting.MATERIAL_LIST, di) ;
			}
		}) ;
		
		btnSelectPrinterName.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if ( System.currentTimeMillis() - lastSelectPrinterNameTime > 1000 )
				{
					lastSelectPrinterNameTime = System.currentTimeMillis() ;
					new Thread(){
						public void run() {
							try {
								printList = Common.set.getPrinterList() ;
				    				
			    				final String[] ListStr = new String[printList.size()];
			    				for (int i = 0; i < printList.size(); i++) {
			    				    ListStr[i] = printList.get(i);
			    				}
			    			
				    			AlertDialog.Builder MyListAlertDialog = new AlertDialog.Builder(getActivity());
				    			MyListAlertDialog.setTitle("Select Printer");

				    			DialogInterface.OnClickListener ListClick = new DialogInterface.OnClickListener() {
				    				public void onClick(DialogInterface dialog, int which) {
				    					etPrinterName.setText(printList.get(which));
				    					dialog.cancel();
				    				}
				    			};
				    			// 建立按下取消什麼事情都不做的事件
				    			DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener() {
				    				public void onClick(DialogInterface dialog, int which) {
				    					dialog.cancel() ;
				    				}
				    			};
				    			MyListAlertDialog.setItems(ListStr, ListClick);
				    			MyListAlertDialog.setNeutralButton("取消", OkClick);
				    			
				    			selectPrintAlert = MyListAlertDialog ;
				    			
				    			getActivity().runOnUiThread(new Runnable() {
									
									@Override
									public void run() {
										selectPrintAlert.show();
									}
								});
			    			}
			    			catch (Exception ex)
			    			{
			    				Common.ShowAlert("Fail", "Unknow Error" ) ;
			    			}
			    			finally{
			    				
			    			}
							
							
							
						};
					}.start() ;
				}
			}
		}) ;
		return rootView;
	}
	
	/**
	 * 把儲存獨立出來
	 */
	public void saveSetting()
	{
		Common.set = getSetting() ;
		Common.writeSharePerf(Common.SETTING, Common.set.toJSON());
	}
	
	@Override
	public void onDetach() {
		saveSetting() ;
		super.onDetach();
	}
	
	
	public void showSelect(TextView textview, String[] arr, DialogInterface.OnClickListener dialog)
	{
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(Common.activity, android.R.layout.simple_spinner_dropdown_item, arr);
	

		AlertDialog.Builder genderBuilder = new AlertDialog.Builder(Common.activity)
            .setTitle("Select")
            .setSingleChoiceItems(adapter, 0, dialog );
		
        AlertDialog genderAlert = genderBuilder.create();
        genderAlert.show();
	}
	
	public void setBySetting(Setting set)
	{
		Log.i(TAG, "Run set by setting:" + set.toJSON()) ;
		etPrinterName.setText( set.printerName ) ;
		etLayerHeight.setText( set.layerHight ) ;
		etMaterialType.setText( set.material );
		etSpeed.setText( ""+ set.speed ) ;
		
		etTemp.setText("" + set.temp ) ;
		
		
		etTextSize.setText( set.textSize ) ;
		etTextColor.setText( set.textColor ) ;
		etTextPosition.setText( set.textPosition ) ;
		etSignature.setText( set.signature ) ;
	}
	
	public Setting getSetting()
	{
		Setting set = new Setting() ;
		set.printerName = etPrinterName.getText().toString() ;
		set.layerHight =  etLayerHeight.getText().toString() ;
		set.material = etMaterialType.getText().toString() ;
		set.speed = etSpeed.getText().toString();
		
		set.temp = etTemp.getText().toString() ;
		
		set.textSize = etTextSize.getText().toString() ;
		set.textColor = etTextColor.getText().toString() ;
		set.textPosition = etTextPosition.getText().toString() ;
		set.signature = etSignature.getText().toString();
		return set ;
	}
	
	@Override
	public void onStop() {

		super.onStop();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.e(TAG, "onActivity Result" + requestCode + "," + resultCode) ;
	    if ( requestCode == SettingFragment.SELECT_GCODE_RESULT && data != null && data.getData() != null )
	    {
	    	Log.i(TAG, "選了gcode SELECT_GCODE_RESULT:" + data.getData().toString() ) ;
	    	try 
	    	{
	    		GCodeParser gcp = new GCodeParser(data.getData()) ;
	    		gcp.settingFragment = this ; // 設定才有callback
	    		gcp.parseInThread() ;
	    		this.pdLoading = ProgressDialog.show(getActivity(), "Loading GCode", "Loading And Analysis GCode, Please wait...", false);
	    	}
	    	catch (Exception ex)
	    	{
	    		Log.e(TAG, "parser檔案丟excepition" + ex.getMessage() ) ;
	    	}
	    }
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * 給import用的callback XXD
	 * @param gCodeParser
	 */
	public void parseOK(GCodeParser gCodeParser) {
		Common.set.overrideSet( gCodeParser.set )  ;
		getActivity().runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				pdLoading.hide() ;
				setBySetting( Common.set ) ;
			}
		}) ;
	}

}
