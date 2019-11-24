package com.github.ghmk5.dcmanj.info;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import com.github.ghmk5.dcmanj.util.Util;

public class Entry {

  // 書誌情報
  private Integer id;
  private String type;
  private Boolean adult;
  private String circle;
  private String author;
  private String title;
  private String subtitle;
  private String volume;
  private String issue;
  private String note;
  private Integer pages;
  private Double size;
  private Path path;
  private OffsetDateTime date;
  private String original;
  private String release;

  // noteを生成するために使うリスト
  private ArrayList<String> noteAsList;

  // タイプの判断材料を入れておくための真偽値
  private Boolean isDoujinshi;
  private Boolean isComic;
  private Boolean isMagazine;
  private Boolean isNovel;

  // Fileを引数にとるコンストラクタ内で使用
  private AppInfo appInfo;

  private static HashMap<String, String> columnNameMap;
  private static HashMap<String, String> tagNameMap;
  private static HashMap<String, String> dataClassMap;

  public static String[] COLUMN_NAMES;
  public static String[] TAG_NAMES;

  static {
    //@formatter:off
    String[][] strings = {
      {"ID", "rowid", "Integer"},
      {"種別", "type", "String"},
      {"成人向け", "adult", "Boolean"},
      {"サークル", "circle", "String"},
      {"著者", "author", "String"},
      {"タイトル", "title", "String"},
      {"副題", "subtitle", "String"},
      {"巻号", "volume", "String"},
      {"issue", "issue", "String"},
      {"備考", "note", "String"},
      {"元ネタ", "original", "String"},
      {"配布イベ", "release", "String"},
      {"頁数", "pages", "Integer"},
      {"容量", "size", "Double"},
      {"パス", "path", "Path"},
      {"日付", "date", "OffsetDateTime"}
    };
    //@formatter:on

    columnNameMap = new HashMap<String, String>();
    tagNameMap = new HashMap<String, String>();
    dataClassMap = new HashMap<String, String>();

    List<String> tagNameList = new ArrayList<String>();
    List<String> columnNameList = new ArrayList<String>();
    for (String[] row : strings) {
      columnNameMap.put(row[0], row[1]);
      tagNameMap.put(row[1], row[0]);
      dataClassMap.put(row[0], row[2]);
      dataClassMap.put(row[1], row[2]);
      tagNameList.add(row[0]);
      columnNameList.add(row[1]);
    }

    COLUMN_NAMES = columnNameList.toArray(new String[columnNameList.size()]);
    TAG_NAMES = tagNameList.toArray(new String[tagNameList.size()]);

  }

  public static String columnNameOf(String tagName) {
    return columnNameMap.get(tagName);
  }

  public static String tagNameOf(String columnName) {
    return tagNameMap.get(columnName);
  }

  public static String dataClassNameOf(String name) {
    return dataClassMap.get(name);
  }

