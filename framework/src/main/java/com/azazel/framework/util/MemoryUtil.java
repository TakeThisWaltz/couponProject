package com.azazel.framework.util;

import android.content.Context;

import java.io.File;

/**
 * Created by ji on 2016. 11. 8..
 */

public class MemoryUtil {
    public static long getTotalInternalMemory(Context context) {
        long totalSpace = new File(context.getFilesDir().getAbsoluteFile().toString()).getTotalSpace();

        return totalSpace;
    }

    public static long getTotalUnusedMemory(Context context) {
        long freeBytesInternal = new File(context.getFilesDir().getAbsoluteFile().toString()).getFreeSpace();

        return freeBytesInternal;

    }

    public static float getPercentageMemoryFree(Context context) {
        long total = getTotalInternalMemory(context);
        long avail = getTotalUnusedMemory(context);
        float percentage = ((float) avail / (float) total);
        return percentage;
    }
}
