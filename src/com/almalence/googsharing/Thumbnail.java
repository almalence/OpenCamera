/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.almalence.googsharing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import android.util.Log;

//<!-- -+-
import com.almalence.opencam.PluginManager;
//-+- -->
/* <!-- +++
import com.almalence.opencam_plus.PluginManager;
+++ --> */

import com.almalence.util.Util;

public class Thumbnail
{
	private static final String		TAG					= "Thumbnail";

	public static final String		LAST_THUMB_FILENAME	= "last_thumb";
	private static final int		BUFSIZE				= 4096;

	private Uri						mUri;
	private Bitmap					mBitmap;
	private Bitmap					mFullBitmap;
	// whether this thumbnail is read from file
	private boolean					mFromFile			= false;

	private static ContentResolver	mResolver			= null;

	public static final String		DCIM				= Environment.getExternalStoragePublicDirectory(
																Environment.DIRECTORY_DCIM).toString();
	public static final String		DIRECTORY			= DCIM + "/Camera";
	// Match the code in MediaProvider.computeBucketValues().
	public static final String		BUCKET_ID			= String.valueOf(DIRECTORY.toLowerCase().hashCode());

	public Thumbnail(Uri uri, Bitmap bitmap, Bitmap fullBitmap, int orientation)
	{
		mUri = uri;
		mBitmap = rotateImage(bitmap, orientation);
		if (fullBitmap != null)
			mFullBitmap = rotateImage(fullBitmap, orientation);
		if (mBitmap == null)
			throw new IllegalArgumentException("null bitmap");
	}

	public Uri getUri()
	{
		return mUri;
	}

	public Bitmap getBitmap()
	{
		return mBitmap;
	}

	public Bitmap getFullBitmap()
	{
		Media image = getLastImageThumbnail(mResolver);

		if (image == null)
			return null;

		Bitmap fullBitmap = null;

		try
		{
			fullBitmap = Images.Media.getBitmap(mResolver, image.uri);
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		mFullBitmap = null;
		System.gc();

		mUri = image.uri;
		mFullBitmap = rotateImage(fullBitmap, image.orientation);
		if (mFullBitmap == null)
			throw new IllegalArgumentException("null bitmap");

		return mFullBitmap;
	}

	public void setFromFile(boolean fromFile)
	{
		mFromFile = fromFile;
	}

	public boolean fromFile()
	{
		return mFromFile;
	}

	private static Bitmap rotateImage(Bitmap bitmap, int orientation)
	{
		if (orientation != 0)
		{
			// We only rotate the thumbnail once even if we get OOM.
			Matrix m = new Matrix();
			m.setRotate(orientation, bitmap.getWidth() * 0.5f, bitmap.getHeight() * 0.5f);

			try
			{
				Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
				// If the rotated bitmap is the original bitmap, then it
				// should not be recycled.
				if (rotated != bitmap)
					bitmap.recycle();
				return rotated;
			} catch (Exception t)
			{
				Log.w(TAG, "Failed to rotate thumbnail", t);
			}
		}

		return bitmap;
	}

	// Stores the bitmap to the specified file.
	public void saveTo(File file)
	{
		FileOutputStream f = null;
		BufferedOutputStream b = null;
		DataOutputStream d = null;
		try
		{
			f = new FileOutputStream(file);
			b = new BufferedOutputStream(f, BUFSIZE);
			d = new DataOutputStream(b);
			d.writeUTF(mUri.toString());
			mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, d);
			d.close();
		} catch (IOException e)
		{
			Log.e(TAG, "Fail to store bitmap. path=" + file.getPath(), e);
		} finally
		{
			Util.closeSilently(f);
			Util.closeSilently(b);
			Util.closeSilently(d);
		}
	}

	// Loads the data from the specified file.
	// Returns null if failure.
	public static Thumbnail loadFrom(File file)
	{
		Uri uri = null;
		Bitmap bitmap = null;
		FileInputStream f = null;
		BufferedInputStream b = null;
		DataInputStream d = null;
		try
		{
			f = new FileInputStream(file);
			b = new BufferedInputStream(f, BUFSIZE);
			d = new DataInputStream(b);
			uri = Uri.parse(d.readUTF());
			bitmap = BitmapFactory.decodeStream(d);
			d.close();
		} catch (IOException e)
		{
			Log.i(TAG, "Fail to load bitmap. " + e);

			return null;
		} finally
		{
			Util.closeSilently(f);
			Util.closeSilently(b);
			Util.closeSilently(d);
		}

		Thumbnail thumbnail = createThumbnail(uri, bitmap, null, 0);
		if (thumbnail != null)
			thumbnail.setFromFile(true);

		return thumbnail;
	}