  public Entry(File file, AppInfo appInfo) throws ZipException, IOException {

    this.appInfo = appInfo;

    path = file.toPath();
    adult = null;
    isComic = false;
    isDoujinshi = false;
    isMagazine = false;
    isNovel = false;

    // 拡張子があれば除く
    String name = file.getName().replaceFirst("\\.[a-zA-Z0-9]{1,4}$", "");

    // 正規化し、いくつかの文字を元に戻す(ファイル名に使えない文字関連はファイル生成時に別に変換)
    name = Normalizer.normalize(name, Normalizer.Form.NFKC);
    name = name.replaceAll("~", "～");
    name = name.replaceAll("!", "！");
    name = name.replaceAll("//?", "？");

    // カッコ入り要素間のスペースを除く
    name = name.replaceAll("\\) +\\(", ")(");

    // 最初のスクエアブラケットとその前後に分解 分解出来なければファイル名をタイトル+後置付随詞とみなす
    Pattern pattern = Pattern.compile("^([^\\[]+)?\\[([^\\]]+)\\](.+)$");
    Matcher matcher = pattern.matcher(name);
    String prePositions = "";
    String insideOfBracket = "";
    String body = "";;
    if (matcher.find()) {
      prePositions = matcher.group(1);
      insideOfBracket = matcher.group(2);
      body = matcher.group(3);
    } else {
      body = name;
    }

    // bodyをタイトルと後置付随詞に分解
    String postPositions;
    pattern = Pattern.compile("([\\(\\[【].+)$");
    matcher = pattern.matcher(body);
    if (matcher.find()) {
      postPositions = matcher.group(1);
      title = body.replace(postPositions, "");
    } else {
      postPositions = null;
      title = body;
    }

    // 前置付随詞群をパース
    if (Objects.nonNull(prePositions)) {
      prePositions = parsePreps(prePositions);
    }

    // 最初のブラケットの中身をパース
    circle = null;
    author = null;
    pattern = Pattern.compile("([^\\(]+)\\(([^\\)]+)\\)");
    matcher = pattern.matcher(insideOfBracket);
    if (matcher.find()) {
      circle = matcher.group(1).trim();
      author = matcher.group(2).trim();
      isDoujinshi = true;
    } else if (insideOfBracket.contains("雑誌")) {
      isMagazine = true;
      isComic = false;
    } else if (isDoujinshi) {
      circle = insideOfBracket.trim();
    } else if (!insideOfBracket.equals("")) {
      author = insideOfBracket.trim();
    }

    // タイトル部分をパース -- issueを検出
    issue = null;
    pattern = Pattern.compile("(\\d{4})年(\\d{1,2})月号");
    matcher = pattern.matcher(title);
    Pattern pattern2 = Pattern.compile("(\\d{4})-(\\d{1,2})");
    Matcher matcher2 = pattern2.matcher(title);
    if (matcher.find()) {
      issue = matcher.group(1) + "-" + String.format("%02d", Integer.valueOf(matcher.group(2)));
      title = title.replace(matcher.group(0), "");
      title = trim(title);
      isMagazine = true;
    } else if (matcher2.find()) {
      issue = matcher2.group(1) + "-" + String.format("%02d", Integer.valueOf(matcher2.group(2)));
      title = title.replace(matcher2.group(0), "");
      title = trim(title);
      isMagazine = true;
    }

    // タイトル部分をパース -- volumeを検出
    title = title.trim();
    String[] volRegExs = {" ([Vv][Oo][Ll]\\.? ?[\\d\\.]+) ?([^\\d]+)?", " (Vol[\\d]+)",
        " ([^\\d ]{2,4}[\\d\\.]{1,3}) ?(.+)?", " 第?([\\d\\.]{1,3})巻 ?(.+)?"};
    for (String volRegEx : volRegExs) {
      pattern = Pattern.compile(volRegEx);
      matcher = pattern.matcher(title);
      if (matcher.find()) {
        volume = matcher.group(1);
        subtitle = matcher.group(2);
        if (Objects.nonNull(subtitle)) {
          subtitle = trim(subtitle);
        }
        title = title.replace(matcher.group(0), "");
        break;
      }
    }
    if (Objects.isNull(volume)) {
      pattern = Pattern.compile("^([^\\d]+) ?(\\d+)$");
      matcher = pattern.matcher(title);
      if (matcher.find()) {
        volume = matcher.group(2);
        title = matcher.group(1);
      }
    }
    title = trim(title);

    // タイトル部分をパース -- 付録表記を検出
    note = null;
    if (Objects.isNull(noteAsList)) {
      noteAsList = new ArrayList<String>();
    }
    pattern = Pattern.compile(" \\+ [^ ]+[(カード)|(リーフレット)|(小冊子)]$");
    matcher = pattern.matcher(title);
    String foundString;
    if (matcher.find()) {
      foundString = matcher.group(0);
      noteAsList.add(trim(foundString));
      title = title.replace(foundString, "");
      title = trim(title);
    }

    // 後置付随詞群をパース
    if (Objects.nonNull(postPositions)) {
      parsePostps(postPositions);
    }

    // パース後に残った前置付随詞があれば備考の先頭に追加
    // ∵ 最後に追加すると(もしあれば)元ネタ候補の位置が崩れるため
    if (Objects.nonNull(prePositions)) {
      for (String prePosition : prePositions.split(",")) {
        noteAsList.add(0, prePosition);
      }
    }

    // 備考の重複を除く
    noteAsList = new ArrayList<String>(new LinkedHashSet<>(noteAsList));

    // 備考のリストをStringにしてフィールドに代入
    if (noteAsList.size() > 0) {
      note = String.join(",", noteAsList.toArray(new String[noteAsList.size()]));
    }

    // 可能なら種別をセット
    setType();

    // 種別が同人誌でadultフラグがnullならtrueにセット
    if (Objects.nonNull(type) && type.equals("doujinshi") && Objects.isNull(adult)) {
      adult = true;
    }

    // 頁数とサイズをセット
    acquireSizeAndPages();
  }

