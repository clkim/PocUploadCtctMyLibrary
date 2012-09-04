package com.example.android.photobyintent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.ctctlabs.ctctwsjavalib.CTCTConnection;
import com.ctctlabs.ctctwsjavalib.Image;


public class PhotoIntentActivity extends SherlockActivity {

	private static final int ACTION_TAKE_PHOTO_B = 1;
	private static final int ACTION_TAKE_PHOTO_S = 2;
	private static final int ACTION_TAKE_VIDEO = 3;

	private static final String BITMAP_STORAGE_KEY = "viewbitmap";
	private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
	private ImageView mImageView;
	private Bitmap mImageBitmap;
	
	private WebView webview;

	private static final String VIDEO_STORAGE_KEY = "viewvideo";
	private static final String VIDEOVIEW_VISIBILITY_STORAGE_KEY = "videoviewvisibility";
//	private VideoView mVideoView;
	private Uri mVideoUri;
	
	private final BitmapFactory.Options bmOptions = new BitmapFactory.Options();

	private static final String CURRENT_PHOTO_PATH_KEY = "currentphotopath";
	private static final String TIMESTAMP_KEY = "timestampinfilename";
	private String mCurrentPhotoPath;
    
	private CTCTConnection conn;    
    private HashMap<String, Object> attributes;
    
    private static final String REDIRECT_URI	= "https://uploadctctdomain";
    private static final String AUTHORIZE_PATH	= "https://oauth2.constantcontact.com/oauth2/oauth/siteowner/authorize"; 
    private static final String CLIENT_ID		= "8fc5424e-d919-4739-823f-f78a465b61d1";
    private String accessToken;
    private String userName;

	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";
	
	private static final String LOG_TAG = PhotoIntentActivity.class.getSimpleName();

	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
	private String timeStamp;
	private Boolean hasCameraCanceled = false;
	private Boolean hasCameraOKed = false;
	private Boolean hasStartedActivityTakePictureIntent = false;
	
	
	private class UploadImageAsyncTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... paths) {
			String picturePathSdCard = paths[0];
			
			/* Get the size of the image */
			bmOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(picturePathSdCard, bmOptions);
			if( bmOptions.outWidth==-1 || bmOptions.outHeight==-1 ) return null;
			
			// set BitmapFactory.Options object to be used by decodeFile()
			int scaleFactorUpload = calculateInSampleSize(bmOptions, 200, 150); // dim of stored uploaded picture is ~800w ~600h
			Log.d(LOG_TAG, "** picture outWidth, outHeight: "+bmOptions.outWidth+", "+bmOptions.outHeight);
			Log.d(LOG_TAG, "** inSampleSize is "+scaleFactorUpload);
			bmOptions.inJustDecodeBounds = false;
			bmOptions.inSampleSize = scaleFactorUpload;
			bmOptions.inPurgeable = true;
			
			File file = new File(picturePathSdCard);
			Bitmap bm = BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);
			
			String imageUrl;
			try {
				// rotate picture if portrait
				ExifInterface exif = new ExifInterface(picturePathSdCard);
				Log.d(LOG_TAG, "** exif orientation tag value is "+exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0));
				Log.d(LOG_TAG, "** exif image width, length, hasThumbnail: "+exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1) +
						", "+exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1) +
						", "+exif.hasThumbnail());
				if (ExifInterface.ORIENTATION_ROTATE_90 == exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
					Matrix matrix = new Matrix();
					matrix.preRotate(90f);
					bm = Bitmap.createBitmap(bm, 0, 0, bmOptions.outWidth, bmOptions.outHeight, matrix, true);
				}

				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				bm.compress(CompressFormat.JPEG, 100, bos);
				byte[] data = bos.toByteArray();

				conn = new CTCTConnection();
				userName = conn.authenticateOAuth2(accessToken);
				if (userName == null) return null;
				Log.d(LOG_TAG, "** authenticated with "+userName);
				//boolean isAuthenticated = conn.authenticate(API_KEY, USERNAME, PASSWORD);
				//if (!isAuthenticated) return null;
				//Log.d(LOG_TAG, "** authenticated with "+USERNAME);

				attributes = new HashMap<String, Object>();
				String folderId = "2";
				String fileName = "clktest"+timeStamp+".jpg";
				String description = "InvoiceNow Image";
				Image imageModelObj = conn.createImage(attributes, folderId, fileName, data, description);
				imageModelObj.commit();
				imageUrl = (String)imageModelObj.getAttribute("ImageURL");
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			} finally {
				conn = null;
				attributes = null;
			}
			
			return imageUrl;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				Toast.makeText(getApplicationContext(), "Uploaded to your CTCT MyLibrary Plus", Toast.LENGTH_LONG).show();
				Log.d(LOG_TAG, "** Image Url is "+result);
			} else {
				Log.d(LOG_TAG, "**Error in JPEG picture, not uploaded");
				Toast.makeText(getApplicationContext(), "Sorry! Failed to upload to your CTCT MyLibrary Plus", Toast.LENGTH_LONG).show();
			}
			super.onPostExecute(result);
		}
		
	}
	
	public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float)height / (float)reqHeight);
			} else {
				inSampleSize = Math.round((float)width / (float)reqWidth);
			}
		}
		return inSampleSize;
	}

	
	/* Photo album for this application */
	private String getAlbumName() {
		return getString(R.string.album_name);
	}

	
	private File getAlbumDir() {
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			
			storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()){
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}
			
		} else {
			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
		}
		
		return storageDir;
	}

	private File createImageFile() throws IOException {
		// Create an image file name
		timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		File albumF = getAlbumDir();
		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
		return imageF;
	}

	private File setUpPhotoFile() throws IOException {
		File f = createImageFile();
		mCurrentPhotoPath = f.getAbsolutePath();
		Log.d("PhotoIntentActivity", "photo path is "+mCurrentPhotoPath);
		return f;
	}

	private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
		int targetW = mImageView.getWidth();
		int targetH = mImageView.getHeight();

		/* Get the size of the image */
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;
		
		/* Figure out which way needs to be reduced less */
		int scaleFactor = 1;
		if ((targetW > 0) || (targetH > 0)) {
			scaleFactor = Math.min(photoW/targetW, photoH/targetH);	
		}
		Log.d("PhotoIntentActivity", "scale factor is "+scaleFactor);

		/* Set bitmap options to scale the image decode target */
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
		mImageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		
		/* Associate the Bitmap to the ImageView */
		mImageView.setImageBitmap(mImageBitmap);
		mVideoUri = null;
		mImageView.setVisibility(View.VISIBLE);