	public static Thumbnail getLastThumbnail(ContentResolver resolver)
	{
		mResolver = resolver;
		Media image = getLastImageThumbnail(resolver);
		Media video = getLastVideoThumbnail(resolver);

		if (image == null && video == null)
			return null;

		Bitmap bitmap = null;
		Media lastMedia = null;

		try
		{
			// If there is only image or video, get its thumbnail. If both
			// exist,
			// get the thumbnail of the one that is newer.
			if (image != null && (video == null || image.dateTaken >= video.dateTaken))
			{
				bitmap = Images.Thumbnails.getThumbnail(resolver, image.id, Images.Thumbnails.MICRO_KIND, null);
				lastMedia = image;
			} else if (video != null)
			{
				bitmap = Video.Thumbnails.getThumbnail(resolver, video.id, Video.Thumbnails.MICRO_KIND, null);
				lastMedia = video;
			}
		} catch (Exception ex)
		{
			Log.e("getLastThumbnail", "createThumbnail exception " + ex.getMessage());
			return null;
		}

		try
		{
			return createThumbnail(lastMedia.uri, bitmap, null, lastMedia.orientation);
		} catch (Exception ex)
		{
			Log.e("getLastThumbnail", "createThumbnail exception " + ex.getMessage());
			return null;
		}
	}

	private static class Media
	{
		public Media(long id, int orientation, long dateTaken, Uri uri)
		{
			this.id = id;
			this.orientation = orientation;
			this.dateTaken = dateTaken;
			this.uri = uri;
		}

