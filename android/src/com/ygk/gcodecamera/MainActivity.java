package com.ygk.gcodecamera;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

	static public String TAG = "MainActivity" ;
	
	public CameraFragment cameraFragment ;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Common.init(this) ;
		super.onCreate(savedInstanceState);
		
		
		cameraFragment = new CameraFragment();
		Common.cameraFragment = cameraFragment;
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, cameraFragment).commit();
		}
		
		String raw = Common.readSharePerf(Common.SETTING) ;
		Common.set = Setting.fromJSON(raw) ;
		
		if (Common.set == null)
		{
			Common.set =  Setting.getDefaultSetting() ;
		}
		
		// init color
		Setting.colorMap.put("White"	, "#FFFFFF");
		Setting.colorMap.put("Silver"	, "#C0C0C0");
		Setting.colorMap.put("Gray"	, "#808080");
		Setting.colorMap.put("Black"	, "#000000");
		Setting.colorMap.put("Red"		, "#FF0000");
		Setting.colorMap.put("Maroon"	, "#800000");
		Setting.colorMap.put("Yellow"	, "#FFFF00");
		Setting.colorMap.put("Olive"	, "#808000");
		Setting.colorMap.put("Lime"	, "#00FF00"	);
		Setting.colorMap.put("Green"	, "#008000");
		Setting.colorMap.put("Aqua"	, "#00FFFF"	);
		Setting.colorMap.put("Teal"	, "#008080"	);
		Setting.colorMap.put("Blue"	, "#0000FF"	);
		Setting.colorMap.put("Navy"	, "#000080"	);
		Setting.colorMap.put("Fuchsia"	, "#FF00FF"	);
		Setting.colorMap.put("Purple"	, "#800080");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStop() {
		
		Common.writeSharePerf(Common.SETTING, Common.set.toJSON());
		super.onStop();
	}

}