  /**
   * データベースの戻り値からEntryのインスタンスを生成する データベースに要求するフィールドは 'rowid, *'でなければならない
   *
   * @param resultSet
   * @throws SQLException
   */
  public Entry(ResultSet resultSet) throws SQLException {

    id = resultSet.getInt("rowid");
    type = resultSet.getString("type");
    if (Objects.isNull(resultSet.getString("adult"))) {
      adult = null;
    } else {
      adult = Boolean.valueOf(resultSet.getString("adult"));
    }
    circle = resultSet.getString("circle");
    author = resultSet.getString("author");
    title = resultSet.getString("title");
    subtitle = resultSet.getString("subtitle");
    volume = resultSet.getString("volume");
    issue = resultSet.getString("issue");
    note = resultSet.getString("note");
    pages = resultSet.getInt("pages");
    size = resultSet.getDouble("size");
    path = Path.of(resultSet.getString("path"));
    if (Objects.nonNull(resultSet.getString("date"))) {
      date = OffsetDateTime.parse(resultSet.getString("date"), Util.DTF);
    } else {
      date = null;
    }
    original = resultSet.getString("original");
    release = resultSet.getString("release");

  }

  /**
   * 他のEntryインスタンスと内容を比較する<BR>
   * 2つのEntryインスタンスのデータベース登録内容が同一となるか否か、すなわち<BR>
   * データベース登録/参照される16個のフィールド全てが一致するか否かを示す真偽値を返す
   *
   * @param entry 比較対象のEntryインスタンス
   * @return データベースレコードとして取り込んだ際に同一内容となるなら真
   */
  public Boolean isIdenticalTo(Entry entry) {
    return this.getColumnValueMap().equals(entry.getColumnValueMap());
  }

  /**
   * データベースに登録するフィールド値について所与のEntryインスタンスとの相違を調べ、<BR>
   * 一致しないものを値に持つマップを返す
   *
   * @param entry 比較対象のEntryインスタンス
   * @return データベースカラム名をキーに持つマップ
   */
  public HashMap<String, Object> getUpdatedValueMap(Entry entryFromDB) {
    HashMap<String, Object> updatedValueMap = new HashMap<String, Object>();
    HashMap<String, Object> valueMap = getColumnValueMap();
    HashMap<String, Object> dBEntryValueMap = entryFromDB.getColumnValueMap();
    for (String columnName : valueMap.keySet()) {
      if (!Util.isIdentical(valueMap.get(columnName), dBEntryValueMap.get(columnName))) {
        updatedValueMap.put(columnName, valueMap.get(columnName));
      }
    }
    return updatedValueMap;
  }

  /**
   * データベースカラム名と対応する値を紐付けたマップを返す
   *
   * @return HashMap<String データベースカラム名, Object 値> columnValueMap
   */
  public HashMap<String, Object> getColumnValueMap() {
    HashMap<String, Object> columnValueMap = new HashMap<String, Object>();
    columnValueMap.put("rowid", id);
    columnValueMap.put("type", type);
    columnValueMap.put("adult", adult);
    columnValueMap.put("circle", circle);
    columnValueMap.put("author", author);
    columnValueMap.put("title", title);
    columnValueMap.put("subtitle", subtitle);
    columnValueMap.put("volume", volume);
    columnValueMap.put("issue", issue);
    columnValueMap.put("note", note);
    columnValueMap.put("pages", pages);
    columnValueMap.put("size", size);
    columnValueMap.put("path", path);
    columnValueMap.put("date", date);
    columnValueMap.put("original", original);
    columnValueMap.put("release", release);
    return columnValueMap;
  }

