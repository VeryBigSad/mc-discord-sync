package io.github.thedoctorone;

import java.io.*;
import java.util.ArrayList;

public class SyncFileOperation {
    private String path;
    private File file;
    SyncFileOperation() throws IOException {
        path = "plugins/discord/syncList.dat";
        file = new File(path);
        if(!file.exists()) {
            writeSyncFile(new ArrayList<String>());
        }
    }

    public ArrayList<String> readSyncFile() {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);

            ArrayList<String> toReturn = (ArrayList<String>) ois.readObject();

            ois.close();
            fis.close();
            return toReturn;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<String>();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<String>();
        }
    }

    public boolean writeSyncFile(ArrayList<String> save) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(save);

            oos.close();
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
