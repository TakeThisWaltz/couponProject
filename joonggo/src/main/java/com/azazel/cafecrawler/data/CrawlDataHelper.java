package com.azazel.cafecrawler.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.azazel.cafecrawler.CrawlConstants;
import com.azazel.framework.AzApplication;
import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.LOG;

import java.util.ArrayList;
import java.util.List;

public class CrawlDataHelper extends SQLiteOpenHelper {
    private static final String TAG = "CrawlDataHelper";

    public static final String CategoryTable = "tb_category";
    public static final String SearchListTable = "tb_search";
    public static final String ScrapTable = "tb_scrap";

    private static final String DB = "cafe_crawl.db";

    private static final int VERSION = 5;

    private static CrawlDataHelper INSTANCE;

    private SQLiteDatabase mDb;


    public static synchronized CrawlDataHelper getInstance() {
        if (INSTANCE == null)
            INSTANCE = new CrawlDataHelper(AzApplication.APP_CONTEXT);

        return INSTANCE;
    }

    public CrawlDataHelper(Context context) {
        super(context, DB, null, VERSION);
        mDb = getWritableDatabase();
    }

    public void beginTransaction() {
        mDb.beginTransaction();
    }

    public void endTransaction(boolean isCommit) {
        if (isCommit)
            mDb.setTransactionSuccessful();
        mDb.endTransaction();
    }


    public long insertCategory(String categoryId, String title) {
        LOG.i(TAG, "insertCategory : " + categoryId + ", title : " + title);

        ContentValues cv = new ContentValues();
        cv.put("category_id", categoryId);
        cv.put("title", title);

        long id = mDb.insert(CategoryTable, null, cv);
        return id;
    }

    public void resetCategoryList() {
        LOG.i(TAG, "resetCategoryList ");
        mDb.delete(CategoryTable, null, null);
    }

    public Search insertSearchList(String categoryId, String categoryTitle, String keyword, long lastestArticleId) {
        LOG.i(TAG, "insertSearchList : " + categoryId + ", keyword : " + keyword);

        ContentValues cv = new ContentValues();
        cv.put("category_id", categoryId);
        cv.put("keyword", keyword);
        cv.put("new_article", lastestArticleId);

        long id = mDb.insert(SearchListTable, null, cv);

        Search search = new Search(id, categoryId, categoryTitle, keyword, 0, 0, true);
        search.newArticle = lastestArticleId;


        return search;
    }

    public long insertScrap(Article article) {
        LOG.i(TAG, "insertOrUpdateScrap : " + article);
        long id = 0;

        ContentValues cv = new ContentValues();
        cv.put("article_id", article.articleId);
        cv.put("title", article.title);
        cv.put("writer", article.writer);
        cv.put("thumb", article.thumb);
        cv.put("info", article.date);
        cv.put("type", article.type);

        Cursor cs = mDb.query(ScrapTable, new String[]{"_id", "type"}, "article_id = " + article.articleId, null, null, null, null);
        if (cs.moveToNext() && cs.getInt(1) <= article.type) {
            mDb.update(ScrapTable, cv, "article_id = " + article.articleId, null);
            id = cs.getLong(0);
        } else {
            if (article.type == CrawlConstants.ArticleType.MY_ARTICLE ||
                    article.type == CrawlConstants.ArticleType.MY_COMMENT)
                cv.put("alarm", 0);

            id = mDb.insert(ScrapTable, null, cv);
        }
        cs.close();

        return id;
    }

    public long updateScrap(Article article) {
        LOG.i(TAG, "updateScrap : " + article.articleId);

        ContentValues cv = new ContentValues();
        if (article.title != null)
            cv.put("title", article.title);
        if (article.writer != null)
            cv.put("writer", article.writer);
        if (article.thumb != null)
            cv.put("thumb", article.thumb);
        if (article.date != null)
            cv.put("info", article.date);
        cv.put("comment", article.comment);
        cv.put("is_read", article.isRead);
        cv.put("type", article.type);
        cv.put("alarm", article.alarm);

        long id = mDb.update(ScrapTable, cv, "article_id = " + article.articleId, null);

        return id;
    }

    public long setIsReadScrap(Article article) {
        LOG.i(TAG, "setIsReadScrap : " + article.articleId + ", isRead : " + article.isRead);

        ContentValues cv = new ContentValues();
        cv.put("is_read", article.isRead);
        if (article.comment > 0)
            cv.put("comment", article.comment);

        long id = mDb.update(ScrapTable, cv, "article_id = " + article.articleId, null);

        return id;
    }

