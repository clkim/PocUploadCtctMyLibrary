package com.jinmobi.android.uploadmlp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.ctctlabs.ctctwsjavalib.CTCTConnection;
import com.ctctlabs.ctctwsjavalib.Image;


public class UploadMlpActivity extends SherlockActivity {

	private static final int ACTION_TAKE_PHOTO_B = 1;

	private static final String BITMAP_STORAGE_KEY = "viewbitmap";
	private ImageView mImageView;
	private Bitmap mImageBitmap;
	
	private WebView webview;
	
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
	
	private static final String LOG_TAG			 = UploadMlpActivity.class.getSimpleName();

	private AlbumStorageDirFactory mAlbumStorageDirFactory	= null;
	private String timeStamp;
	private Boolean hasCameraCanceled = false;
	private Boolean hasCameraOKed = false;
	private Boolean hasStartedActivityTakePictureIntent		= false;
	
	
	private class UploadImageAsyncTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... paths) {
			String picturePathSdCard = paths[0];
			
			/* Get the size of the image */
			bmOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(picturePathSdCard, bmOptions);
			if( bmOptions.outWidth==-1 || bmOptions.outHeight==-1 ) return null;
			
			// set BitmapFactory.Options object to be used by decodeFile()
			int scaleFactorUpload = calculateInSampleSize(bmOptions, 200, 150); // these dim seems to create files in MLP mostly 50-70 KB, sometimes 100+ KB
//			Log.d(LOG_TAG, "** picture outWidth, outHeight: "+bmOptions.outWidth+", "+bmOptions.outHeight);
//			Log.d(LOG_TAG, "** inSampleSize is "+scaleFactorUpload);
			bmOptions.inJustDecodeBounds = false;
			bmOptions.inSampleSize = scaleFactorUpload;
			bmOptions.inPurgeable = true;
			
			File file = new File(picturePathSdCard);
			Bitmap bm = BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);
			
			String imageUrl;
			try {
				// rotate picture if portrait
				ExifInterface exif = new ExifInterface(picturePathSdCard);
//				Log.d(LOG_TAG, "** exif orientation tag value is "+exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0));
//				Log.d(LOG_TAG, "** exif image width, length, hasThumbnail: "+exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1) +
//						", "+exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1) +
//						", "+exif.hasThumbnail());
				if (ExifInterface.ORIENTATION_ROTATE_90 == exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
					Matrix matrix = new Matrix();
					matrix.preRotate(90f);
					bm = Bitmap.createBitmap(bm, 0, 0, bmOptions.outWidth, bmOptions.outHeight, matrix, true);
				}

				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				bm.compress(CompressFormat.JPEG, 100, bos);
				byte[] data = bos.toByteArray();

				conn = new CTCTConnection();
				//if (userName == null)
						userName = conn.authenticateOAuth2(accessToken);
				if (userName == null) return null;
				Log.d(LOG_TAG, "** authenticated with "+userName);

				attributes = new HashMap<String, Object>();
				SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				String folderId = shPref.getString(getString(R.string.pref_key_folderid), "2"); //default is "2";
				String fileName = shPref.getString(getString(R.string.pref_key_filename), "_UploadMLP") + timeStamp+".jpg";
				String description = shPref.getString(getString(R.string.pref_key_filedesc), "UploadMLP picture");
				Image imageModelObj = conn.createImage(attributes, folderId, fileName, data, description);
				imageModelObj.commit();
				imageUrl = (String)imageModelObj.getAttribute("ImageURL");
			} catch (SocketException se) {
				return SocketException.class.getSimpleName();
			} catch (UnknownHostException uke) {	
				return UnknownHostException.class.getSimpleName();
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
				attributes = null;
			}
			
			return imageUrl;
		}

		@Override
		protected void onPostExecute(String result) {
			String message = "";
			if (result == null) {
//				Log.d(LOG_TAG, "**Error -- picture not uploaded");
				// TODO try detect different errors? e.g. not MLP and exceeded 5 images, image too large? 
				if (conn.getResponseStatusCode() == HttpStatus.SC_BAD_REQUEST) {
					message = "Snap! Possible bad folder id setting.";
				} else if (conn.getResponseStatusCode() == 0) {
					message = "Snap! Please try again. (Unknown reason.)";
				} else {
					message = "Snap! Not uploaded. ("+conn.getResponseStatusReason()+".)";
				}
			}
			else if ( result.equals(UnknownHostException.class.getSimpleName())
					  || result.equals(SocketException.class.getSimpleName()) ) {
				message = "Please check your Internet connection. ("+result+".)";
			}
			else if (result != null) {
				message = "Uploaded to CTCT MyLibrary Plus";
//				Log.d(LOG_TAG, "** Image Url is "+result);
			}
			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
			conn = null;
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
//						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}
			
		} else {
//			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
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
			mImageView.setVisibility(View.VISIBLE);
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
				mCurrentPhotoPath = f.getAbsolutePath();
//				Log.d("UploadMlpActivity", "photo path is "+mCurrentPhotoPath);
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


	private void handleBigCameraPhoto() {
//		Log.d(LOG_TAG, "** in handleBigPhoto, mCurrentPhotoPath is "+mCurrentPhotoPath);
		if (mCurrentPhotoPath != null) {
			setPicFromExifThumbnail();
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


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mImageView = (ImageView) findViewById(R.id.imageView1);
		webview = (WebView) findViewById(R.id.webview);
		
		mImageBitmap = null;
		
		// check whether access token already saved
		final String keyAccessTokey = getString(R.string.key_shpref_access_token);
        accessToken = getPreferences(Context.MODE_PRIVATE).getString(keyAccessTokey, null);
        if (accessToken == null) {
            // set up webview for OAuth2 login
            webview.setWebViewClient(new WebViewClient() {
            	@Override
            	public boolean shouldOverrideUrlLoading(WebView view, String url) {
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
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
//		Log.d(LOG_TAG, "** savedInstanceState value in onCreate(), hasCameraCanceled "+
//				((savedInstanceState!=null) ? String.valueOf(savedInstanceState.getBoolean("hascameracanceled")) : null)
//		);
//		Log.d(LOG_TAG, "** exiting onCreate(), hasCameraCanceled "+hasCameraCanceled);
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
//		Log.d(LOG_TAG, "** exiting onResume(), hasCameraCanceled "+hasCameraCanceled);
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
			camera.setEnabled(false).setTitle("Snap! No "+getText(R.id.menu_camera));
		}
		return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_camera:
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
			return true;
		case R.id.menu_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		case R.id.menu_gallery:
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("content://media/internal/images/media"));
			startActivity(intent);
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
		} // switch
	}

	// Some lifecycle callbacks so that the image can survive orientation change
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
		outState.putString(CURRENT_PHOTO_PATH_KEY, mCurrentPhotoPath);
		outState.putString(TIMESTAMP_KEY, timeStamp);
		outState.putBoolean("hascameracanceled", hasCameraCanceled); //TODO put in R.string
		outState.putBoolean("hasStartedActivityTakePictureIntent", hasStartedActivityTakePictureIntent);
		outState.putBoolean("hascameraoked", hasCameraOKed);
		outState.putString("accesstoken", accessToken);
		outState.putString("username", userName);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
		mImageView.setImageBitmap(mImageBitmap);
		mCurrentPhotoPath = savedInstanceState.getString(CURRENT_PHOTO_PATH_KEY);
		timeStamp = savedInstanceState.getString(TIMESTAMP_KEY);
		hasCameraCanceled = savedInstanceState.getBoolean("hascameracanceled"); //TODO use R.string
		hasStartedActivityTakePictureIntent = savedInstanceState.getBoolean("hasStartedActivityTakePictureIntent");
		hasCameraOKed = savedInstanceState.getBoolean("hascameraoked");
		accessToken = savedInstanceState.getString("accesstoken");
		userName = savedInstanceState.getString("username");
//		Log.d(LOG_TAG, "** exiting onRestoreInstanceState(), savedInstanceState hasCameraCanceled "+hasCameraCanceled);
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

}