//		mVideoView.setVisibility(View.INVISIBLE);
	}
	
	private void setPicFromExifThumbnail() {
		try {
			ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
			if (exif.hasThumbnail()) {
				byte[] data = exif.getThumbnail();
				mImageBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
				// rotate if portrait
				if (ExifInterface.ORIENTATION_ROTATE_90 == exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
					Matrix matrix = new Matrix();
					matrix.preRotate(90f);
					mImageBitmap = Bitmap.createBitmap(mImageBitmap, 0, 0, mImageBitmap.getWidth(), mImageBitmap.getHeight(), matrix, true);
				}
			}
			/* Associate the Bitmap to the ImageView */
			mImageView.setImageBitmap(mImageBitmap);
			mVideoUri = null;
			mImageView.setVisibility(View.VISIBLE);
//			mVideoView.setVisibility(View.INVISIBLE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void galleryAddPic() {
		    Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
			File f = new File(mCurrentPhotoPath);
		    Uri contentUri = Uri.fromFile(f);
		    mediaScanIntent.setData(contentUri);
		    this.sendBroadcast(mediaScanIntent);
	}

	private void dispatchTakePictureIntent(int actionCode) {

		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		switch(actionCode) {
		case ACTION_TAKE_PHOTO_B:
			hasCameraOKed = false;
			hasCameraCanceled = false;
			File f = null;
			try {
				f = createImageFile();
				//f = setUpPhotoFile();
				mCurrentPhotoPath = f.getAbsolutePath();
				Log.d("PhotoIntentActivity", "photo path is "+mCurrentPhotoPath);
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
			} catch (IOException e) {
				e.printStackTrace();
				f = null;
				mCurrentPhotoPath = null;
			}
			break;

		default:
			break;			
		} // switch

		startActivityForResult(takePictureIntent, actionCode);
		hasStartedActivityTakePictureIntent = true;
	}

	private void dispatchTakeVideoIntent() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);
	}

	private void handleSmallCameraPhoto(Intent intent) {
		Bundle extras = intent.getExtras();
		mImageBitmap = (Bitmap) extras.get("data");
		mImageView.setImageBitmap(mImageBitmap);
		mVideoUri = null;
		mImageView.setVisibility(View.VISIBLE);
//		mVideoView.setVisibility(View.INVISIBLE);
	}

	private void handleBigCameraPhoto() {
		Log.d(LOG_TAG, "** in handleBigPhoto, mCurrentPhotoPath is "+mCurrentPhotoPath);
		if (mCurrentPhotoPath != null) {
			setPicFromExifThumbnail();
			//setPic();
			galleryAddPic();
			
			// check if logged in to to upload
			if (accessToken == null) {
				Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show();
	        	// login done in onResume()
				//  after login, upload is done in webview.setWebViewClient code
			} else {
				// upload to save photo in CTCT Library
				new UploadImageAsyncTask().execute(mCurrentPhotoPath);
				mCurrentPhotoPath = null;
			}
		}

	}

	private void handleCameraVideo(Intent intent) {
		mVideoUri = intent.getData();
//		mVideoView.setVideoURI(mVideoUri);
		mImageBitmap = null;
//		mVideoView.setVisibility(View.VISIBLE);
		mImageView.setVisibility(View.INVISIBLE);
	}

	Button.OnClickListener mTakePicOnClickListener = 
		new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
		}
	};

	Button.OnClickListener mTakePicSOnClickListener = 
		new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_S);
		}
	};

	Button.OnClickListener mTakeVidOnClickListener = 
		new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			dispatchTakeVideoIntent();
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mImageView = (ImageView) findViewById(R.id.imageView1);
		webview = (WebView) findViewById(R.id.webview);
