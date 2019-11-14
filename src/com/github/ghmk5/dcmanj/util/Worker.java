package com.github.ghmk5.dcmanj.util;

import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import org.apache.commons.io.FileUtils;
import com.github.ghmk5.dcmanj.gui.ImportDialog;
import com.github.ghmk5.dcmanj.info.AppInfo;
import com.github.ghmk5.dcmanj.info.Entry;

public class Worker extends SwingWorker<ArrayList<Object>, Object[]> {

  Window caller;
  ProgressMonitor progressMonitor;
  AppInfo appInfo;
  ArrayList<Object> processedKeys;
  ArrayList<Entry> entryList;
  int max = 100;
  int progressPart;
  int totalProgress = 0;
  int partMax = 0;
  int processed = 0;
  MagZip zipper;
  ArrayList<Entry> entriesCouldntZip;
  ArrayList<Entry> entriesOfIllegalType;
  ArrayList<Entry> entriesCouldntMove;
  ArrayList<Entry> entriesCouldntInsert;
  File dbFile;

  public Worker(Window caller, ProgressMonitor progressMonitor, ArrayList<Entry> entryList,
      AppInfo appInfo) {
    super();
    this.caller = caller;
    this.progressMonitor = progressMonitor;
    this.entryList = entryList;
    this.appInfo = appInfo;
    progressPart = max / entryList.size();
    processedKeys = new ArrayList<Object>();
    entriesCouldntZip = new ArrayList<Entry>();
    entriesOfIllegalType = new ArrayList<Entry>();
    entriesCouldntMove = new ArrayList<Entry>();
    entriesCouldntInsert = new ArrayList<Entry>();
    dbFile = new File(appInfo.getDbFilePath());
  }

  void setFProgress(float f) {
    setProgress(totalProgress + (int) ((float) progressPart * f));
  }

  @Override
  protected ArrayList<Object> doInBackground() {
    File srcFile;
    File saveDir;
    String newFilename;
    File newFile;
    Double newSize;


    for (Entry entry : entryList) {

      if (progressMonitor.isCanceled()) {
        progressMonitor.close();
        break;
      }

      processed++;
      partMax = totalProgress + progressPart;

      srcFile = entry.getPath().toFile();

      publish(new Object[] {entry.getPath().toFile().getName(), processed, totalProgress});

      // entryをzipして保管するオプションが選択されている && 元がディレクトリの場合は一時ファイルにzipし、移動元を一時ファイルに切り替える
      if (appInfo.getZipToStore() && srcFile.isDirectory()) {
        // 一時ファイルとしてzip
        zipper = new MagZip();
        zipper.addPropertyChangeListener(new PropertyChangeListener() {

          @Override
          public void propertyChange(PropertyChangeEvent evt) {
            if ("progress".equals(evt.getPropertyName())) {
              if (evt.getNewValue() instanceof Float) {
                setFProgress((float) evt.getNewValue());
              }
            }
          }
        });
        try {
          srcFile = zipper.zipToTmp(srcFile, "DcManJTMP", ".zip");
        } catch (IOException e) {
          // 一時ファイルにzipできなかった場合
          entriesCouldntZip.add(entry);
          continue;
        }
      } else if (!srcFile.isDirectory() && !srcFile.isFile()) {
        // 元ファイルが存在しないか、ファイルでもディレクトリでもなかった場合
        entriesOfIllegalType.add(entry);
        continue;
      }
      // 元がディレクトリでzipして保管オブションが選択されていない場合はそのままここに来る

      // 保存先ディレクトリを設定
      if (Objects.isNull(entry.getId())) { // インポートの場合
        saveDir = Util.prepSaveDir(appInfo, srcFile);
      } else { // 移動の場合
        // TODO 適切な保存先を返す処理
        saveDir = Util.prepSaveDir(appInfo, srcFile);
      }

      // 保存先のFileインスタンスを生成
      newFilename = entry.generateNameToSave();
      if (srcFile.isFile() && srcFile.getName().endsWith(".zip")) {
        newFilename += ".zip";
      }
      newFile = new File(saveDir, newFilename);

      // 同名のエントリが既に存在する場合、後置付随詞群に"再"を付け加えて再生成
      if (newFile.exists()) {
        String note = entry.getNote();
        if (Objects.nonNull(note)) {
          ArrayList<String> noteAsList =
              new ArrayList<String>(Arrays.asList(entry.getNote().split(",")));
          noteAsList.add("再");
          note = String.join(",", noteAsList.toArray(new String[noteAsList.size()]));
        } else {
          note = "再";
        }
        entry.setNote(note);
        newFile = new File(saveDir, entry.generateNameToSave() + ".zip");
      }

      // 移動実行
      try {
        if (srcFile.isFile()) {
          FileUtils.moveFile(srcFile, newFile);
        } else {
          FileUtils.moveDirectory(srcFile, newFile);
        }
      } catch (IOException e) {
        entriesCouldntMove.add(entry);
        continue;
      }

      // 元がディレクトリでzipして保存した場合は元のディレクトリを削除
      // TODO 移動の場合の判断分岐
      if (appInfo.getZipToStore() && entry.getPath().toFile().isDirectory()) {
        try {
          FileUtils.deleteDirectory(entry.getPath().toFile());
        } catch (IOException e) {
          entriesCouldntMove.add(entry);
          continue;
        }
      }

      // 処理済みエントリに対応する呼び出し元entryMapのキーを登録
      if (caller instanceof ImportDialog) {
        processedKeys.add(entry.getPath().toFile().getName());
      } else {
        processedKeys.add(entry.getId());
      }

      // Entryのサイズとパスを書き換え
      if (newFile.isFile()) {
        newSize = newFile.length() / 1024d / 1024d;
      } else {
        newSize = FileUtils.sizeOfDirectory(newFile) / 1024d / 1024d;
      }
      entry.setSize(newSize);
      entry.setPath(newFile.toPath());
      entry.setDate(OffsetDateTime.now(ZoneId.systemDefault()));

      // Entryの内容をデータベースに反映
      if (Objects.isNull(entry.getId())) { // インポートの場合
        try {
          ImportDialog.putNewRecord(entry, dbFile);
          setProgress(partMax);
          totalProgress = partMax;
        } catch (SQLException e) {
          entriesCouldntInsert.add(entry);
          continue;
        }
      } else {
        // TODO 移動の場合のSQL文生成と実行
      }

      publish(new Object[] {entry.getPath().toFile().getName(), processed, totalProgress});

    }
    return processedKeys;
  }