  // フラグ値からエントリ種別を判定してtypeフィールドに代入
  private void setType() {
    Boolean[] booleans = {this.isDoujinshi, this.isComic, this.isMagazine, this.isNovel};
    int trueCount = 0;
    for (Boolean b : booleans) {
      if (b) {
        trueCount++;
      }
    }
    if (trueCount == 1) {
      if (isDoujinshi) {
        type = "doujinshi";
      } else if (isComic) {
        type = "comic";
      } else if (isMagazine) {
        type = "magazine";
      } else {
        type = "novel";
      }
    }
  }

  // 後置付随詞群をパースする
  private void parsePostps(String postPositions) {

    // 後置付随詞群を分割
    ArrayList<String> postPositionsList =
        new ArrayList<String>(Arrays.asList(splitNotes(postPositions)));

    // 後置付随詞群をパース
    ArrayList<String> elementsToRemove = new ArrayList<String>();
    if (Objects.isNull(noteAsList)) {
      noteAsList = new ArrayList<String>();
    }

    // appInfo.noteRegExStringsに登録された要素がリストの先頭にくるように
    // = 登録されていない要素がリストの最後になるようにリストを構築
    // ∵ 登録されていない要素は元ネタの可能性が高いので、ImportDialogのコンテキストメニューアイテム
    // "最後の要素を元ネタに移動"で扱えるようにするため
    String foundString;
    for (String postPosition : postPositionsList) {
      for (String regEx : appInfo.getNoteRegExStrings()) {
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(postPosition);
        if (matcher.find()) {
          foundString = matcher.group(0);
          if (Objects.nonNull(foundString) && !foundString.equals("")) {
            noteAsList.add(foundString);
          }
          elementsToRemove.add(foundString);
        }
      }
    }
    for (String string : elementsToRemove) {
      postPositionsList.remove(string);
    }
    for (String postPosition : postPositionsList) {
      if (Objects.nonNull(postPosition) && !postPosition.equals("")) {
        noteAsList.add(postPosition);
      }
    }
  }

  // 前置付随詞群をパースする
  private String parsePreps(String prePositions) {

    // 前置付随詞群を分割
    ArrayList<String> prePositionsList =
        new ArrayList<String>(Arrays.asList(splitNotes(prePositions)));

    // 前置付随詞群をパース
    release = null;
    type = null;
    if (Objects.isNull(noteAsList)) {
      noteAsList = new ArrayList<String>();
    }
    isDoujinshi = false;
    isComic = false;
    isMagazine = false;
    isNovel = false;
    adult = null;
    ArrayList<String> elementsToRemove = new ArrayList<String>();
    if (Objects.nonNull(prePositions)) {
      for (String prePosition : prePositionsList) {
        for (String evRegex : appInfo.getEvRegExStrings()) {
          Pattern pattern = Pattern.compile(evRegex);
          Matcher matcher = pattern.matcher(prePosition);
          if (matcher.find()) {
            elementsToRemove.add(prePosition);
            release = prePosition;
            isDoujinshi = true;
            break;
          }
          if (prePosition.matches(".*?コミック$")) {
            isComic = true;
            if (prePosition.matches(".*成[年人].*")) {
              adult = true;
            }
            elementsToRemove.add(prePosition);
          }
          if (prePosition.contains("同人")) {
            isDoujinshi = true;
            if (prePosition.contains("CG集")) {
              noteAsList.add("CG集");
            }
            elementsToRemove.add(prePosition);
          }
          if (prePosition.matches(".*成[年人].*")) {
            adult = true;
          }
          if (prePosition.contains("一般")) {
            adult = false;
          }
          if (prePosition.contains("雑誌")) {
            isMagazine = true;
            elementsToRemove.add(prePosition);
          }
          if (prePosition.contains("小説")) {
            isNovel = true;
            elementsToRemove.add(prePosition);
          }
        }
      }
      for (String elementToRemove : elementsToRemove) {
        prePositionsList.remove(elementToRemove);
      }
    }

    if (prePositionsList.size() == 0) {
      return null;
    } else {
      return String.join(",", prePositionsList.toArray(new String[prePositionsList.size()]));
    }

  }

