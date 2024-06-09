package com.sangdev.miniproject;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {

    public static String copyRawFileToInternalStorage(Context context, int resourceId, String filename) throws IOException {
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        File outputFile = new File(context.getFilesDir(), filename);
        FileOutputStream outputStream = new FileOutputStream(outputFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        outputStream.close();
        inputStream.close();

        return outputFile.getAbsolutePath();
    }
}
