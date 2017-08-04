package org.shipkit.internal.util;

import org.shipkit.internal.notes.util.IOUtil;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class PropertiesUtil {

    public static Properties readProperties(File versionFile) {
        Properties p = new Properties();
        FileReader reader = null;
        try {
            reader = new FileReader(versionFile);
            p.load(reader);
        } catch (Exception e) {
            throw new RuntimeException("Problems reading version file: " + versionFile);
        } finally {
            IOUtil.close(reader);
        }
        return p;
    }
}
