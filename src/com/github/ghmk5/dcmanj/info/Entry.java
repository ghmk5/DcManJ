package com.github.ghmk5.dcmanj.info;

import java.io.File;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.github.ghmk5.dcmanj.util.Util;

public class Entry {

  Integer id;
  String type;
  Boolean adult;
  String circle;
  String author;
  String title;
  String subtitle;
  String volume;
  String issue;
  String note;
  Integer pages;
  Double size;
  Path path;
  OffsetDateTime date;
  String original;
  String release;

  // noteを生成するために使うリスト
  ArrayList<String> noteAsList;

  // タイプの判断材料を入れておくための真偽値
  Boolean isDoujinshi;
  Boolean isComic;
  Boolean isMagazine;
  Boolean isNovel;

  // 前置・後置タグ群のパースに使う正規表現 最終的には設定パネルから入力できるようにする
  String[] eventKeys = {"^C\\d{2,3}$", "サンクリ", "酒保", "コミティア", "COMITIA", "紅楼夢", "例大祭", "神戸かわさき",
      "砲雷", "夜戦に突入す", "もう何も恐くない", "みみけっと", "ふたけっと", "ぱんっあ☆ふぉー", "とら祭り", "こみトレ", "^SC\\d{2}", "Cレヴォ",
      "C[Oo][Mm][Ii][Cc]1☆"};
  String[] noteKeys = {"DL版", "CG集", "別スキャン", "^+", "補完", "雑誌寄せ集め", "修正版"};

  public static void main(String[] args) {


    File dir = new File("D:\\docs\\mk5\\Downloads\\nty");
    for (File file : dir.listFiles()) {
      if (!file.isFile()) {
        continue;
      }
      String fileName = file.getName();
      try {
        Entry entry = new Entry(fileName);

        System.out.println(fileName);
        String[] outAry = {entry.type, entry.circle, entry.author, entry.title, entry.volume,
            entry.subtitle, entry.issue, entry.note, entry.release};
        String[] tagAry =
            {"type", "circle", "author", "title", "volume", "subtitle", "issue", "note", "release"};

        for (int i = 0; i < outAry.length; i++) {
          System.out.println("  " + tagAry[i] + ": " + outAry[i]);
        }

        System.out.println("  nameToSave: " + entry.generateNameToSave());

        System.out.println("");

        Boolean[] booleans =
            {entry.adult, entry.isDoujinshi, entry.isComic, entry.isMagazine, entry.isNovel};
        String[] tagAry2 = {"adult", "isDoujinshi", "isComic", "isMagazine", "isNovel"};
        for (int i = 0; i < booleans.length; i++) {
          System.out.println("  " + tagAry2[i] + ": " + String.valueOf(booleans[i]));
        }

        System.out.println("");

      } catch (Exception e) {
        System.out.println(fileName);
        System.out.println("  failed to parse");
        e.printStackTrace();
      }
    }

  }

  public Entry(String fileName) {

    adult = false;
    isComic = false;
    isDoujinshi = false;
    isMagazine = false;
    isNovel = false;

    // 拡張子があれば除く
    String name = fileName.replaceFirst("\\.[a-zA-Z0-9]{1,4}$", "");

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
      parsePreps(prePositions);
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
      title = title.replace(issue, "");
      title = trim(title);
      isMagazine = true;
    } else if (matcher2.find()) {
      issue = matcher2.group(1) + "-" + String.format("%02d", Integer.valueOf(matcher2.group(2)));
      title = title.replace(issue, "");
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
    noteAsList = new ArrayList<String>();
    pattern = Pattern.compile(" \\+ [^ ]+カード");
    matcher = pattern.matcher(title);
    pattern2 = Pattern.compile(" \\+ [^ ]+リーフレット");
    matcher2 = pattern2.matcher(title);
    String foundString;
    if (matcher.find()) {
      foundString = matcher.group(0);
      noteAsList.add(trim(foundString));
      title = title.replace(foundString, "");
      title = trim(title);
    } else if (matcher2.find()) {
      foundString = matcher2.group(0);
      noteAsList.add(trim(foundString));
      title = title.replace(foundString, "");
      title = trim(title);
    }

    // 後置付随詞群をパース
    if (Objects.nonNull(postPositions)) {
      parsePostps(postPositions);
    }

    // 備考のリストをStringにしてフィールドに代入
    if (noteAsList.size() > 0) {
      note = String.join(",", noteAsList.toArray(new String[noteAsList.size()]));
    }

    // 可能なら種別をセット
    setType();
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
        if (adult) {
          sb.append("(成年コミック)");
        } else {
          sb.append("(一般コミック)");
        }
        sb.append(" [");
        if (Objects.nonNull(author)) {
          sb.append(author + "] ");
        } else {
          sb.append("著者不詳] ");
        }
        break;
      case "magazine":
        if (adult) {
          sb.append("(成年コミック) [雑誌] ");
        } else {
          sb.append("(一般コミック) [雑誌] ");
        }
        break;
      case "novel":
        sb.append("(小説)");
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

    if (Objects.nonNull(note)) {
      sb.append(" ");
      ArrayList<String> noteAsList = new ArrayList<String>(Arrays.asList(note.split(",")));
      for (String element : noteAsList) {
        sb.append("(" + element + ")");
      }
    }

    return sb.toString();
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
    String foundString;
    for (String postPosition : postPositionsList) {
      for (String regEx : noteKeys) {
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
  private void parsePreps(String prePositions) {

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
    adult = false;
    ArrayList<String> elementsToRemove = new ArrayList<String>();
    if (Objects.nonNull(prePositions)) {
      for (String prePosition : prePositionsList) {
        for (String evRegex : eventKeys) {
          Pattern pattern = Pattern.compile(evRegex);
          Matcher matcher = pattern.matcher(prePosition);
          if (matcher.find()) {
            elementsToRemove.add(prePosition);
            release = prePosition;
            isDoujinshi = true;
            break;
          }
          if (prePosition.contains("コミック")) {
            isComic = true;
            if (prePosition.matches(".*成[年人].*")) {
              adult = true;
            }
            elementsToRemove.add(prePosition);
          }
          if (prePosition.contains("同人")) {
            isDoujinshi = true;
            adult = true;
            if (prePosition.contains("CG集")) {
              noteAsList.add("CG集");
            }
            elementsToRemove.add(prePosition);
          }
          if (prePosition.contains("雑誌")) {
            isMagazine = true;
            elementsToRemove.add(prePosition);
          }
        }
      }
      for (String elementToRemove : elementsToRemove) {
        prePositionsList.remove(elementToRemove);
      }
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
   * データベースの戻り値からEntryのインスタンスを生成する データベースに要求するフィールドは 'rowid, *'でなければならない
   *
   * @param resultSet
   * @throws SQLException
   */
  public Entry(ResultSet resultSet) throws SQLException {

    id = resultSet.getInt("rowid");
    type = resultSet.getString("type");
    adult = Boolean.valueOf(resultSet.getString("adult"));
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
        getSubtitle(), getVolume(), getIssue(), getEntryTitle(), getNote(), getPages(), getSize(),
        getPath().toString(), dateString, getOriginal(), getRelease()};
    return row;
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
    list.add(sb.toString());
    list.add(title);
    if (Objects.nonNull(volume)) {
      list.add(volume);
    }
    if (Objects.nonNull(subtitle)) {
    }
    if (Objects.nonNull(issue)) {
      list.add(issue);
    }
    String outString = String.join(" ", list.toArray(new String[list.size()]));
    return outString;
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