//		mVideoView = (VideoView) findViewById(R.id.videoView1);
		mImageBitmap = null;
		
		// check whether access token already saved
		final String keyAccessTokey = getString(R.string.key_shpref_access_token);
        accessToken = getPreferences(Context.MODE_PRIVATE).getString(keyAccessTokey, null);
        if (accessToken == null) {
            // set up webview for OAuth2 login
            webview.setWebViewClient(new WebViewClient() {
            	@Override
            	public boolean shouldOverrideUrlLoading(WebView view, String url) {
            		//Log.d(TAG, "** in shouldOverrideUrlLoading(), url is: " + url);
            		if ( url.startsWith(REDIRECT_URI) ) {
            			
            			// extract OAuth2 access_token appended in url
            			if ( url.indexOf("access_token=") != -1 ) {
            				accessToken = mExtractToken(url);

            				// store in default SharedPreferences
            				Editor e = getPreferences(Context.MODE_PRIVATE).edit();
            				e.putString(keyAccessTokey, accessToken);
            				e.commit();
            				
            				// login successful
            				Toast.makeText(getApplicationContext(), "Login successful, uploading picture", Toast.LENGTH_LONG).show();
            				mImageView.setVisibility(View.VISIBLE);
            	            webview.setVisibility(View.GONE);
            				
            	            // continue with what would have been done in handleBigCameraPhoto() and onResume()
            	            //  upload to save photo in CTCT Library
            				new UploadImageAsyncTask().execute(mCurrentPhotoPath);
            				mCurrentPhotoPath = null;
            				//  return to Camera app
            				dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
            			}

            			// don't go to redirectUri
            			return true;
            		}

            		// load the webpage from url (login and grant access)
            		Toast.makeText(getApplicationContext(), "Loading page...", Toast.LENGTH_SHORT).show();
            		return super.shouldOverrideUrlLoading(view, url); // return false; 
            	}
            });
        }
		