		public final long	id;
		public final int	orientation;
		public final long	dateTaken;
		public final Uri	uri;
	}

	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int size, int pixels)
	{
		final int side = Math.min(bitmap.getWidth(), bitmap.getHeight());

		final Bitmap bitmapCropped = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - side) / 2,
				(bitmap.getHeight() - side) / 2, side, side);

		final Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(output);

		final int color = 0xffffffff;
		final Paint paint = new Paint();
		final Rect rectSrc = new Rect(0, 0, bitmapCropped.getWidth(), bitmapCropped.getHeight());
		final Rect rect = new Rect(6, 6, output.getWidth() - 6, output.getHeight() - 6);
		final RectF rectF = new RectF(rect);
		final RectF rectFBorder = new RectF(0, 0, output.getWidth(), output.getHeight());
		final float roundPx = pixels;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmapCropped, rectSrc, rect, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.DST_ATOP));
		canvas.drawRoundRect(rectFBorder, roundPx, roundPx, paint);

		return output;
	}

	public static Media getLastImageThumbnail(ContentResolver resolver)
	{
		Media internalMedia = null;
		Media externalMedia = null;

		try
		{
			Uri baseUri = Images.Media.INTERNAL_CONTENT_URI;

			Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
			String[] projection = new String[] { ImageColumns._ID, ImageColumns.ORIENTATION, ImageColumns.DATE_TAKEN };
			String selection = ImageColumns.DATA + " like '" + PluginManager.getSaveDir(false).getAbsolutePath() + "%' AND " + ImageColumns.MIME_TYPE + "='image/jpeg'";
			String order = ImageColumns.DATE_TAKEN + " DESC," + ImageColumns._ID + " DESC";

			Cursor cursor = null;

			try
			{
				cursor = resolver.query(query, projection, selection, null, order);
				if (cursor != null && cursor.moveToFirst())
				{
					final long id = cursor.getLong(0);
					internalMedia = new Media(id, cursor.getInt(1), cursor.getLong(2), ContentUris.withAppendedId(
							baseUri, id));
				}
			} finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}
		} catch (Exception e)
		{

		}

		try
		{
			Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
			Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
			String[] projection = new String[] { ImageColumns._ID, ImageColumns.ORIENTATION, ImageColumns.DATE_TAKEN };
			String selection = ImageColumns.DATA + " like '" + PluginManager.getSaveDir(false).getAbsolutePath() + "%' AND " + ImageColumns.MIME_TYPE + "='image/jpeg'";
			String order = ImageColumns.DATE_TAKEN + " DESC," + ImageColumns._ID + " DESC";

			Cursor cursor = null;

			try
			{
				cursor = resolver.query(query, projection, selection, null, order);
				if (cursor != null && cursor.moveToFirst())
				{
					final long id = cursor.getLong(0);
					externalMedia = new Media(id, cursor.getInt(1), cursor.getLong(2), ContentUris.withAppendedId(
							baseUri, id));
				}
			} finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}
		} catch (Exception e)
		{

		}

		if (internalMedia == null)
		{
			return externalMedia;
		} else if (externalMedia == null)
		{
			return internalMedia;
		} else
		{
			return internalMedia.dateTaken > externalMedia.dateTaken ? internalMedia : externalMedia;
		}
	}

	public static Thumbnail createThumbnail(byte[] jpeg, int orientation, int inSampleSize, Uri uri)
	{
		// Create the thumbnail.
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = inSampleSize;
		Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, options);
		return createThumbnail(uri, bitmap, null, orientation);
	}

	private static Thumbnail createThumbnail(Uri uri, Bitmap bitmap, Bitmap fullBitmap, int orientation)
	{
		if (bitmap == null)
		{
			Log.e(TAG, "Failed to create thumbnail from null bitmap");
			return null;
		}
		try
		{
			return new Thumbnail(uri, bitmap, fullBitmap, orientation);
		} catch (IllegalArgumentException e)
		{
			Log.e(TAG, "Failed to construct thumbnail", e);
			return null;
		}
	}

	private static Media getLastVideoThumbnail(ContentResolver resolver)
	{
		
		Media internalMedia = null;
		Media externalMedia = null;

		try
		{
			Uri baseUri = Video.Media.INTERNAL_CONTENT_URI;

			Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
			String[] projection = new String[] { VideoColumns._ID, VideoColumns.DATA, VideoColumns.DATE_TAKEN };
			String selection = VideoColumns.DATA + " like '" + PluginManager.getSaveDir(false).getAbsolutePath() + "%' AND " + VideoColumns.MIME_TYPE + "='video/mp4'";
			String order = VideoColumns.DATE_TAKEN + " DESC," + VideoColumns._ID + " DESC";

			Cursor cursor = null;

			try
			{
				cursor = resolver.query(query, projection, selection, null, order);
				if (cursor != null && cursor.moveToFirst())
				{
					final long id = cursor.getLong(0);
					internalMedia = new Media(id, 0, cursor.getLong(2), ContentUris.withAppendedId(baseUri, id));
				}
			} finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}
		} catch (Exception e)
		{

		}

		try
		{
			Uri baseUri = Video.Media.EXTERNAL_CONTENT_URI;
			Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
			String[] projection = new String[] { VideoColumns._ID, VideoColumns.DATA, VideoColumns.DATE_TAKEN };
			String selection = VideoColumns.DATA + " like '" + PluginManager.getSaveDir(false).getAbsolutePath() + "%' AND " + VideoColumns.MIME_TYPE + "='video/mp4'";
			String order = VideoColumns.DATE_TAKEN + " DESC," + VideoColumns._ID + " DESC";

			Cursor cursor = null;

			try
			{
				cursor = resolver.query(query, projection, selection, null, order);
				if (cursor != null && cursor.moveToFirst())
				{
					final long id = cursor.getLong(0);
					externalMedia = new Media(id, 0, cursor.getLong(2), ContentUris.withAppendedId(baseUri, id));
				}
			} finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}
		} catch (Exception e)
		{

		}

		if (internalMedia == null)
		{
			return externalMedia;
		} else if (externalMedia == null)
		{
			return internalMedia;
		} else
		{
			return internalMedia.dateTaken > externalMedia.dateTaken ? internalMedia : externalMedia;
		}
	}

	public static Bitmap createVideoThumbnail(FileDescriptor fd, int targetWidth)
	{
		return createVideoThumbnail(null, fd, targetWidth);
	}

	public static Bitmap createVideoThumbnail(String filePath, int targetWidth)
	{
		return createVideoThumbnail(filePath, null, targetWidth);
	}

	private static Bitmap createVideoThumbnail(String filePath, FileDescriptor fd, int targetWidth)
	{
		Bitmap bitmap = null;
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		try
		{
			if (filePath != null)
			{
				retriever.setDataSource(filePath);
			} else
			{
				retriever.setDataSource(fd);
			}
			bitmap = retriever.getFrameAtTime(-1);
		} catch (IllegalArgumentException ex)
		{
			// Assume this is a corrupt video file
		} catch (RuntimeException ex)
		{
			// Assume this is a corrupt video file.
		} finally
		{
			try
			{
				retriever.release();
			} catch (RuntimeException ex)
			{
				// Ignore failures while cleaning up.
			}
		}
		if (bitmap == null)
			return null;

		// Scale down the bitmap if it is bigger than we need.
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		if (width > targetWidth)
		{
			float scale = (float) targetWidth / width;
			int w = Math.round(scale * width);
			int h = Math.round(scale * height);
			bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
		}
		return bitmap;
	}
}