  @Override
  protected void process(List<Object[]> chunks) {
    String filename;
    int processed;
    for (Object[] chunk : chunks) {
      filename = (String) chunk[0];
      processed = (int) chunk[1];
      progressMonitor.setNote("(" + String.valueOf(processed) + "/"
          + String.valueOf(entryList.size()) + "): " + filename);
    }
  }

  @Override
  protected void done() {
    if (entriesOfIllegalType.size() > 0 || entriesCouldntZip.size() > 0
        || entriesCouldntMove.size() > 0 || entriesCouldntInsert.size() > 0) {
      StringBuilder stringBuilder = new StringBuilder("処理中にエラーが発生しました\n\n");
      if (entriesOfIllegalType.size() > 0) {
        stringBuilder.append("  以下のエントリはファイルが存在しないか、あるいはファイルでもディレクトリでもないため処理できません\n");
        for (Entry entry : entriesOfIllegalType) {
          stringBuilder.append("    ");
          stringBuilder.append(entry.getPath().toString());
          stringBuilder.append("\n");
        }
        stringBuilder.append("\n");
      }
      if (entriesCouldntZip.size() > 0) {
        stringBuilder.append("  以下のファイルは圧縮できませんでした(ファイル名もしくは一時ファイル領域に問題?)\n");
        for (Entry entry : entriesCouldntZip) {
          stringBuilder.append("    ");
          stringBuilder.append(entry.getPath().toString());
          stringBuilder.append("\n");
        }
        stringBuilder.append("\n");
      }
      if (entriesCouldntMove.size() > 0) {
        stringBuilder.append("  以下のファイルは保存先に移動できませんでした(ファイルが使用中?)\n");
        for (Entry entry : entriesCouldntMove) {
          stringBuilder.append("    ");
          stringBuilder.append(entry.getPath().toString());
          stringBuilder.append("\n");
        }
        stringBuilder.append("\n");
      }
      if (entriesCouldntInsert.size() > 0) {
        stringBuilder.append("  以下のエントリはデータベースに登録できませんでした(ファイル名もしくはデータベースファイルに問題?)\n");
        for (Entry entry : entriesCouldntInsert) {
          stringBuilder.append("    ");
          stringBuilder.append(entry.getPath().toString());
          stringBuilder.append("\n");
        }
        stringBuilder.append("\n");
      }

      String message = stringBuilder.toString();
      JOptionPane.showMessageDialog(caller, message, "エラー", JOptionPane.ERROR_MESSAGE);
    }
    if (caller instanceof ImportDialog) {
      ((ImportDialog) caller).refreshMap(processedKeys);
      ((ImportDialog) caller).updateTable();
    }
    setProgress(max);
  }
}
