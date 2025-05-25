package com.shatteredpixel.shatteredpixeldungeon.utils;
import com.watabou.noosa.Game;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class MobLogger {

    private static final String LOG_FILE = "mob_behavior.log";
    private static BufferedWriter writer;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    static {
        try {
            //File logFile = new File(Game.instance.getFilesDir(), LOG_FILE);
            FileHandle fileHandle = Gdx.files.local(LOG_FILE);
            File logFile = fileHandle.file();
            writer = new BufferedWriter(new FileWriter(logFile, true));
            writer.write("\n=== New Session Started at " + dateFormat.format(new Date()) + " ===\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void log(String message) {
        if (writer != null) {
            try {
                writer.write("[" + dateFormat.format(new Date()) + "] " + message + "\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
