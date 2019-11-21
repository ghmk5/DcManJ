package com.github.ghmk5.dcmanj.util;

import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import com.github.ghmk5.dcmanj.gui.BrowserWindow;
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
  MagUnzip unzipper;
  ArrayList<Entry> entriesCouldntZip;
  ArrayList<Entry> entriesOfIllegalType;
  ArrayList<Entry> entriesWithNameDuplicated;
  ArrayList<Entry> entriesNoChangeInReImport;
  ArrayList<Entry> entriesCouldntMove;
  ArrayList<Entry> entriesCouldntInsert;
  ArrayList<Entry> entriesCouldntUpdate;
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
    entriesWithNameDuplicated = new ArrayList<Entry>();
    entriesNoChangeInReImport = new ArrayList<Entry>();
    entriesCouldntMove = new ArrayList<Entry>();
    entriesCouldntInsert = new ArrayList<Entry>();
    entriesCouldntUpdate = new ArrayList<Entry>();
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

    Boolean isZipped;
    Boolean isUnZipped;
    Boolean isRenamed;

    for (Entry entry : entryList) {

      isZipped = false;
      isUnZipped = false;
      isRenamed = false;

      if (progressMonitor.isCanceled()) {
        progressMonitor.close();
        break;
      }

      processed++;
      partMax = totalProgress + progressPart;

      srcFile = entry.getPath().toFile();

      publish(new Object[] {entry.getPath().toFile().getName(), processed, totalProgress});

      if (srcFile.isFile()) {
        if (caller instanceof BrowserWindow) {
          // 移動処理であり、移動時にunzipするオプションが選択されている場合のみ一時ファイルとしてunzipし、移動元を一時ファイルに切り替える
          if (!appInfo.getMoveAsReImport() && appInfo.getUnzipOnMove()
              && srcFile.getName().matches(".+\\.[zZ][iI][pP]$")) {
            isUnZipped = true;

            try {
              unzipper = new MagUnzip(srcFile, new String[] {"MS932", "UTF-8"});
              unzipper.addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                  if ("progress".equals(evt.getPropertyName())) {
                    if (evt.getNewValue() instanceof Float) {
                      setFProgress((float) evt.getNewValue());
                    }
                  }
                }
              });
              srcFile = unzipper.unzipToTmp(false);
            } catch (IOException e) {
              // 一時ファイルにunzipできなかった場合
              entriesCouldntZip.add(entry);
              continue;
            }
          }
        }
      } else if (srcFile.isDirectory()) {
        // 元ファイルがディレクトリ
        // 新規インポートであり、zipして保存オプションが選択されている
        // または 移動処理であり、再インポートオプションと(インポート時)zipして保存オプションが選択されている
        // または 移動処理であり、zipして移動オプションが選択されている
        // 以上の条件に該当する場合、一時ファイルにzipし、移動元を一時ファイルに切り替える
        if ((caller instanceof ImportDialog && appInfo.getZipToStore())
            || (caller instanceof BrowserWindow && appInfo.getZipOnMove())
            || (caller instanceof BrowserWindow && appInfo.getMoveAsReImport()
                && appInfo.getZipToStore())) {
          isZipped = true;
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
        }
      } else {
        // 元ファイルが存在しないか、ファイルでもディレクトリでもなかった場合
        entriesOfIllegalType.add(entry);
        continue;
      }
      // 元がディレクトリでzipして保管オブションが選択されていない場合はそのままここに来る

      // 保存先ディレクトリを設定
      if ((caller instanceof ImportDialog)
          || (caller instanceof BrowserWindow && appInfo.getMoveAsReImport())) { // インポートの場合
        saveDir = Util.prepSaveDir(appInfo, srcFile, true);
      } else if (appInfo.getSelectDestDirOnMove()) { // 移動先を指定される場合
        saveDir = Util.prepSaveDir(appInfo, srcFile, false);
      } else { // 移動先を指定せず、圧縮/展開/再命名だけ行う場合
        saveDir = entry.getPath().getParent().toFile();
      }

      // 保存先のFileインスタンスを生成
      newFilename = entry.generateNameToSave();
      if (srcFile.isFile() && srcFile.getName().endsWith(".zip")) {
        newFilename += ".zip";
      }
      newFile = new File(saveDir, newFilename);
      if (!srcFile.getName().equals(newFilename)) {
        isRenamed = true;
      }

      // 再インポートで圧縮/展開/ファイル名修正のいずれも発生しない場合、リストに入れておいてスキップ
      if (caller instanceof BrowserWindow && appInfo.getMoveAsReImport() && !isZipped && !isUnZipped
          && !isRenamed && srcFile.getAbsolutePath().equals(newFile.getAbsolutePath())) {
        entriesNoChangeInReImport.add(entry);
        continue;
      }

      // 同名のエントリが既に存在する場合
      if (newFile.exists()) {
        // 移動処理で移動先を指定しない(圧縮/展開/再命名のみ行う設定でなにもやることがなかった)場合
        // または再インポート指定で移動を伴わなかった場合、なにもせずスキップ
        if ((caller instanceof BrowserWindow) && !appInfo.getSelectDestDirOnMove() && !isZipped
            && !isUnZipped && !isRenamed) {
          continue;
        }

        // インポート/再インポート/移動で偶然にファイル名が被った場合、後置付随詞群に"再"を付け加えて再生成
        entriesWithNameDuplicated.add(entry);
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

      // 圧縮/展開したため元のディレクトリを削除する必要がある場合の処理
      if (isZipped) {
        // 元がディレクトリで圧縮した場合
        try {
          FileUtils.deleteDirectory(entry.getPath().toFile());
        } catch (IOException e) {
          entriesCouldntMove.add(entry);
          continue;
        }
      } else if (isUnZipped) {
        try {
          Files.delete(entry.getPath());
        } catch (IOException e) {
          // TODO ここでファイルが消せない問題
          entriesCouldntMove.add(entry);
          e.printStackTrace();
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
          Util.putNewRecord(entry, dbFile);
          setProgress(partMax);
          totalProgress = partMax;
        } catch (SQLException e) {
          entriesCouldntInsert.add(entry);
          continue;
        }
      } else {
        ArrayList<Entry> entriesToUpdate = new ArrayList<Entry>();
        entriesToUpdate.add(entry);
        try {
          Util.updateDB(entriesToUpdate, dbFile);
          setProgress(partMax);
          totalProgress = partMax;
        } catch (SQLException e) {
          entriesCouldntUpdate.add(entry);
          continue;
        }
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
        || entriesCouldntMove.size() > 0 || entriesCouldntInsert.size() > 0
        || entriesCouldntUpdate.size() > 0) {
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
        stringBuilder.append("  以下のファイルは圧縮 / 展開できませんでした(ファイル名もしくは一時ファイル領域に問題?)\n");
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
      if (entriesCouldntInsert.size() > 0) {
        stringBuilder.append("  以下のエントリの内容でデータベースを更新できませんでした(ファイル名もしくはデータベースファイルに問題?)\n");
        for (Entry entry : entriesCouldntUpdate) {
          stringBuilder.append("    ");
          stringBuilder.append(entry.getPath().toString());
          stringBuilder.append("\n");
        }
        stringBuilder.append("\n");
      }
      String message = stringBuilder.toString();
      JOptionPane.showMessageDialog(caller, message, "エラー", JOptionPane.ERROR_MESSAGE);
    }

    // 同名ファイルが存在した場合 および 再インポート指定で実質的変更がなかったためスキップされた場合
    if (entriesWithNameDuplicated.size() > 0 || entriesNoChangeInReImport.size() > 0) {
      StringBuilder stringBuilder = new StringBuilder();
      if (entriesWithNameDuplicated.size() > 0) {
        stringBuilder.append("以下のエントリは既に同名ファイルが存在したためファイル名末尾に(再)が付与されました\n");
        for (Entry entry : entriesWithNameDuplicated) {
          stringBuilder.append("  ");
          stringBuilder.append(entry.getPath().toString());
          stringBuilder.append("\n");
        }
        stringBuilder.append("\n");
      }
      if (entriesNoChangeInReImport.size() > 0) {
        stringBuilder.append("以下のエントリは指定の処理内容で実質的変更を伴わなかったためスキップされました\n");
        for (Entry entry : entriesNoChangeInReImport) {
          stringBuilder.append("  ");
          stringBuilder.append(entry.getPath().toString());
          stringBuilder.append("\n");
        }
        stringBuilder.append("\n");
      }
      String message = stringBuilder.toString();
      JOptionPane.showMessageDialog(caller, message, "情報", JOptionPane.INFORMATION_MESSAGE);
    }

    // インポートの場合、ダイアログのテーブルを更新
    if (caller instanceof ImportDialog) {
      ((ImportDialog) caller).refreshMap(processedKeys);
      ((ImportDialog) caller).updateTable();
    } else if (caller instanceof BrowserWindow) {
      ((BrowserWindow) caller).refreshTable(entryList);
    }
    setProgress(max);
  }
}
