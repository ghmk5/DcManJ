package com.github.ghmk5.dcmanj.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class MagUnzip {

  private final static int BUFFER_SIZE = 2048;
  private ZipFile zipFile;
  ArrayList<ZipEntry> zipEntries;
  private HashMap<String, ArrayList<ZipEntry>> mappedEntries;
  private float progress;
  private float oldProgress;
  private int numFilesToGo;
  private int numFilesTreated;

  private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    this.pcs.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    this.pcs.removePropertyChangeListener(listener);
  }

  static byte[] buf = new byte[1024];

  public MagUnzip(File fileToUnzip, String[] charsetNames) throws ZipException, IOException {
    progress = 0f;
    oldProgress = 0f;
    numFilesToGo = 0;
    numFilesTreated = 0;

    for (String charsetName : charsetNames) {
      try {
        zipFile = new ZipFile(fileToUnzip, Charset.forName(charsetName));
        zipEntries = new ArrayList<ZipEntry>(Collections.list(zipFile.entries()));
        numFilesToGo = zipEntries.size();
        break;
      } catch (IllegalArgumentException e) {
        continue;
      }
    }

    if (Objects.isNull(zipEntries)) {
      throw new IllegalArgumentException("no charsetName suitable for the archive.");
    }

  }

  private void mapEntries() {
    String[] splittedPaths;
    ArrayList<ZipEntry> zipEntriesInDir;
    mappedEntries = new HashMap<String, ArrayList<ZipEntry>>();
    for (ZipEntry zipEntry : zipEntries) {
      splittedPaths = zipEntry.getName().split("/");
      if (!splittedPaths[0].equals("__MACOSX")) {
        if (Objects.nonNull(mappedEntries.get(splittedPaths[0]))) {
          zipEntriesInDir = mappedEntries.get(splittedPaths[0]);
        } else {
          zipEntriesInDir = new ArrayList<ZipEntry>();
        }
        zipEntriesInDir.add(zipEntry);
        mappedEntries.put(splittedPaths[0], zipEntriesInDir);
      }
    }
  }

  private String putOnRoot(String pathString) {
    String[] splittedPath = pathString.split("/");
    ArrayList<String> pathList = new ArrayList<String>(Arrays.asList(splittedPath));
    pathList.remove(0);
    if (pathList.size() == 0) {
      return null;
    } else {
      splittedPath = pathList.toArray(new String[pathList.size()]);
      return String.join("/", splittedPath);
    }
  }

  public File unzip(File destDir, Boolean allowSingleRootDir) throws IOException {
    Pattern pattern = Pattern.compile("(.+)+ \\((\\d+)\\)$");
    Matcher matcher;
    String dirName =
        Paths.get(zipFile.getName()).getFileName().toString().replaceAll("(\\.\\w+)+$", "");
    dirName = Normalizer.normalize(dirName, Normalizer.Form.NFKC).replaceAll("~", "～")
        .replaceAll("!", "！").replaceAll("\\\\", "＼").replaceAll("/", "／").replaceAll(":", "：")
        .replaceAll("\\?", "？").replaceAll("\"", "”").replaceAll("<", "＜").replaceAll(">", "＞")
        .replaceAll("\\|", "｜").replaceAll("\\*", "＊");
    File unzippedDir = new File(destDir, dirName);

    // 所与の名を持つディレクトリが既に存在する場合、Windows Explorerの流儀で名前にカッコ入り数字を足して新しく作る
    while (unzippedDir.exists()) {
      matcher = pattern.matcher(unzippedDir.getName());
      if (matcher.find()) {
        unzippedDir = new File(destDir,
            matcher.group(1) + " (" + String.valueOf(Integer.valueOf(matcher.group(2)) + 1) + ")");
      } else {
        unzippedDir = new File(destDir, dirName + " (2)");
      }
    }

    BufferedInputStream bufIS;

    mapEntries();
    String intenalPath;
    String pathToWrite;
    for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {

      if (zipEntry.getName().startsWith("__MACOSX") || zipEntry.getName().endsWith(".DS_Store")) {
        continue;
      }

      if (!allowSingleRootDir && mappedEntries.keySet().size() == 1) {
        intenalPath = putOnRoot(zipEntry.getName());
        if (Objects.isNull(intenalPath)) {
          continue;
        }
      } else {
        intenalPath = zipEntry.getName();
      }

      // 書き出すパスを調整
      ArrayList<String> list = new ArrayList<String>();
      for (String string : intenalPath.split("/")) {
        string = Normalizer.normalize(string, Normalizer.Form.NFKC).replaceAll("~", "～")
            .replaceAll("!", "！").replaceAll("\\\\", "＼").replaceAll("/", "／").replaceAll(":", "：")
            .replaceAll("\\?", "？").replaceAll("\"", "”").replaceAll("<", "＜").replaceAll(">", "＞")
            .replaceAll("\\|", "｜").replaceAll("\\*", "＊");
        list.add(string);
      }
      pathToWrite = String.join("/", list.toArray(new String[list.size()]));

      // create destination file
      File destFile = new File(unzippedDir, pathToWrite);

      // create parent directories if needed
      File parentDestFile = destFile.getParentFile();
      parentDestFile.mkdirs();

      if (!zipEntry.isDirectory()) {
        bufIS = new BufferedInputStream(zipFile.getInputStream(zipEntry));
        int currentByte;

        // buffer for writing file
        byte data[] = new byte[BUFFER_SIZE];

        // write the current file to disk
        FileOutputStream fOS = new FileOutputStream(destFile);
        BufferedOutputStream bufOS = new BufferedOutputStream(fOS, BUFFER_SIZE);

        while ((currentByte = bufIS.read(data, 0, BUFFER_SIZE)) != -1) {
          bufOS.write(data, 0, currentByte);
        }

        // close BufferedOutputStream
        bufOS.flush();
        bufOS.close();
      }
      numFilesTreated++;
      oldProgress = progress;
      progress = (float) numFilesTreated / (float) numFilesToGo;
      this.pcs.firePropertyChange("progress", oldProgress, progress);
    }
    zipFile.close();
    return unzippedDir;
  }

  public File unzipToTmp(Boolean allowSingleRootDir) throws IOException {
    return unzip(new File(System.getProperty("java.io.tmpdir")), allowSingleRootDir);
  }

  public static void main(String[] args) {
    String[] charsetNames = {"MS932", "UTF-8"};
    File fileToUnzip = new File("D:\\drop\\hoge.zip");
    try {
      MagUnzip unzipper = new MagUnzip(fileToUnzip, charsetNames);
      // File result = unzipper.unzip(new File("D://drop/"), false);
      File result = unzipper.unzipToTmp(false);
      Util.showInFiler(null, result);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
