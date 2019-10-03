package com.github.ghmk5.dcmanj.info;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Objects;
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

}
