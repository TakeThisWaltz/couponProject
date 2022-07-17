package kr.azazel.barcode.joonggo;

public class Article implements Comparable {
    // public long id = -1;
    public long articleId;
    public String title;
    public String writer;
    public String thumb;
    public String date;
    public int comment;
    public boolean isRead;
    public int type;
    public boolean alarm;
    public long autoReUpload;
    public boolean isDeleted = false;

    public String price;

    public Article(long articleId, String title, int type) {
        this.articleId = articleId;
        this.title = title;
        this.type = type;
    }

    public Article(long articleId, String title, String writer, String thumb, String date, int type) {
        this.articleId = articleId;
        this.title = title;
        this.writer = writer;
        this.thumb = thumb;
        this.date = date;
        this.type = type;
    }

    public Article(long articleId, String title, String writer, String thumb, String date, int comment, boolean isRead, int type, boolean alarm) {
        //this.id = id;
        this.articleId = articleId;
        this.title = title;
        this.writer = writer;
        this.thumb = thumb;
        this.date = date;
        this.comment = comment;
        this.isRead = isRead;
        this.type = type;
        this.alarm = alarm;
    }

    @Override
    public boolean equals(Object o) {
        Article other = (Article) o;
        return other.articleId == this.articleId;//(title + writer).equals(other.title + other.writer);
    }

    @Override
    public int compareTo(Object o) {
        long cur = articleId;
        long other = ((Article) o).articleId;
        return (cur < other ? 1 : (cur == other ? 0 : -1));
    }

    @Override
    public String toString() {
        return "[Article : " + articleId + "] - title : " + title + ", writer : " + writer + ", date : " + date + ", comment : " + comment + ", thumb : " + thumb + ", reup : " + autoReUpload;
    }

}

