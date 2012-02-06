package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class RoundRobinList extends ArrayList<String> {

    private static final long serialVersionUID = 7969804056598809927L;

    private final ArrayList<String> blackList = new ArrayList<String>();
    private int currentIndex = -1;

    public RoundRobinList() {
    }

    public RoundRobinList(final String fileName) {
        loadFromTextFile(fileName, true);
    }

    public RoundRobinList(final String fileName, final boolean clearList) {
        loadFromTextFile(fileName, clearList);
    }

    public String getNextHost() {
        if (currentIndex != -1) {
            this.ignoreHost();
        }
        if (size() <= 0) {
            return null;
        }
        currentIndex = new Random(System.currentTimeMillis()).nextInt(size());
        return get(currentIndex);
    }

    public boolean ignoreHost() {
        if (currentIndex == -1) {
            return false;
        }

        blackList.add(get(currentIndex));
        this.remove(currentIndex);
        currentIndex = -1;

        return true;
    }

    public boolean ignoreHost(final String host) {
        if (contains(host)) {
            blackList.add(host);
            this.remove(host);
            return true;
        }
        return false;
    }

    public void loadFromTextFile(final String fileName, final boolean clearList) {
        if (clearList && !isEmpty()) {
            clear();
            blackList.clear();
            currentIndex = -1;
        }

        final File fPointer = new File(fileName);

        BufferedReader reader = null;
        FileReader fr = null;
        try {
            fr = new FileReader(fPointer);
            reader = new BufferedReader(fr);
            String text = null;

            while ((text = reader.readLine()) != null) {
                if (text == null || text.isEmpty()) {
                    continue;
                }
                this.add(text);
            }
        } catch (final FileNotFoundException e) {
            System.out.println(e.toString());
        } catch (final IOException e) {
            System.out.println(e.toString());
        } finally {
            try {
                if (fr != null) {
                    fr.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (final IOException e) {
            }
        }
    }

    public void reset() {
        currentIndex = -1;
        if (blackList.isEmpty()) {
            return;
        }
        this.addAll(blackList);
        blackList.clear();
    }
}