  // 備考相当部分の文字列をパースして分割する
  private static String[] splitNotes(String notes) {
    notes = notes.trim();
    notes = notes.replaceAll("[（\\[［【]", "(");
    notes = notes.replaceAll("[）\\]］】]", ")");
    notes = notes.replaceAll("\\)([ 　]+)?\\(", ")(");
    notes = notes.replaceAll("^([ 　]+)?\\(", "");
    notes = notes.replaceAll("\\)([ 　]+)?$", "");
    return notes.split("\\)\\(");
  }

  // タイトルや副題の前後のスペースや波ダッシュを除く
  private static String trim(String string) {
    string = string.trim();
    string = string.replaceFirst("^[～~-―－]", "");
    string = string.replaceFirst("[～~-―－]$", "");
    string = string.replaceAll(" +", " ");
    return string;
  }

  /**
   * 保存されたエントリから容量とファイル数を取得してフィールドにセットする<BR>
   * ファイル名の文字コードはMS932->UTF8の順で試す<BR>
   * それ以外だとエラーメッセージを出す
   */
  public void acquireSizeAndPages() {
    File file = this.path.toFile();
    int newPages;
    double newSize;
    if (file.isFile()) {
      newSize = file.length() / 1024d / 1024d;
      if (file.getName().matches(".+\\.[Zz][Ii][Pp]$")) {
        ZipFile zipFile;
        try {
          zipFile = new ZipFile(file, Charset.forName("MS932"));
          try {
            newPages = (int) zipFile.stream().filter(x -> !x.isDirectory()).count();
          } catch (IllegalArgumentException e) {
            zipFile.close();
            zipFile = new ZipFile(file);
            newPages = (int) zipFile.stream().filter(x -> !x.isDirectory()).count();
          } finally {
            zipFile.close();
          }
        } catch (Exception e) {
          JOptionPane.showMessageDialog(null,
              "zipファイル " + file.getName() + " の内容を読み取れません\n(暗号化されている or 未知の文字コードが使用されている?)", "エラー",
              JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
          return;
        }
      } else {
        newPages = 1;
      }
    } else if (file.isDirectory()) {
      newSize = FileUtils.sizeOfDirectory(file) / 1024d / 1024d;
      try { // パーミッション/アクセス権の問題で内包するファイル数が取得できない場合ここで例外が出る
        newPages = file.listFiles().length;
      } catch (Exception e) {
        System.out.println(file.getAbsolutePath());
        newPages = 0;
      }
    } else {
      JOptionPane.showMessageDialog(null, "Entry.pathの内容にファイルでもディレクトリでもないエントリのパスが入っている", "エラー",
          JOptionPane.ERROR_MESSAGE);
      return;
    }
    this.pages = newPages;
    this.size = newSize;
  }

  /**
   * エントリのデータ実体をコピー/移動する
   *
   * @param destFile コピー/移動先
   * @param changePath Entryインスタンスのpathフィールドを書き換えるか否か
   * @param leftSource 元のデータ実体を残すか否か
   * @throws IOException 移動元にアクセスできなかった場合
   * @throws IllegalArgumentException 元のデータ実体がファイルでもディレクトリでもなかった場合
   */
  public void moveTo(File destFile, Boolean changePath, Boolean leftSource) throws IOException {

    File srcFile = getPath().toFile();

    if (srcFile.isFile()) {
      if (leftSource) {
        FileUtils.copyFile(srcFile, destFile);
      } else {
        FileUtils.moveFile(srcFile, destFile);
      }
    } else if (srcFile.isDirectory()) {
      if (leftSource) {
        FileUtils.copyDirectory(srcFile, destFile);
      } else {
        FileUtils.moveDirectory(srcFile, destFile);
      }
    } else {
      throw new IllegalArgumentException(
          "Entry.moveToメソッドにファイルでもディレクトリでもないFileオブジェクトが渡された(シンボリックリンク?)");
    }

    if (changePath) {
      setPath(destFile.toPath());
    }
  }

  /**
   * Entryクラスのインスタンスからフィールド値を取り出して返す
   *
   * @return
   */
  public Object[] getRowData() {
    String dateString;
    if (Objects.nonNull(date)) {
      dateString = getDate().format(Util.DTF);
    } else {
      dateString = null;
    }
    Object[] row = {getId(), getType(), getAdult(), getCircle(), getAuthor(), getTitle(),
        getSubtitle(), getVolume(), getIssue(), getEntryTitle(), getNote(), getPages(),
        String.format("%.2f", getSize()), getPath().toString(), dateString, getOriginal(),
        getRelease()};
    return row;
  }

  // ファイル名を生成
  public String generateNameToSave() {
    StringBuilder sb = new StringBuilder();
    if (Objects.isNull(type)) {
      type = "unknown";
    }
    switch (type) {
      case "doujinshi":
        if (Objects.nonNull(release)) {
          sb.append("(" + release + ")");
        }
        sb.append("(同人誌)");
        sb.append(" [");
        if (Objects.nonNull(circle)) {
          sb.append(circle);
          if (Objects.nonNull(author)) {
            sb.append(" (" + author + ")");
          }
          sb.append("] ");
        } else if (Objects.nonNull(author)) {
          sb.append(author + "] ");
        } else {
          sb.append("サークル不詳] ");
        }
        break;
      case "comic":
        if (Objects.nonNull(adult)) {
          if (adult) {
            sb.append("(成年コミック)");
          } else {
            sb.append("(一般コミック)");
          }
        } else {
          sb.append("(コミック)");
        }
        sb.append(" [");
        if (Objects.nonNull(author)) {
          sb.append(author + "] ");
        } else {
          sb.append("著者不詳] ");
        }
        break;
      case "magazine":
        if (Objects.nonNull(adult)) {
          if (adult) {
            sb.append("(成年コミック) [雑誌] ");
          } else {
            sb.append("(一般コミック) [雑誌] ");
          }
        } else {
          sb.append("(コミック) [雑誌] ");
        }
        break;
      case "novel":
        if (Objects.nonNull(adult)) {
          if (adult) {
            sb.append("(成年小説) [");
          } else {
            sb.append("(一般小説) [");
          }
        }
        if (Objects.nonNull(author)) {
          sb.append(author + "] ");
        } else {
          sb.append("著者不詳] ");
        }
        break;
      default:
        sb.append("(種別不詳) [");
        if (Objects.nonNull(author)) {
          sb.append(author + "] ");
        } else {
          sb.append("著者不詳] ");
        }
    }

    sb.append(title);

    if (Objects.nonNull(volume)) {
      sb.append(" " + volume);
    }

    if (Objects.nonNull(subtitle)) {
      sb.append(" " + subtitle);
    }

    if (Objects.nonNull(issue)) {
      sb.append(" " + issue);
    }

    if (Objects.nonNull(note)) {
      sb.append(" ");
      ArrayList<String> noteAsList = new ArrayList<String>(Arrays.asList(note.split(",")));
      if (Objects.nonNull(original)) {
        noteAsList.add(original);
      }
      for (String element : noteAsList) {
        sb.append("(" + element + ")");
      }
    } else if (Objects.nonNull(original)) {
      sb.append(" (" + original + ")");
    }

    // ファイル名に使えない文字を全角に置換
    String filename = sb.toString().replaceAll("\\\\", "＼").replaceAll("/", "／")
        .replaceAll(":", "：").replaceAll("\\?", "？").replaceAll("\"", "”").replaceAll("<", "＜")
        .replaceAll(">", "＞").replaceAll("\\|", "｜").replaceAll("\\*", "＊");

    // Windowsではファイル名末尾のピリオドやスペースが許されないので除去
    filename = filename.trim();
    filename = filename.replaceAll("\\.+$", "");

    return filename;
  }

  /**
   * テーブルで表示するためのエントリタイトル文字列を生成して返す
   *
   * @return
   */
  public String getEntryTitle() {
    ArrayList<String> list = new ArrayList<String>();
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    if (Objects.isNull(type)) {
      if (Objects.nonNull(circle)) {
        sb.append(circle);
        if (Objects.nonNull(author)) {
          sb.append("(");
          sb.append(author);
          sb.append(")]");
        } else {
          sb.append("] ");
        }
      } else if (Objects.nonNull(author)) {
        sb.append(author);
        sb.append("]");
      } else {
        sb.append("サークル著者不詳] ");
      }
    } else {
      switch (type) {
        case "magazine":
          sb.append("雑誌]");
          break;
        case "doujinshi":
          if (Objects.nonNull(circle)) {
            sb.append(circle);
            if (Objects.nonNull(author)) {
              sb.append("(");
              sb.append(author);
              sb.append(")]");
            } else {
              sb.append("] ");
            }
          } else if (Objects.nonNull(author)) {
            sb.append(author);
            sb.append("]");
          } else {
            sb.append("サークル著者不詳] ");
          }
          break;
        default:
          if (Objects.nonNull(author)) {
            sb.append(author);
            sb.append("]");
          } else {
            sb.append("著者不詳] ");
          }
      }
    }
    list.add(sb.toString());
    list.add(title);
    if (Objects.nonNull(volume)) {
      list.add(volume);
    }
    if (Objects.nonNull(subtitle)) {
      list.add(subtitle);
    }
    if (Objects.nonNull(issue)) {
      list.add(issue);
    }
    if (Objects.nonNull(note)) {
      list.add("(" + String.join(")(", note.split(",")) + ")");
    }
    String outString = String.join(" ", list.toArray(new String[list.size()]));
    return outString;
  }

  /**
   * インポートに必要な情報が揃っているか否かを示す真偽値を返す
   *
   * @return
   */
  public boolean isReady() {
    boolean result;
    if (Objects.isNull(type) || type.equals("unknown") || title.equals("")) {
      result = false;
    } else if (Objects.isNull(title) || title.equals("")) {
      result = false;
    } else {
      result = true;
      switch (type) {
        case "doujinshi":
          if (Objects.isNull(circle)) {
            result = false;
          }
          break;
        case "comic":
          if (Objects.isNull(author)) {
            result = false;
          }
          break;
        case "magazine":
          if (Objects.isNull(volume) && Objects.isNull(issue)) {
            result = false;
          }
          break;
      }
    }
    return result;
  }

  public Integer getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public Boolean getAdult() {
    return adult;
  }

  public String getCircle() {
    return circle;
  }

  public String getAuthor() {
    return author;
  }

  public String getTitle() {
    return title;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public String getVolume() {
    return volume;
  }

  public String getIssue() {
    return issue;
  }

  public String getNote() {
    return note;
  }

  public Integer getPages() {
    return pages;
  }

  public Double getSize() {
    return size;
  }

  public Path getPath() {
    return path;
  }

  public OffsetDateTime getDate() {
    return date;
  }

  public String getOriginal() {
    return original;
  }

  public String getRelease() {
    return release;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setAdult(Boolean adult) {
    this.adult = adult;
  }

  public void setCircle(String circle) {
    this.circle = circle;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setSubtitle(String subtitle) {
    this.subtitle = subtitle;
  }

  public void setVolume(String volume) {
    this.volume = volume;
  }

  public void setIssue(String issue) {
    this.issue = issue;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public void setPages(Integer pages) {
    this.pages = pages;
  }

  public void setSize(Double size) {
    this.size = size;
  }

  public void setPath(Path path) {
    this.path = path;
  }

  public void setDate(OffsetDateTime date) {
    this.date = date;
  }

  public void setOriginal(String original) {
    this.original = original;
  }

  public void setRelease(String release) {
    this.release = release;
  }

}
