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
import android.database.Cursor;
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
import com.ctctlabs.ctctwsjavalib.MutableModelObject;

/**
 * This is main activity of app.
 *  It normally starts the Camera app,
 *  then uploads pictures taken to CTCT MyLibrary Plus.
 *  It can be called by another app to upload picture with Intent ACTION_SEND
 * @author ckim
 *
 */
public class UploadMlpActivity extends SherlockActivity {

	private static final int ACTION_TAKE_PHOTO_B		= 10;
	private static final int ACTION_PICK_PHOTO_GALLERY	= 20;
	private static final String BITMAP_STORAGE_KEY		= "viewbitmap";
	private static final String CURRENT_PHOTO_PATH_KEY	= "currentphotopath";
    private static final String REDIRECT_URI			= "https://uploadctctdomain";
    private static final String AUTHORIZE_PATH			= "https://oauth2.constantcontact.com/oauth2/oauth/siteowner/authorize"; 
    private static final String CLIENT_ID				= "8fc5424e-d919-4739-823f-f78a465b61d1";
	private static final String JPEG_FILE_PREFIX		= "IMG_";
	private static final String JPEG_FILE_SUFFIX		= ".jpg";
	private static final String LOG_TAG					= UploadMlpActivity.class.getSimpleName();
	
	private final BitmapFactory.Options bmOptions		= new BitmapFactory.Options();
	
	private ImageView				mImageView;
	private WebView					mWebview;
	private Bitmap					mImageBitmap;
	private String					mAccessToken;
	private AlbumStorageDirFactory	mAlbumStorageDirFactory;
	private String					mCurrentPhotoPath;
	private CTCTConnection			conn;    
    private HashMap<String, Object>	attributes;
    private String					userName;
	
	private Boolean hasCameraCanceled					= false;
	private Boolean hasCameraOKed						= false;
	private Boolean hasStartedActivityTakePictureIntent	= false;
	private Boolean hasBeenStartedBySendIntent			= false;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mImageView = (ImageView) findViewById(R.id.imageview);
		mWebview = (WebView) findViewById(R.id.webview);
		