//		mVideoUri = null;
		/*
		Button picBtn = (Button) findViewById(R.id.btnIntend);
		setBtnListenerOrDisable( 
				picBtn, 
				mTakePicOnClickListener,
				MediaStore.ACTION_IMAGE_CAPTURE
		);

		Button picSBtn = (Button) findViewById(R.id.btnIntendS);
		setBtnListenerOrDisable( 
				picSBtn, 
				mTakePicSOnClickListener,
				MediaStore.ACTION_IMAGE_CAPTURE
		);

		Button vidBtn = (Button) findViewById(R.id.btnIntendV);
		setBtnListenerOrDisable( 
				vidBtn, 
				mTakeVidOnClickListener,
				MediaStore.ACTION_VIDEO_CAPTURE
		);
		*/
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}
		
		Log.d(LOG_TAG, "** savedInstanceState value in onCreate(), hasCameraCanceled "+
				((savedInstanceState!=null) ? String.valueOf(savedInstanceState.getBoolean("hascameracanceled")) : null)
		);
		Log.d(LOG_TAG, "** exiting onCreate(), hasCameraCanceled "+hasCameraCanceled);
	}
        
	private String mExtractToken(String url) {
		// url has format https://localhost/#access_token=<tokenstring>&token_type=Bearer&expires_in=315359999
		String[] sArray = url.split("access_token=");
		return (sArray[1].split("&token_type=Bearer"))[0];
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		if (accessToken==null && hasCameraOKed) {
			// need to get access token with OAuth2.0
            mImageView.setVisibility(View.GONE);
            webview.setVisibility(View.VISIBLE);
            // do OAuth2 login
            String authorizationUri = mReturnAuthorizationRequestUri();
            webview.loadUrl(authorizationUri);
		} else if (!hasCameraCanceled && !hasStartedActivityTakePictureIntent)
				dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
		Log.d(LOG_TAG, "** exiting onResume(), hasCameraCanceled "+hasCameraCanceled);
	}
	
	private String mReturnAuthorizationRequestUri() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(AUTHORIZE_PATH);
    	sb.append("?response_type=token");
    	sb.append("&client_id="+CLIENT_ID);
    	sb.append("&redirect_uri="+REDIRECT_URI);
    	return sb.toString();
    }
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater(); // using ActionBarSherlock
		inflater.inflate(R.menu.activity, menu);
		// check if can handle intent
		if (!isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)) {
			MenuItem camera = menu.findItem(R.id.menu_camera);
			camera.setEnabled(false).setTitle("Snap! No app found to handle "+getText(R.id.menu_camera)); //TODO
		}
		return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_camera:
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_TAKE_PHOTO_B: {
			hasStartedActivityTakePictureIntent = false;
			if (resultCode == RESULT_OK) {
				hasCameraOKed = true;
				handleBigCameraPhoto();
			}
			if (resultCode == RESULT_CANCELED) {
				hasCameraCanceled = true;
				new File(mCurrentPhotoPath).delete();
			}
			break;
		} // ACTION_TAKE_PHOTO_B

		case ACTION_TAKE_PHOTO_S: {
			if (resultCode == RESULT_OK) {
				handleSmallCameraPhoto(data);
			}
			break;
		} // ACTION_TAKE_PHOTO_S

		case ACTION_TAKE_VIDEO: {
			if (resultCode == RESULT_OK) {
				handleCameraVideo(data);
			}
			break;
		} // ACTION_TAKE_VIDEO
		} // switch
	}

	// Some lifecycle callbacks so that the image can survive orientation change
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
		outState.putParcelable(VIDEO_STORAGE_KEY, mVideoUri);
		outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null) );
		outState.putBoolean(VIDEOVIEW_VISIBILITY_STORAGE_KEY, (mVideoUri != null) );
		outState.putString(CURRENT_PHOTO_PATH_KEY, mCurrentPhotoPath);
		outState.putString(TIMESTAMP_KEY, timeStamp);
		outState.putBoolean("hascameracanceled", hasCameraCanceled); //TODO
		outState.putBoolean("hasStartedActivityTakePictureIntent", hasStartedActivityTakePictureIntent);
		outState.putBoolean("hascameraoked", hasCameraOKed);
		outState.putString("accesstoken", accessToken);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
		mVideoUri = savedInstanceState.getParcelable(VIDEO_STORAGE_KEY);
		mImageView.setImageBitmap(mImageBitmap);
		mImageView.setVisibility(
				savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ? 
						ImageView.VISIBLE : ImageView.INVISIBLE
		);
//		mVideoView.setVideoURI(mVideoUri);
//		mVideoView.setVisibility(
//				savedInstanceState.getBoolean(VIDEOVIEW_VISIBILITY_STORAGE_KEY) ? 
//						ImageView.VISIBLE : ImageView.INVISIBLE
//		);
		mCurrentPhotoPath = savedInstanceState.getString(CURRENT_PHOTO_PATH_KEY);
		timeStamp = savedInstanceState.getString(TIMESTAMP_KEY);
		hasCameraCanceled = savedInstanceState.getBoolean("hascameracanceled"); //TODO
		hasStartedActivityTakePictureIntent = savedInstanceState.getBoolean("hasStartedActivityTakePictureIntent");
		hasCameraOKed = savedInstanceState.getBoolean("hascameraoked");
		accessToken = savedInstanceState.getString("accesstoken");
		Log.d(LOG_TAG, "** exiting onRestoreInstanceState(), savedInstanceState hasCameraCanceled "+hasCameraCanceled);
	}

	/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 *
	 * @param context The application's environment.
	 * @param action The Intent action to check for availability.
	 *
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
			packageManager.queryIntentActivities(intent,
					PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	private void setBtnListenerOrDisable( 
			Button btn, 
			Button.OnClickListener onClickListener,
			String intentName
	) {
		if (isIntentAvailable(this, intentName)) {
			btn.setOnClickListener(onClickListener);        	
		} else {
			btn.setText( 
				getText(R.string.cannot).toString() + " " + btn.getText());
			btn.setClickable(false);
		}
	}

}