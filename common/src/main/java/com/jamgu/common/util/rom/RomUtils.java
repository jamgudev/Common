package com.jamgu.common.util.rom;

import android.os.Build;
import android.view.WindowManager;
import com.jamgu.common.util.log.JLog;
import java.lang.reflect.Field;

/**
 * 机型检查工具
 */
public class RomUtils {

    private static final String TAG = "RomUtils";

    private static Boolean sIsMiui;

    public static boolean isMIUI() {
        try {
            if (sIsMiui == null) {
                Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
                Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                sIsMiui = field != null;
            }
        } catch (Throwable e) {
        }
        return sIsMiui != null ? sIsMiui : false;
    }

    public static boolean isHUAWEI() {

        // 通过判断是否是 eumi rom更加严谨
        String manufacturer = Build.MANUFACTURER;
        if ("huawei".equalsIgnoreCase(manufacturer)) {
            return true;
        }
        return false;
    }

    private static Boolean sIsFlyMe;

    public static boolean isFlyme() {
        try {
            if (sIsFlyMe == null) {
                Field darkFlag = WindowManager.LayoutParams.class
                        .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
                Field meizuFlags = WindowManager.LayoutParams.class
                        .getDeclaredField("meizuFlags");
                sIsFlyMe = darkFlag != null && meizuFlags != null;
            }
        } catch (Throwable e) {
            JLog.w(TAG, "", e);
        }
        return sIsFlyMe != null ? sIsFlyMe : false;
    }

    private static Boolean sIsOppo;
    public static boolean isOppo() {
        if (sIsOppo == null) {
            String board = Build.BOARD;
            sIsOppo = board != null && Build.BOARD.toLowerCase().contains("oppo");
        }

        return sIsOppo != null ? sIsOppo : false;
    }


}
