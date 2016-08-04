package com.ygk.gcodecamera;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class CameraFragment extends Fragment implements SurfaceHolder.Callback, Runnable{


	static public int SELECT_IMAGE_RESULT = 54321 ;
	
	static public String TAG = "CameraFragment" ;
	@SuppressWarnings("deprecation")
	Camera mCamera;
	SurfaceView previewSurfaceView;
	SurfaceHolder previewSurfaceHolder;
	
	ImageButton ivInfo ;
	ImageButton ivSetting ;
	ImageButton btnTakePhoto ;
	TextView tvPreview ;
	ImageView ivPreview ;
	ImageView ivPreviewFull ;
	
	Button btnLoadImage ;
	ProgressBar pbLoading ;
	
	boolean previewing = false;
	Thread thread = null ;
	ConcurrentLinkedQueue<String> scanQueue = new ConcurrentLinkedQueue<String>() ;
	
	public CameraFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		
		ivInfo = (ImageButton) rootView.findViewById(R.id.ivInfo) ;
		ivSetting = (ImageButton) rootView.findViewById(R.id.ivSetting) ;
		btnTakePhoto = (ImageButton) rootView.findViewById(R.id.btnTakePhoto) ;
		previewSurfaceView = (SurfaceView) rootView.findViewById(R.id.svCamera ) ;
		tvPreview = (TextView) rootView.findViewById(R.id.tvPreview) ;
		ivPreview = (ImageView) rootView.findViewById(R.id.ivPreview) ;
		ivPreviewFull = (ImageView) rootView.findViewById(R.id.ivPreviewFull) ;
		
		btnLoadImage = (Button) rootView.findViewById(R.id.btnLoadImage) ;
		pbLoading = (ProgressBar) rootView.findViewById(R.id.pbLoading);
		
		
		tvPreview.setDrawingCacheEnabled(true);
	
		previewSurfaceHolder = previewSurfaceView.getHolder();
        previewSurfaceHolder.addCallback(this);
        previewSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        btnTakePhoto.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				takePicture() ;
			}
		}) ;
        
        ivSetting.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.i(TAG, "Open Settings Tab") ;
				Fragment fragment = new SettingFragment() ;
				getActivity().getFragmentManager().beginTransaction().addToBackStack(null).add(R.id.container, fragment).commit();
			}
		}) ;
        
        
        ivInfo.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {

		    	Activity activity = getActivity() ;
		    	if ( activity != null  )
		    	{
			    	AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());  
			    	String message = "\n" +
			    			"Thank you for Download GCode Camera! \n\n" +
			    			"you can mark your Print Setting on Photo very easy!!\n" +
			    			"\n" +
			    			"1.Take Photo and mark your setting.\n" +
			    			"2.Load Photo from your Album and mark setting\n" +
			    			"3.Load GCode file to Setting(Layer Height, Speed, Temp), you don't need type again.\n" +
			    			"4.Manaul Change Settings from setting panel, leave Empty for disable it. \n" +
			    			"5.Change Text Size, Color, Signature for youself.\n" +
			    			"\n" +
			    			"If you went to more function or parse another value from GCode, Just leave comment or mail in Google Play Store.\n" +
			    			"IOS version Coming soon.\n" +
			    			"\n" +
			    			"Share Your Print EveryWhere !!" ;
			    	Log.i(TAG, message) ;
			    	//alertDialog.setTitle("GCode Camera");
			    	alertDialog.setMessage(message);
			    	alertDialog.setView(LayoutInflater.from(activity).inflate(R.layout.bus_info_alert,null));
			    	alertDialog.setPositiveButton("OK", null);  
			    	AlertDialog alert = alertDialog.create();
			    	alert.show();
		    	}
			}
		});

        
        
        btnLoadImage.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/*");
				startActivityForResult(intent, SELECT_IMAGE_RESULT);
			}
		}) ;
        
        updatePreview();
        msc = new MediaScannerConnection(getActivity(), mscClient );   
        	
		return rootView;
	}
	
	
	long displayPhotoPreviewTime = 0 ;
	
	/**
	 * 顯示 照片preview
	 */
	public void displayPhotoPreview(Bitmap bitmap)
	{
		ivPreview.setImageBitmap(bitmap) ;
		ivPreview.setVisibility(View.VISIBLE) ;
		displayPhotoPreviewTime = System.currentTimeMillis() ;
		
		new Thread(){
			public void run() {
				try {
					Thread.sleep(2000) ;
					if (System.currentTimeMillis()-displayPhotoPreviewTime >1000 )
					{
						fadeOutAndHideImage(ivPreview) ;
					}
				} catch (InterruptedException e) {
					Log.e(TAG, "Fade out Error") ;
				}
				
			};
		}.start();
	}
	
	/**
	 * 顯示 照片preview
	 */
	public void displayPhotoPreviewFullScreen(Bitmap bitmap)
	{
		ivPreviewFull.setImageBitmap(bitmap) ;
		ivPreviewFull.setVisibility(View.VISIBLE) ;
		displayPhotoPreviewTime = System.currentTimeMillis() ;
		
		new Thread(){
			public void run() {
				try {
					Thread.sleep(3000) ;
					getActivity().runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							//pbLoading.setVisibility(View.GONE) ;
						}
					});
					
					if (System.currentTimeMillis()-displayPhotoPreviewTime >1000 )
					{
						fadeOutAndHideImage(ivPreviewFull) ;
					}
				} catch (InterruptedException e) {
					Log.e(TAG, "Fade out Error") ;
				}
				
			};
		}.start();
	}
	
	private void fadeOutAndHideImage(final ImageView img) 
	{
		getActivity().runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Animation fadeOut = new AlphaAnimation(1, 0);
				fadeOut.setInterpolator(new AccelerateInterpolator());
				fadeOut.setDuration(500);

				fadeOut.setAnimationListener(new AnimationListener() {
					public void onAnimationEnd(Animation animation) {
						img.setVisibility(View.GONE);
					}

					public void onAnimationRepeat(Animation animation) {
					}

					public void onAnimationStart(Animation animation) {
					}
				});
				img.startAnimation(fadeOut);
			}
		}) ;
		
	}

	/**
	 * 呼叫按下事件
	 */
	public void takePicture()
	{
		if ( mCamera != null )
		{
			try {
				mCamera.takePicture(null, null, new PictureCallback(){
					@Override
					public void onPictureTaken(byte[] data, Camera camera) {
					
						Log.i(TAG, "收到Picture bytes=" + data.length) ;
						
						Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length).copy(Config.ARGB_8888, true); ;
						
						int r = getActivity().getWindowManager().getDefaultDisplay().getRotation() ; // 0 1 2 3 = 0~270
						//bitmap = getRotatedBitmap(bitmap, r*90) ;
						
						// 依照螢幕方向旋轉 bitmap 確定要畫的字只會在左下角
						
						Log.i(TAG, "取得圖片:" + bitmap.getWidth() + bitmap.getHeight() ) ;
						
						markOnPicture(bitmap) ;
						displayPhotoPreview( bitmap) ;
						insertImage(bitmap) ;
						
						System.gc() ;
						mCamera.startPreview() ;
					}
				})  ;
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Take Picture fail -____- is camera busy???" + ex.getMessage());
			}
		}
	}
	
	
	/**
	 * 插入bitmap media store
	 * @param bitmap
	 */
	protected void insertImage(Bitmap bitmap) {

		File cacheDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File f = new File(cacheDir, "GcodeCamera_" + System.currentTimeMillis() + ".jpg");
        String path = null ;
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            path = f.getAbsolutePath() ;

            if ( path != null )
    	    {
            	Uri uri = CameraFragment.addImageToGallery(this.getActivity(),
            			f.getAbsolutePath(), "GCode Camera", Common.set.toString()) ;
            	
    	        //String result = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), path, "GCodeCamera","") ;
 
    	        Log.i(TAG, "insert Image return:" + uri.toString()) ;
    	        	
    	       	scanQueue.add(uri.toString()) ;
    	       	if ( !msc.isConnected() ) {
    	       		msc.connect() ;
    	       	}
    	      	//f.delete();
    	    }
        //} catch (FileNotFoundException e) {
        //    e.printStackTrace();
        //} catch (IOException e) {
        //    e.printStackTrace();
        } catch (Exception ex) {
            Common.ShowAlert("Error", "Can't Insert Image to Android MediaStore") ;
        }
	}
	
	public static Uri addImageToGallery(Context context, String filepath, String title, String description) {    
	    ContentValues values = new ContentValues();
	    values.put(Media.TITLE, title);
	    values.put(Media.DESCRIPTION, description); 
	    values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
	    values.put(Images.Media.MIME_TYPE, "image/jpeg");
	    values.put(MediaStore.MediaColumns.DATA, filepath);

	    return context.getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, values);
	}

	/**
	 * 在picture上標上所需資訊
	 * @param bitmap
	 */
	protected void markOnPicture(Bitmap bitmap) {
        Canvas c = new Canvas(bitmap);
        Paint p = new Paint() ;
        //Bitmap preview = tvPreview.getDrawingCache() ;
        Bitmap preview = Common.set.makeTextView().getDrawingCache() ;
        c.drawBitmap(preview , 0 , c.getHeight()-preview.getHeight() , p);
	}


	public MediaScannerConnectionClient mscClient = new MediaScannerConnectionClient() {     
		public void onMediaScannerConnected() {     
			//msc.scanFile("/sdcard/image.jpg", "image/jpeg");     
			
			try {
				while ( true )
				{
					String uri = scanQueue.poll() ;
					if ( uri == null) break ;
					Log.i(TAG, "Scan File:" + uri ) ;
					msc.scanFile( uri, "image/jpeg") ;
				}
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Scan File Failure") ;
			}
		}     
	
		public void onScanCompleted(String path, Uri uri) {    
			if ( msc != null) 
			{
				msc.disconnect();
			}
		}     
   	};
   	MediaScannerConnection msc ;
   	
   	
    private Bitmap getRotatedBitmap(Bitmap bitmap, int rotation) {
        if (rotation % 360 == 0) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight() / 2, matrix, true);
    }
    
	public Camera openCamera()
	{
		Camera camera = null ;
		try {
			
			camera = Camera.open() ;
			if ( camera == null ) return null ;
			
			/*
			Camera.Parameters parameters = camera.getParameters();
			if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
				parameters.set("orientation", "portrait");
				
			} else {
				parameters.set("orientation", "landscape");
				camera.setDisplayOrientation(0);
			}
			*/
			
			int r = getActivity().getWindowManager().getDefaultDisplay().getRotation() ; // 0 1 2 3 = 0~270
			Log.i(TAG, "Set Camera oreation " + r ) ;
			switch (r) {
				case 0: r = 1; break ;
				case 1: r = 0; break ;
				case 2: r = 3; break ;
				case 3: r = 2; break ;
			}
			camera.setDisplayOrientation((r)*90);
			
			Camera.Parameters params = camera.getParameters() ;
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
			
			int wentWidth = Common.wentWidth;
			int wentHeight = Common.wentHeight ;
			
			// 自動尋找 大小接近1280x960的 幹 以免太大outofmemroy 等問題
			List<Size> sizeList = params.getSupportedPictureSizes() ;
			int bestDiff = 2147483647 ;
			Size bestFitSize = null ;
			for ( Size size: sizeList)
			{
				int diff = Math.abs( (size.width * size.height) - (wentWidth * wentHeight ) ) ;
				Log.i(TAG, "支援Size:" + size.width + ", " + size.height + ", diff=" + diff ) ;
				if ( diff < bestDiff )
				{ 
					bestDiff = diff ;
					bestFitSize = size ;
				}
			}
			
			 
			
			
			if ( bestFitSize == null ) return null ;
			Log.i(TAG, "@@ 選擇Size:" + bestFitSize.width + "," + bestFitSize.height ) ;

			params.setPictureSize(bestFitSize.width	, bestFitSize.height ) ;
			
			camera.setParameters( params ) ;
		}
		catch (Exception ex)
		{
			if ( camera != null ) camera.release() ;
			Log.e(TAG, "Open Camera Error:" + ex.getMessage()) ;
			return null ;
		}
		

		return camera;
	}
	
	public void updatePreview()
	{
		Common.activity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if ( Common.set != null && tvPreview != null && Common.set.textSize != null )
				{
					Common.set.toTextView( tvPreview ) ;
					// 另外依照螢幕比例去設定這個值
					// get screen 
					
					double rateW = (double) Common.screenWidth / Common.wentWidth ;
					double rateH = (double) Common.screenHeight / Common.wentHeight ;
					//Log.i(TAG, Common.screenWidth + "," + Common.screenHeight) ;
					//Log.i(TAG, Common.wentWidth + "," + Common.wentHeight) ;
					double rate = (rateW + rateH ) / 1.85 ; // 原本看是字有點小 現在把字型大一點
					
					//Log.i(TAG, "@@ rate = " + rate ) ;
					tvPreview.setTextSize( TypedValue.COMPLEX_UNIT_PX, (int) ( Integer.parseInt(Common.set.textSize) * rate) ) ; // in sp for different screen 
				}
			}

			private Activity getApplicationContext() {
				// TODO Auto-generated method stub
				return null;
			}
		}) ;

	}
	
	public void ShutterCallback()
	{
		
	}
	
	@Override
	 public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
	  if(previewing)
	  {
		  if ( mCamera != null)
		  {
			  mCamera.stopPreview();
		  }
		  previewing = false;
		  this.thread = null ;
	  }
	   
	   
	  try {
		  if ( arg0 != null && mCamera != null )
		  {  
			   mCamera.setPreviewDisplay(arg0);
			   mCamera.startPreview();
		  }
		  previewing = true;
		  this.thread = new Thread(this) ;
		  this.thread.start() ;
	  } catch (IOException e) {
	   // TODO Auto-generated catch block
	   e.printStackTrace();
	  }
	   
	   
	 }
	 
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.e(TAG, "onActivity Result" + requestCode + "," + resultCode) ;
	    /*
		if ( requestCode == CameraFragment.SELECT_GCODE_RESULT && data != null && data.getData() != null )
	    {
	    	Log.i(TAG, "選了gcode SELECT_GCODE_RESULT:" + data.getData().toString() ) ;
	    	try 
	    	{
	    		GCodeParser gcp = new GCodeParser(data.getData()) ;
	    		gcp.parse() ;
	    	}
	    	catch (Exception ex)
	    	{
	    		Log.e(TAG, "parser檔案丟excepition" + ex.getMessage() ) ;
	    	}
	    }
	    else */
		if ( requestCode == CameraFragment.SELECT_IMAGE_RESULT && data != null && data.getData() != null )
	    {
	    	//pbLoading.setVisibility(View.VISIBLE);
	    	Uri uri = data.getData();  
            Log.e(TAG, "Get URI=" + uri.toString());  
            ContentResolver cr = getActivity().getContentResolver();
            Bitmap bitmap ;
            
            try {  
            	BufferedInputStream bis = new BufferedInputStream(cr.openInputStream(uri));
            	
                // First decode with inJustDecodeBounds=true to check dimensions
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Config.ARGB_8888;
                options.inJustDecodeBounds = true;
                options.inMutable = true ;
                
                //BitmapFactory.decodeResource(res, resId, options);
                BitmapFactory.decodeStream(bis, null, options) ;
                
                
                // Calculate inSampleSize
                options.inSampleSize = Common.calculateInSampleSize(options, Common.wentWidth, Common.wentHeight);
                bis.close() ;
                bis = new BufferedInputStream( cr.openInputStream(uri) );
                Log.i(TAG, "inSampleSize=" + options.inSampleSize);
                
                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;
                
                try {
                	bitmap = BitmapFactory.decodeStream(bis, null, options);
                	markOnPicture(bitmap) ;
                	//displayPhotoPreview(bitmap) ;
                	displayPhotoPreviewFullScreen(bitmap) ;
                	insertImage(bitmap) ;
                	
                }
                catch (OutOfMemoryError error)
                {
                	Common.ShowAlert("Error", "Image to big, try smaller one") ;
                }
                System.gc() ;
            } catch ( IOException e) {  
                Log.e("Exception", e.getMessage(),e);  
            }  
	    }
	    else
	    {
	    	//pbLoading.setVisibility(View.GONE);
	    }
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onPause() {
		
		updatePreview();
		super.onPause();
	}
	
	@Override
	public void onHiddenChanged(boolean hidden) {
		updatePreview();
		super.onHiddenChanged(hidden);
	}
	
	@Override
	public void onResume() {
		Log.i(TAG, "Resume Camera Fragment") ;
		
		updatePreview();
		// parse setting
		super.onResume();
	}
	
	 @Override
	 public void surfaceCreated(SurfaceHolder arg0) {
	  // TODO Auto-generated method stub
	  mCamera = openCamera() ;
	  if ( mCamera == null)
	  {
		  //show alert "不能打開相機" ;
	  }
	 }
	 
	 @Override
	 public void surfaceDestroyed(SurfaceHolder arg0) {
	  // TODO Auto-generated method stub

		 if ( mCamera != null )
		 {
			  mCamera.stopPreview();
			  mCamera.release();
		 }
	  mCamera = null;
	  previewing = false;
	  this.thread = null ;
	 }

	@Override
	public void run() {
		
		while( previewing )
		{
			try {
				updatePreview();
				Thread.sleep(1000) ;
			}
			catch (Exception ex)
			{
				Log.e(TAG, "update preview failure") ;
			}
		}
	}
}