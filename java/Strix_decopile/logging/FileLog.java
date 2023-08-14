package Strix_decopile.logging;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileLog {
    protected String timePattern = "HH:mm:ss";
    private SimpleDateFormat timeFormat;
    private Date now;
    protected String fileName;
    protected Writer fw;

    public FileLog(String fileName) throws IOException {
        this.timeFormat = new SimpleDateFormat(this.timePattern);
        this.now = new Date();
        this.setFile(fileName);
    }

    public synchronized void setFile(String fileName) throws IOException {
        this.fileName = fileName;
        this.fw = null;

        try {
            this.fw = new BufferedWriter(new FileWriter(fileName, true));
        } catch (FileNotFoundException var5) {
            String parentName = (new File(fileName)).getParent();
            if (parentName != null) {
                File parentDir = new File(parentName);
                if (!parentDir.exists() && parentDir.mkdirs()) {
                    this.fw = new BufferedWriter(new FileWriter(fileName, true));
                    return;
                }

                throw var5;
            }

            throw var5;
        }

    }

    protected void closeFile() {
        if (this.fw != null) {
            try {
                this.fw.close();
            } catch (IOException ignored) {
            }
        }

    }

    public synchronized void log(String msg) {
        if (msg != null) {
            this.now.setTime(System.currentTimeMillis());
            if (this.fw != null) {
                try {
                    this.fw.append('[');
                    this.fw.append(this.timeFormat.format(this.now));
                    this.fw.append(']');
                    this.fw.append(' ');
                    this.fw.append("[strixplatform]");
                    this.fw.append(' ');
                    this.fw.append(msg);
                    this.fw.append('\n');
                    this.fw.flush();
                } catch (IOException var3) {
                    var3.printStackTrace();
                }

            }
        }
    }
}