		// check whether access token already saved
		final String keyAccessToken = getString(R.string.key_shpref_access_token);
        mAccessToken = getPreferences(Context.MODE_PRIVATE).getString(keyAccessToken, null);
        if (mAccessToken == null) {
            
        	// set up mWebview for OAuth2 login
            mWebview.setWebViewClient(new WebViewClient() {
            	@Override
            	public boolean shouldOverrideUrlLoading(WebView view, String url) {
            		if ( url.startsWith(REDIRECT_URI) ) {
            			
            			// extract OAuth2 access_token appended in url
            			if ( url.indexOf("access_token=") != -1 ) {
            				mAccessToken = mExtractToken(url);

            				// store in default SharedPreferences
            				Editor e = getPreferences(Context.MODE_PRIVATE).edit();
            				e.putString(keyAccessToken, mAccessToken);
            				e.commit();
            				
            				// login successful
            				Toast.makeText(getApplicationContext(), "Login successful, uploading picture", Toast.LENGTH_LONG).show();
            				mImageView.setVisibility(View.VISIBLE);
            	            mWebview.setVisibility(View.GONE);
            				
            	            // continue with where left off before having to login
            	            
            	            //  continue with what would have been done in handlePicture(): upload to save image in CTCT library 
            	            new UploadImageAsyncTask().execute(mCurrentPhotoPath);
            				mCurrentPhotoPath = null;
            				//  continue with what would have been done in onResume(): return to Camera app
            	            if (!hasCameraCanceled && !hasStartedActivityTakePictureIntent && !hasBeenStartedBySendIntent) {
            	            	dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
            	            }
            			}

            			// don't go to redirectUri; it is a dummy uri anyway
            			return true;
            		}
            		

            		// load the webpage from url (login and grant access)
            		Toast.makeText(getApplicationContext(), "Loading page...", Toast.LENGTH_SHORT).show();
            		return super.shouldOverrideUrlLoading(view, url); // return false; 
            	}
            });
        }
		
        
        // set picture file base directory path
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			// directory is /Pictures/ in Froyo
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			// directory is /DCIM/
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}
		
		
		// store default values for Preference settings
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		
		// check whether this is ACTION_SEND intent called from the Share menu in Gallery app
		Intent intent = getIntent();
		String type = intent.getType();
		if (Intent.ACTION_SEND.equals(intent.getAction()) && type!=null && type.startsWith("image/")) {
			Bundle extras = intent.getExtras();
			if (extras!= null && extras.containsKey(Intent.EXTRA_STREAM)) {
				// Get file path
				Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
				String filename = mParseUriToFilepath(uri);
				
				if (filename != null) {
					mCurrentPhotoPath = filename;
					
					hasBeenStartedBySendIntent = true;				// hasBeenStartedBySendIntent = true
					
					handleActionSendIntentPicture();
				}
			}
			// remove data so activity will not keep processing intent
			intent.removeExtra(Intent.EXTRA_STREAM);
		} 
	}
    
	/**
	 * Helper to extract access token appended in url string
	 * @param url is the string url
	 * @return the access token
	 */
	private String mExtractToken(String url) {
		// url has format https://localhost/#access_token=<tokenstring>&token_type=Bearer&expires_in=315359999
		String[] sArray = url.split("access_token=");
		return (sArray[1].split("&token_type=Bearer"))[0];
	}
	
	/**
	 * Helper to extract file path from uri
	 * @param uri is the uri for the picture
	 * @return file path for the picture
	 */
	private String mParseUriToFilepath(Uri uri) {
		// reference -- Android: Add your application to the "Share" menu
		//  http://twigstechtips.blogspot.com/2011/10/android-sharing-images-or-files-through.html
		
		// if uri is in ACTION_SEND intent from Gallery app's Share menu, uri is 'content://media/external/images/media/1'
		String[] projection = { MediaStore.Images.Media.DATA }; // value is '_data'
		Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
		if (cursor != null) {
			// could be null if uri is not from Gallery app
			//  e.g. if you used OI file manager for picking the media
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			String selectedImagePath = cursor.getString(column_index);
			cursor.close();
			if (selectedImagePath != null) {
				return selectedImagePath;
			}
		}
		// if uri is not from Gallery app, e.g. from OI file manager
		String filemanagerPath = uri.getPath();
		if (filemanagerPath != null) {
			return filemanagerPath;
		}

		return null;
	}
	
	/** Life cycle method. */
	@Override
	protected void onResume() {
		super.onResume();
		// check whether Cancel out of Camera app before any picture taken
		if (mImageBitmap == null) mImageView.setVisibility(View.INVISIBLE);
		
		// should start Camera app?
		// check whether need to login
		if ( (mAccessToken==null && hasCameraOKed) || (mAccessToken==null && hasBeenStartedBySendIntent) ) {
			// need to get access token with OAuth2.0
            mImageView.setVisibility(View.GONE);
            mWebview.setVisibility(View.VISIBLE);
            // do OAuth2 login, logic is set up in mWebview.setWebViewClient()
            String authorizationUri = mReturnAuthorizationRequestUri();
            mWebview.loadUrl(authorizationUri);
		} else if (!hasCameraCanceled && !hasStartedActivityTakePictureIntent && !hasBeenStartedBySendIntent) {
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
		}
	}
	
	/**
	 * Helper to form url to do OAuth2 authentication by CTCT WebServices
	 * @return the url to do OAuth2 authentication
	 */
	private String mReturnAuthorizationRequestUri() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(AUTHORIZE_PATH);
    	sb.append("?response_type=token");
    	sb.append("&client_id="+CLIENT_ID);
    	sb.append("&redirect_uri="+REDIRECT_URI);
    	return sb.toString();
    }
	
	
	/**
	 * Helper to start activity in the Camera app
	 * @param actionCode is used to identify the result from the Camera app activity
	 */
	private void dispatchTakePictureIntent(int actionCode) {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		switch(actionCode) {
		case ACTION_TAKE_PHOTO_B:
			hasCameraOKed = false;					// hasCameraOKed = false
			hasCameraCanceled = false;					// hasCameraCanceled = false
			File f = null;
			try {
				f = createImageFile();
				mCurrentPhotoPath = f.getAbsolutePath();
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
			} catch (IOException e) {
				e.printStackTrace();
				f = null;
				mCurrentPhotoPath = null;
			}
			break;
		default:
			break;			
		}

		startActivityForResult(takePictureIntent, actionCode);
		
		hasStartedActivityTakePictureIntent = true;			// hasStartedActivityTakePictureIntent = true
	}

	
	/**
	 * Helper to create a picture file to store picture bitmap image
	 * @return the file created
	 * @throws IOException
	 */
	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		File albumF = getAlbumDir();
		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
		return imageF;
	}
	
	/**
	 * Helper to make directory for the picture file
	 * @return a directory (file) object
	 */
	private File getAlbumDir() {
		File storageDir = null;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());
			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()){
						Log.w(LOG_TAG, "Failed to create directory: "+ storageDir.getAbsolutePath());
						return null;
					}
				}
			}
		} else {
			Log.w(LOG_TAG, getString(R.string.app_name)+": External storage is not mounted READ/WRITE.");
		}
		
		return storageDir;
	}
	
	/**
	 * Helper to set the directory subfolder
	 * @return the subfolder name
	 */
	private String getAlbumName() {
		return getString(R.string.album_name);
	}
	
	
	/**
	 * Callback to return result from the Camera app activity started by app
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_TAKE_PHOTO_B: {
			hasStartedActivityTakePictureIntent = false;	// hasStartedActivityTakePictureIntent = false
			
			if (resultCode == RESULT_OK) {
				hasCameraOKed = true;				// hasCameraOKed = true
				// mCurrentPhotoPath was already set before starting Camera app activity, so just handle photo
				handleCameraPhoto();
			}
			if (resultCode == RESULT_CANCELED) {
				hasCameraCanceled = true;				// hasCameraCanceled = true
				// mCurrentPhotoPath was already set before starting Camera app activity, so delete that temp file
				new File(mCurrentPhotoPath).delete();
			}
			break;
		} // ACTION_TAKE_PHOTO_B
		case ACTION_PICK_PHOTO_GALLERY: {
			if (resultCode == RESULT_OK){  
	            Uri selectedImageUri = data.getData();
	            mCurrentPhotoPath = mParseUriToFilepath(selectedImageUri);
	            handlePicture();
	        }
		} // ACTION_PICK_PHOTO_GALLERY
		} // switch
	}

	
	/**
	 * Display picture in the Gallery app,
	 *  and call routine to display picture thumbnail in app, and to do upload
	 */
	private void handleCameraPhoto() {
		if (mCurrentPhotoPath != null) {
			// add picture to be displayed by Gallery app
			galleryAddPic();
		}
		handlePicture();
	}
	
	/**
	 * Call routine to display picture thumbnail in app, and to do upload
	 */
	private void handleActionSendIntentPicture() {
		handlePicture();
	}
	
	/**
	 * Routine to display picture thumbnail in app
	 *  and start background thread to do upload if already authenticated
	 */
	private void handlePicture() {
		if (mCurrentPhotoPath != null) {
			// first, display picture thumbnail in app
			setPicFromExifThumbnail();

			// next, check if logged in so as to upload
			if (mAccessToken == null) {
				Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show();
				// login done in onResume()
				//  after login, upload is done in mWebview.setWebViewClient code
			} else {
				// upload to save photo in CTCT Library
				new UploadImageAsyncTask().execute(mCurrentPhotoPath);
				mCurrentPhotoPath = null;
			}
		}
	}
	
	/**
	 * Helper to display picture in the Gallery app
	 */
	private void galleryAddPic() {
		    Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
			File f = new File(mCurrentPhotoPath);
		    Uri contentUri = Uri.fromFile(f);
		    mediaScanIntent.setData(contentUri);
		    this.sendBroadcast(mediaScanIntent);
	}

	/**
	 * Helper to display picture thumbnail in app
	 */
	private void setPicFromExifThumbnail() {
		try {
			ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
			if (exif.hasThumbnail()) { //TODO a picture cropped within Gallery app seems does not have exif so will not display
				byte[] data = exif.getThumbnail();
				mImageBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
				// rotate if portrait
				if (ExifInterface.ORIENTATION_ROTATE_90 == exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
					Matrix matrix = new Matrix();
					matrix.preRotate(90f);
					mImageBitmap = Bitmap.createBitmap(mImageBitmap, 0, 0, mImageBitmap.getWidth(), mImageBitmap.getHeight(), matrix, true);
				}
			}
			// associate the bitmap to the imageView
			mImageView.setImageBitmap(mImageBitmap);
			mImageView.setVisibility(View.VISIBLE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Inner class that extends AyncTask class to do upload in a background thread
	 * @author ckim
	 *
	 */
	private class UploadImageAsyncTask extends AsyncTask<String, Void, String> {
		
		@Override
		protected String doInBackground(String... paths) {
			String picturePathSdCard = paths[0];
			
			// get the size of the image
			bmOptions.inJustDecodeBounds = true;
			bmOptions.inSampleSize = 1;
			BitmapFactory.decodeFile(picturePathSdCard, bmOptions);
			if( bmOptions.outWidth==-1 || bmOptions.outHeight==-1 ) {
				return null; // there is an error
			}
			
			// set BitmapFactory.Options object to be used by decodeFile()
			int scaleFactorUpload = calculateInSampleSize(bmOptions, 800, 600);
//			Log.d(LOG_TAG, "** picture outWidth, outHeight: "+bmOptions.outWidth+", "+bmOptions.outHeight);
//			Log.d(LOG_TAG, "** inSampleSize is "+scaleFactorUpload);
			bmOptions.inJustDecodeBounds = false;
			bmOptions.inSampleSize = scaleFactorUpload;
			bmOptions.inPurgeable = true;
			bmOptions.inInputShareable = true;
			
			// get bitmap from image file
			File file = new File(picturePathSdCard);
			Bitmap bm = BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);
			
			String imageUrl;
			try {
				// rotate picture if portrait
				ExifInterface exif = new ExifInterface(picturePathSdCard);
				if (ExifInterface.ORIENTATION_ROTATE_90 == exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
					Matrix matrix = new Matrix();
					matrix.postRotate(90f);
					bm = Bitmap.createBitmap(bm, 0, 0, bmOptions.outWidth, bmOptions.outHeight, matrix, true);
				}

				// get byte array of picture image
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				bm.compress(CompressFormat.JPEG, 100, bos);
				byte[] data = bos.toByteArray();

				// some setup needed by java wrapper library to upload image
				
				conn = new CTCTConnection();
				userName = conn.authenticateOAuth2(mAccessToken); //TODO if userName has been cached try skipping this
				if (userName == null) return null; // authentication failed
//				Log.d(LOG_TAG, "** authenticated with "+userName);

				// set values for attributes map used by ctctconnection object
				attributes = new HashMap<String, Object>();
				// retrieve settings values from default SharedPreferences
				SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				String folderId = shPref.getString(getString(R.string.pref_key_folderid), "2"); //default is "2";
				String fileName = shPref.getString(getString(R.string.pref_key_filename), "_UploadMLP")
						+ new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg";
				String description = shPref.getString(getString(R.string.pref_key_filedesc), "UploadMLP picture");
				
				// call the java wrapper library methods
				Image imageModelObj = conn.createImage(attributes, folderId, fileName, data, description);
				imageModelObj.commit();
				if (conn.getResponseStatusCode() == HttpStatus.SC_CREATED) {
					imageUrl = (String)imageModelObj.getAttribute("ImageURL");
				} else {
					imageUrl = null;
				}
				
			} catch (SocketException e) {
				return e.getClass().getSimpleName() + ": " + e.getMessage();	// could be wifi not available
			} catch (UnknownHostException e) {
				return e.getClass().getSimpleName() + ": " + e.getMessage();	// could be wifi not available
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
			if (result == null) { //TODO try detect other errors: image too large?, not MLP and exceeded 5 images
				int connStatusCode = conn.getResponseStatusCode();
				String connStatusReason = conn.getResponseStatusReason();
//				Log.d(LOG_TAG, "** Error - Picture imageUrl null; connection status "+connStatusCode + " " + connStatusReason);
				if (connStatusCode == HttpStatus.SC_BAD_REQUEST) { 
					message = "Snap! Not uploaded (Bad Request; possible bad folder id setting)";
				} else if (connStatusCode == 0) {
					message = "Snap! Please try again (Sorry, reason unknown)";
				} else {
					message = "Snap! Not uploaded (" + connStatusReason + ")";
				}
			}
			else if ( result.contains(UnknownHostException.class.getSimpleName())
					  || result.contains(SocketException.class.getSimpleName()) ) {
//				Log.d(LOG_TAG, "** Error - "+result);
				message = "Please check your Internet connection "
						+ "(" + result.split(": ")[1] + ")"; // reason is after separator; set in catch clause of doInBackground() above
			}
			else if (result != null) {
//				Log.d(LOG_TAG, "**Image Url is "+result);
				message = "Uploaded to CTCT MyLibrary Plus";
			}
			
			
			// Show toast message
			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
			conn = null;
			super.onPostExecute(result);
			
			
			// Stop activity if started by ACTION_SEND intent called from the Share menu in Gallery app
			if (hasBeenStartedBySendIntent) {
				hasBeenStartedBySendIntent = false;					// hasBeenStartedBySendIntent = false
				finish();
			}
		}
		
	}
	
	/**
	 * Helper to calculate value for the BitmapFactory.Options object's inSampleSize
	 * @param options is the BitmapFactory.Options object
	 * @param reqWidth is the requested width
	 * @param reqHeight is the requested height
	 * @return the inSampleSize value
	 */
	private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		
		if (reqWidth==0 || reqHeight==0) return inSampleSize; // probably invalid input, so just return 1
		
		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float)height / (float)reqHeight);
			} else {
				inSampleSize = Math.round((float)width / (float)reqWidth);
			}
		}
		return inSampleSize;
	}
	
	
	/** Callback to create the action bar menu items */
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
	
	
	/**
	 * Callback when an actionbar menu is selected
	 */
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
			// reference -- http://stackoverflow.com/questions/2507898/how-to-pick-an-image-from-gallery-sd-card-for-my-app-in-android
			Intent gIntent = new Intent(Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(gIntent, ACTION_PICK_PHOTO_GALLERY);
			/* view picture images in Gallery app
			// reference -- http://stackoverflow.com/questions/6016000/how-to-open-phones-gallery-through-code
			Intent gIntent = new Intent(Intent.ACTION_VIEW, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			// start uploadmlp app's own activity even after back key > to Gallery app > long-press Home > app icon in recent apps
			gIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET only clears Gallery activity from task stack
															 //  so won't be in Gallery app next time UploadMLP is launched with app icon
			startActivity(gIntent);*/
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	/** Some lifecycle callbacks so that the image can survive orientation change */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
		outState.putString(CURRENT_PHOTO_PATH_KEY, mCurrentPhotoPath);
		outState.putBoolean("hascameracanceled", hasCameraCanceled); //TODO put in R.string
		outState.putBoolean("hasStartedActivityTakePictureIntent", hasStartedActivityTakePictureIntent);
		outState.putBoolean("hascameraoked", hasCameraOKed);
		outState.putString("accesstoken", mAccessToken);
		outState.putString("username", userName);
		outState.putBoolean("hasBeenStartedBySendIntent", hasBeenStartedBySendIntent);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
		mImageView.setImageBitmap(mImageBitmap);
		mCurrentPhotoPath = savedInstanceState.getString(CURRENT_PHOTO_PATH_KEY);
		hasCameraCanceled = savedInstanceState.getBoolean("hascameracanceled"); //TODO use R.string
		hasStartedActivityTakePictureIntent = savedInstanceState.getBoolean("hasStartedActivityTakePictureIntent");
		hasCameraOKed = savedInstanceState.getBoolean("hascameraoked");
		mAccessToken = savedInstanceState.getString("accesstoken");
		userName = savedInstanceState.getString("username");
		hasBeenStartedBySendIntent = savedInstanceState.getBoolean("hasBeenStartedBySendIntent");
	}

}