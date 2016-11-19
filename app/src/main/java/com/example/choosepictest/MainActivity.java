package com.example.choosepictest;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends Activity {

	public static final int TAKE_PHOTO = 1;

	public static final int CROP_PHOTO = 2;

	public static final int CHOOSE_PHOTO = 3;

	private Button takePhoto;

	private Button chooseFromAlbum;

	private ImageView picture;

	private Uri imageUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		takePhoto = (Button) findViewById(R.id.take_photo);
		chooseFromAlbum = (Button) findViewById(R.id.choose_from_album);
		picture = (ImageView) findViewById(R.id.picture);
		takePhoto.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				File outputImage = new File(Environment.getExternalStorageDirectory(),
						"output_image.jpg");
				try {
					if (outputImage.exists()) {
						outputImage.delete();
					}
					outputImage.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				imageUri = Uri.fromFile(outputImage);
				Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
				intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
				startActivityForResult(intent, TAKE_PHOTO);
			}
		});
		chooseFromAlbum.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent("android.intent.action.GET_CONTENT");
//				Intent intent = new Intent(Intent.ACTION_PICK);
//				intent.setDataAndType(Intent.ACTION_GET_CONTENT,"image/*");
				intent.setType("image/*");
				startActivityForResult(intent,CHOOSE_PHOTO);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case TAKE_PHOTO:
			if (resultCode == RESULT_OK) {
				Intent intent = new Intent("com.android.camera.action.CROP");
				intent.setDataAndType(imageUri, "image/*");
				intent.putExtra("scale", true);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
				startActivityForResult(intent, CROP_PHOTO);
			}
			break;
		case CROP_PHOTO:
			Log.d("haha","CROP_PHOTO");
			if (resultCode == RESULT_OK) {
				try {
					Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver()
							.openInputStream(imageUri));
					picture.setImageBitmap(bitmap);
					Log.d("haha","设置图片");
				} catch (FileNotFoundException e) {
					Log.d("haha","设置图片异常");
					e.printStackTrace();
				}
			}
			break;

			case CHOOSE_PHOTO:
				Log.d("haha","choose photo");

				if (resultCode == RESULT_OK){
					//判断安卓版本
					if (Build.VERSION.SDK_INT>=19){
						//4.4以上版本
						handleImageOnKitKat(data);
					}else {
						handleImageBeforeKitKat(data);
					}
				}
				break;
		default:
			break;
		}
	}


	private void handleImageOnKitKat(Intent data){
		String imagePath = null;
		Bitmap bitmap = null;
		//Uri uri = data.getData();

		Uri uri = geturi(data);

		if (DocumentsContract.isDocumentUri(this,uri)){
			//document类型的uri
			String docId = DocumentsContract.getDocumentId(uri);
			if ("com.android.providers.media.documents".equals(uri.getAuthority())){
				String id = docId.split(":")[1];
				String selection = MediaStore.Images.Media._ID+"="+id;
				imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
			}else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())){
				Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
				imagePath = getImagePath(contentUri,null);
			}
		}else if ("content".equalsIgnoreCase(uri.getScheme())){
			imagePath = getImagePath(uri,null);
			//displayImages(imagePath);
		}

		if (imagePath == null) {
			//imagePath = getImagePath(geturi(data),null);
		}

		Log.d("haha","path == "+imagePath);

		displayImages(imagePath);
	}



	/**
	 * 解决小米手机上获取图片路径为null的情况
	 * @param intent
	 * @return
	 */
	public Uri geturi(android.content.Intent intent) {
		Uri uri = intent.getData();
		String type = intent.getType();
		if (uri.getScheme().equals("file") && (type.contains("image/"))) {
			String path = uri.getEncodedPath();

			Log.d("haha","uri path ===== "+path);

			if (path != null) {
				path = Uri.decode(path);
				ContentResolver cr = this.getContentResolver();
				StringBuffer buff = new StringBuffer();
				buff.append("(").append(MediaStore.Images.ImageColumns.DATA).append("=")
						.append("'" + path + "'").append(")");
				Cursor cur = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						new String[] { MediaStore.Images.ImageColumns._ID },
						buff.toString(), null, null);
				int index = 0;
				for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
					index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID);
					// set _id value
					index = cur.getInt(index);
				}
				if (index == 0) {
					// do nothing
				} else {
					Uri uri_temp = Uri
							.parse("content://media/external/images/media/"
									+ index);
					if (uri_temp != null) {
						uri = uri_temp;
					}
				}
			}
		}
		return uri;
	}


	private void handleImageBeforeKitKat(Intent data){
		Uri uri = data.getData();
		String imagePath = getImagePath(uri,null);
		displayImages(imagePath);
	}

	private String getImagePath(Uri uri,String selection){
		String path = null;
		Cursor cursor = getContentResolver().query(uri,null,selection,null,null);

		if (cursor != null){
			if (cursor.moveToFirst()){
				path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
			}
			cursor.close();
		}

		return path;
	}

	private void displayImages(String imagePath){

		if (imagePath != null){
		//	Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
			Bitmap bitmap = Tools.decodeSampledBitmapFromFile(imagePath,getScreenWidthAndHeight()[0]/2,getScreenWidthAndHeight()[1]/2);

			if (bitmap.getWidth() <= getScreenWidthAndHeight()[0]/2) {
				picture.setImageBitmap(bitmap);
			}else {
				Bitmap bitmap1 = Bitmap.createScaledBitmap(bitmap,getScreenWidthAndHeight()[0]/2,bitmap.getHeight()*getScreenWidthAndHeight()[0]/2/bitmap.getWidth(),true);
                Log.d("haha","重新调整过后的bitmap的宽高："+bitmap1.getWidth()+"   "+bitmap1.getHeight());
				picture.setImageBitmap(bitmap1);
			}
		}else {
			Toast.makeText(this,"failed to get image",Toast.LENGTH_SHORT).show();
		}
	}





	public int[] getScreenWidthAndHeight(){

		WindowManager windowManager = (WindowManager)this.getWindowManager();

		int width = windowManager.getDefaultDisplay().getWidth();
		int heigt = windowManager.getDefaultDisplay().getHeight();

		return new int[]{width,heigt};

	}

}