    public Search getSearch(long id) {
        Cursor cs = mDb.rawQuery("select his._id, cat.category_id, cat.title, his.keyword, his.old_article, his.new_article, his.alarm from " + CategoryTable + " cat, " + SearchListTable + " his "
                + " where cat.category_id = his.category_id and his._id = " + id, null);
        Search result = null;
        if (cs.moveToNext())
            result = new Search(cs.getLong(0), cs.getString(1), cs.getString(2), cs.getString(3), cs.getLong(4), cs.getLong(5), (cs.getInt(6) == 1 ? true : false));
        cs.close();
        return result;
    }

    public Article getArticle(long articleId) {
        Cursor cs = mDb.query(ScrapTable, null, "article_id = " + articleId, null, null, null, null);
        Article result = null;
        if (cs.moveToNext()) {
            result = new Article(cs.getLong(1), cs.getString(2), cs.getString(3), cs.getString(4), cs.getString(5), cs.getInt(6), (cs.getInt(7) == 1 ? true : false), cs.getInt(8), (cs.getInt(9) == 1 ? true : false));
            result.autoReUpload = cs.getLong(10);
        }
        cs.close();

        LOG.d(TAG, "getArticle q : " + articleId + ", result : " + result);
        return result;
    }

    public void initMyArticles(List<Article> list) {
        if (list != null && list.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Article item : list) {
                sb.append(", " + item.articleId);
            }
            int cnt = mDb.delete(ScrapTable, "type in (" + CrawlConstants.ArticleType.MY_ARTICLE + ", " + CrawlConstants.ArticleType.MY_COMMENT + ") and article_id not in (" + sb.toString().substring(1) + ")", null);
            LOG.i(TAG, "delete missing my articles : " + cnt);
        }
    }

    public List<Article> getScrapList(int type, boolean isAlarmOnly) {
        Cursor cs = mDb.query(ScrapTable, null
                , "type = " + type + (isAlarmOnly ? " and alarm = 1" : ""), null, null, null, null);
        List<Article> list = new ArrayList<Article>();
        while (cs.moveToNext()) {
            Article item = new Article(cs.getLong(1), cs.getString(2), cs.getString(3), cs.getString(4), cs.getString(5), cs.getInt(6), (cs.getInt(7) == 1 ? true : false), cs.getInt(8), (cs.getInt(9) == 1 ? true : false));
            item.autoReUpload = cs.getLong(10);
            item.isDeleted = (cs.getInt(11) == 1 ? true : false);
            list.add(item);
        }
        cs.close();
        return list;
    }

    public List<Article> getToReUploadList(long condition) {
        LOG.i(TAG, "getToReUploadList : " + AzUtil.getDateStringFromMils(AzApplication.APP_CONTEXT, condition, true));
        Cursor cs = mDb.query(ScrapTable, null
                , "type = " + CrawlConstants.ArticleType.MY_ARTICLE + " and re_upload > 0 and ( re_upload < " + condition + " )", null, null, null, null);
        List<Article> list = new ArrayList<Article>();
        while (cs.moveToNext()) {
            Article item = new Article(cs.getLong(1), cs.getString(2), cs.getString(3), cs.getString(4), cs.getString(5), cs.getInt(6), (cs.getInt(7) == 1 ? true : false), cs.getInt(8), (cs.getInt(9) == 1 ? true : false));
            item.autoReUpload = cs.getLong(10);
            list.add(item);
        }
        cs.close();
        return list;
    }

    public List<Category> getCategoryList() {
        Cursor cs = mDb.query(CategoryTable, null, null, null, null, null, null);
        List<Category> list = new ArrayList<Category>();
        while (cs.moveToNext())
            list.add(new Category(cs.getString(1), cs.getString(2)));
        cs.close();
        return list;
    }

    public List<Category> getCategoryTop(int size) {
        LOG.d(TAG, "getCategoryTop");
        Cursor cs = mDb.query(CategoryTable, null, "hits > 0", null, null, null, "hits desc");
        List<Category> list = new ArrayList<Category>();
        while (cs.moveToNext()) {
            LOG.d(TAG, "cate hits - " + cs.getString(2) + ", " + cs.getInt(3));
            list.add(new Category(cs.getString(1), "  "+cs.getString(2)));
            if(list.size() == size)
                break;
        }
        cs.close();

        if(list.size() > 0){
            list.add(0, new Category("", "┏━자주쓰는 카테고리━┓"));
            list.add(new Category("", "┗━자주쓰는 카테고리━┛"));
        }

        return list;
    }

    public void increaseCategoryHit(String category){
        LOG.d(TAG, "increaseCategoryHit - " + category);

        if(!TextUtils.isEmpty(category)){
            mDb.execSQL("update " + CategoryTable + " set hits = hits + 1 where category_id = '" + category + "'");
            //AzUtil.printCursor(TAG, mDb.query(CategoryTable, null, "category_id = '"+category+"'", null, null, null, null), false);
        }
    }

    public long getBeforeMaxArticleId(long id) {
        Cursor cs = mDb.query(SearchListTable, new String[]{"new_article"}, "_id = " + id, null, null, null, null);
        long result = -1;
        if (cs.moveToNext())
            result = cs.getLong(0);
        cs.close();
        return result;
    }

    public ArrayList<Search> getObserveList(boolean isAlarmOnly) {
        Cursor cs = mDb.rawQuery("select his._id, cat.category_id, cat.title, his.keyword, his.old_article, his.new_article, his.alarm from " + CategoryTable + " cat, " + SearchListTable + " his "
                + " where cat.category_id = his.category_id " + (isAlarmOnly ? " and his.alarm = 1" : "") + " order by his._id", null);
        ArrayList<Search> list = new ArrayList<Search>();
        while (cs.moveToNext())
            list.add(new Search(cs.getLong(0), cs.getString(1), cs.getString(2), cs.getString(3), cs.getLong(4), cs.getLong(5), (cs.getInt(6) == 1 ? true : false)));
        cs.close();
        return list;
    }

    public int updateNewArticle(long searchId, long oldArticle, long newArticle) {
        LOG.i(TAG, "updateNewArticle - searchId : " + searchId + ", oldArticle : " + oldArticle + ", newArticle : " + newArticle);
        ContentValues cv = new ContentValues();
        if (oldArticle > 0)
            cv.put("old_article", oldArticle);
        if (newArticle > 0)
            cv.put("new_article", newArticle);
        return mDb.update(SearchListTable, cv, " _id = " + searchId, null);
    }

    public int updateSearchAlarm(long searchId, boolean alarm) {
        LOG.i(TAG, "updateSearchAlarm - searchId : " + searchId + ", alarm : " + alarm);
        ContentValues cv = new ContentValues();
        cv.put("alarm", alarm ? 1 : 0);
        return mDb.update(SearchListTable, cv, " _id = " + searchId, null);
    }

    public int updateCommentAlarm(long articleId, boolean alarm) {
        LOG.i(TAG, "updateCommentAlarm - articleId : " + articleId + ", alarm : " + alarm);
        ContentValues cv = new ContentValues();
        cv.put("alarm", alarm ? 1 : 0);
        return mDb.update(ScrapTable, cv, " article_id = " + articleId, null);
    }

    public int updateAutoReUpload(long articleId, boolean reUpload) {
        LOG.i(TAG, "updateAutoReUpload - articleId : " + articleId + ", reUpload : " + reUpload);
        ContentValues cv = new ContentValues();
        cv.put("re_upload", reUpload ? System.currentTimeMillis() : 0);
        return mDb.update(ScrapTable, cv, " article_id = " + articleId, null);
    }

    public int updateAutoReUploadFailed(long articleId) {
        LOG.i(TAG, "updateAutoReUploadFailed - articleId : " + articleId);
        ContentValues cv = new ContentValues();
        cv.put("re_upload", -1);
        return mDb.update(ScrapTable, cv, " article_id = " + articleId, null);
    }

    public int updateAutoReUploadProcessing(long articleId) {
        LOG.i(TAG, "updateAutoReUploadFailed - articleId : " + articleId);
        ContentValues cv = new ContentValues();
        cv.put("re_upload", -2);
        return mDb.update(ScrapTable, cv, " article_id = " + articleId, null);
    }

    public int setScrapDeleted(long articleId) {
        LOG.i(TAG, "setScrapDeleted - articleId : " + articleId);
        ContentValues cv = new ContentValues();
        cv.put("alarm", 0);
        cv.put("deleted", 1);
        return mDb.update(ScrapTable, cv, " article_id = " + articleId, null);
    }

    public int deleteKeyword(long searchId) {
        LOG.i(TAG, "deleteKeyword - searchId : " + searchId);
        return mDb.delete(SearchListTable, "_id = " + searchId, null);
    }

    public int deleteScrap(long articleId) {
        LOG.i(TAG, "deleteScrap - articleId : " + articleId);
        return mDb.delete(ScrapTable, "article_id = " + articleId, null);
    }

    public boolean isObserving() {
        return isObservingKeyword() || isObservingScrap() || isObservingMine();
    }

    public boolean isObservingKeyword() {
        boolean result = false;
        Cursor cs = mDb.query(SearchListTable, null, "alarm = 1", null, null, null, null);
        result = cs.getCount() > 0;
        cs.close();
        LOG.i(TAG, "isObservingScrap : " + result);
        return result;
    }

    public boolean isObservingScrap() {
        boolean result = false;
        Cursor cs = mDb.query(ScrapTable, null, "alarm = 1 and deleted = 0 and type = " + CrawlConstants.ArticleType.SCRAP, null, null, null, null);
        result = cs.getCount() > 0;
        cs.close();
        LOG.i(TAG, "isObservingScrap : " + result);
        return result;
    }

    public boolean isObservingMine() {
        boolean result = false;
        Cursor cs = mDb.query(ScrapTable, null, "alarm = 1 and type in (" + CrawlConstants.ArticleType.MY_ARTICLE + ", " + CrawlConstants.ArticleType.MY_COMMENT + ")", null, null, null, null);
        result = cs.getCount() > 0;
        cs.close();
        LOG.i(TAG, "isObservingMine : " + result);
        return result;
    }

    public boolean isReUp() {
        boolean result = false;
        Cursor cs = mDb.query(ScrapTable, null, "re_upload > 0", null, null, null, null);
        result = cs.getCount() > 0;
        cs.close();
        LOG.i(TAG, "isReUp : " + result);
        return result;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        LOG.i(TAG, "onCreate");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + CategoryTable + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "category_id TEXT, "
                + "title TEXT, "
                + "hits INTEGER );");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + SearchListTable + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "category_id TEXT, "
                + "keyword TEXT, "
                + "old_article INTEGER, "
                + "new_article INTEGER, "
                + "alarm INTEGER DEFAULT 1);");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + ScrapTable + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "article_id INTEGER, "
                + "title TEXT, "
                + "writer TEXT, "
                + "thumb TEXT, "
                + "info TEXT, "
                + "comment INTEGER, "
                + "is_read INTEGER, "
                + "type INTEGER, "
                + "alarm INTEGER DEFAULT 1,"
                + "re_upload INTEGER DEFAULT 0,"
                + "deleted INTEGER DEFAULT 0);");

    }



    public class Category {
        public String categoryId;
        public String title;

        Category(String categoryId, String title) {
            this.categoryId = categoryId;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof String)
                return categoryId.equals(o);
            else {
                Category other = (Category) o;
                return (categoryId).equals(other.categoryId);
            }
        }

    }

    public class Search {
        public long id;
        public String categoryId;
        public String categoryTitle;
        public String keyword;
        public long oldArticle;
        public long newArticle;
        public boolean alarm;

        Search(String categoryId, String keyword) {
            this.categoryId = categoryId;
            this.keyword = keyword;
        }

        Search(long id, String categoryId, String categoryTitle, String keyword, long oldArticle, long newArticle, boolean alarm) {
            this.id = id;
            this.categoryId = categoryId;
            this.categoryTitle = categoryTitle;
            this.keyword = keyword;
            this.oldArticle = oldArticle;
            this.newArticle = newArticle;
            this.alarm = alarm;
        }

        @Override
        public String toString() {
            return (keyword);
        }
    }

    public static class Article implements Comparable {
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







    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LOG.f(TAG, "onUpgrade - old : " + oldVersion + ", new : " + newVersion);
        if (oldVersion == 1) {
            LOG.f(TAG, "DB Upgrade : alter scrap table");
            try {

                db.execSQL("ALTER TABLE " + ScrapTable
                        + " add deleted INTEGER default 0");


                db.setTransactionSuccessful();
                LOG.i(TAG, "upgrade finish..");
            } catch (Exception e) {
                LOG.e(TAG, "onUpgrade err", e);
            } finally {
                db.endTransaction();
            }
        }

        if (oldVersion < 3) {
            LOG.f(TAG, "DB Upgrade : clear category, scrap");
            db.delete(CategoryTable, null, null);
            db.delete(ScrapTable, "type = " + CrawlConstants.ArticleType.MY_ARTICLE + " or type = " + CrawlConstants.ArticleType.MY_COMMENT, null);
        }

        if (oldVersion < 4) {
            LOG.f(TAG, "DB Upgrade : clear category");
            db.delete(CategoryTable, null, null);
        }

        if (oldVersion < 5) {
            LOG.f(TAG, "DB Upgrade : add hit column on category..");

            db.execSQL("ALTER TABLE " + CategoryTable
                    + " add hits INTEGER default 0");

            db.execSQL("update " + CategoryTable + " set hits = (select count(*) as cnt from "+SearchListTable+" where category_id = "+CategoryTable+".category_id)");
        }
    }

}