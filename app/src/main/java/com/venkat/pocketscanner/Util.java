package com.venkat.pocketscanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

public class Util {

    /**
     * check directory whether exist, if not then make one;
     *
     * @param path absolute path of directory
     */
    public static boolean checkDir(String path) {
        boolean result = true;
        File file = new File(path);
        if (!file.exists()) {
            result = file.mkdirs();
        } else if (file.isFile()) {
            file.delete();
            result = file.mkdirs();
        }
        return result;
    }

    public static Bitmap loadBitmap(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = null;
        try {
            options.inSampleSize = 1;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            if (options.mCancel || options.outWidth == -1
                    || options.outHeight == -1) {
                return null;
            }
            options.inSampleSize = 2;
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            bitmap = BitmapFactory.decodeFile(path, options);
            if (bitmap == null)
                return null;

            int orientation = getOrientation(path);
            if (orientation != 1) {
                Matrix m = new Matrix();
                m.postRotate(getRotation(orientation));
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, false);
            }

        } catch (OutOfMemoryError ex) {
            ex.printStackTrace();
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * get rotation degrees
     *
     * @param orientation values in {1, 3, 6, 8}
     * @return values in {0, 90, 180, 270}
     */
    private static int getRotation(int orientation) {
        switch (orientation) {
            case 1:
                return 0;
            case 8:
                return 270;
            case 3:
                return 180;
            case 6:
                return 90;
            default:
                return 0;
        }
    }

    /**
     * read orientation from Image file
     *
     * @param file
     * @return
     */
    private static int getOrientation(String file) {
        int orientation = 1;
        ExifInterface exif;
        if (!TextUtils.isEmpty(file)) {
            try {
                exif = new ExifInterface(file);
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ExceptionInInitializerError e) {
                e.printStackTrace();
            }
        }
        return orientation;
    }
}
