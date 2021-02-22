package com.github.ghmk5.dcmanj.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

public class MagZip {

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

  public MagZip() {
    progress = 0f;
    oldProgress = 0f;
    numFilesToGo = 0;
    numFilesTreated = 0;
  }

  private ArrayList<File> dirGlob(File dir, ArrayList<File> arrayOfFiles, Boolean zipHidden)
      throws IOException {
    for (File entry : dir.listFiles()) {
      if (!zipHidden && entry.isHidden())
        continue;
      if (entry.isDirectory()) {
        arrayOfFiles.add(entry);
        dirGlob(entry, arrayOfFiles, zipHidden);
      } else {
        arrayOfFiles.add(entry);
      }
    }
    return arrayOfFiles;
  }

  private void encode(ZipOutputStream zos, File file, String internalPath) throws IOException {
    ZipEntry zipEntry = new ZipEntry(internalPath);
    try {
      zos.putNextEntry(zipEntry);
      if (!internalPath.endsWith("/")) {
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
          for (;;) {
            int len = is.read(buf);
            if (len < 0)
              break;
            zos.write(buf, 0, len);
          }
        }
      }
    } catch (ZipException e) {
      System.out.println(internalPath + "の追加時にエラーが発生");
    }
    numFilesTreated++;
    oldProgress = progress;
    progress = (float) numFilesTreated / (float) numFilesToGo;
    this.pcs.firePropertyChange("progress", oldProgress, progress);
  }

  private void write(ZipOutputStream zos, File file, Boolean isSingle, Boolean zipHidden)
      throws IOException {

    if (file.isDirectory()) {
      ArrayList<File> arrayOfFiles = new ArrayList<>();
      ArrayList<File> filesToGo = dirGlob(file, arrayOfFiles, zipHidden);
      numFilesToGo = filesToGo.size();
      for (File entry : filesToGo) {
        Path relativePath;
        if (isSingle) {
          relativePath = file.toPath().relativize(entry.toPath());
        } else {
          relativePath = file.getParentFile().toPath().relativize(entry.toPath());
        }
        String pathToEncode = relativePath.toString();
        if (System.getProperty("os.name").toLowerCase().startsWith("windows"))
          pathToEncode = pathToEncode.replace('\\', '/');
        if (entry.isDirectory())
          pathToEncode = pathToEncode + "/";
        encode(zos, entry, pathToEncode);
      }
    } else {
      encode(zos, file, file.getName());
    }

  }

  /**
   * zipファイルを保存する
   *
   * @param zos
   * @param arrayOfFiles 要素数が1で、かつ唯一の要素がディレクトリの場合、最上層のディレクトリが除かれる
   * @param zipHidden 隠しファイルを書庫に含めるか否か falseの場合、それ自体が隠し属性のファイルが除外される他、ファイル自体が隠し属性でなくとも
   *        パスに隠し属性のディレクトリが含まれる場合も除外される
   * @throws IOException
   */
  public void mkZip(ZipOutputStream zos, ArrayList<File> arrayOfFiles, Boolean zipHidden)
      throws IOException {
    Boolean isSingle = false;
    if (arrayOfFiles.size() == 1)
      isSingle = true;
    for (File file : arrayOfFiles) {
      write(zos, file, isSingle, zipHidden);
    }
  }

  public File zipToTmp(File file, String prefix, String suffix) throws IOException {
    File tmpFile = File.createTempFile(prefix, suffix);
    if (!file.isFile() && !file.isDirectory()) {
      throw new IllegalArgumentException("MagZip.zipToTmpにファイルでもディレクトリでもないオブジェクトが渡された(シンボリックリンク?)");
    }
    ArrayList<File> listOfFiles = new ArrayList<File>();
    listOfFiles.add(file);
    // Windows10のエクスプローラーは適切にフラグが立ててあればzipファイル内のエントリがUTF-8でも文字化けせずに読めるようなので、UTF-8で保存するように変更した
    ZipOutputStream zos =
        new ZipOutputStream(new FileOutputStream(tmpFile), Charset.forName("UTF-8"));
    mkZip(zos, listOfFiles, false);
    zos.close();
    return tmpFile;
  }

  public static void main(String[] args) throws Exception {

    File fileToZip = new File("/Users/mk5/Desktop/nestedDir");
    File file2ToZip = new File("/Users/mk5/Desktop/propsを保存している箇所.txt");
    ArrayList<File> arrayOfFiles = new ArrayList<>();
    arrayOfFiles.add(fileToZip);
    arrayOfFiles.add(file2ToZip);

    File zipFile = new File("/Volumes/drop/sample.zip"); // 作成するzipファイルの名前
    // Windows10のエクスプローラーは適切にフラグが立ててあればzipファイル内のエントリがUTF-8でも文字化けせずに読めるようなので、UTF-8で保存するように変更した
    ZipOutputStream zos =
        new ZipOutputStream(new FileOutputStream(zipFile), Charset.forName("UTF-8"));

    MagZip zipper = new MagZip();

    try {
      zipper.mkZip(zos, arrayOfFiles, false);
    } catch (Exception e) {
      // TODO 自動生成された catch ブロック
      e.printStackTrace();
    }
    zos.close();

  }

}
