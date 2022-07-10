package com.azazel.cafecrawler;

public class CrawlConstants {

    private static final String CAFE_ID = "10050146";

    public static final long DEFAULT_NETWORK_TIMEOUT = 60000;

    public static final int THREAD_ASYNC_LIMIT = 3;
    public static final int OBSERVING_INTERVAL = 3 * 60 * 1000; // 3 MIN
    public static final int RE_UPLOAD_INTERVAL = 60 * 60 * 1000; // 1 HOUR

    public static final long[] VIBRATE = {100, 1000};


    public interface Event{
        public static final int EXIT = 1;
        public static final int INIT_START = 2;
        public static final int REQUEST_LOGIN = 3;
        public static final int LOGIN_COMPLETED = 4;
        public static final int LOGIN_FAILED = 5;
        public static final int SCRAP_CHANGED = 6;
        public static final int MINE_LOADED = 7;
        public static final int TRY_EXIT = 8;
        public static final int TIME_OUT = 9;
        public static final int REFRESH_MY_ARTICLE = 10;
    }

    public interface ArticleType{
        public static final int SEARCH_RESULT = 1;
        public static final int SCRAP = 2;
        public static final int MY_ARTICLE = 3;
        public static final int MY_COMMENT = 4;

    }

    public interface Urls{
        public static final String GET_CATEGORY = "http://m.cafe.naver.com/MenuListAjax.nhn?search.clubid="+CAFE_ID+"&search.pagingType=more&search.page=%d";
//        public static final String ALL_LIST = "http://m.cafe.naver.com/ArticleAllList.nhn?search.boardtype=L&search.questionTab=A&search.clubid="+CAFE_ID+"&search.page=%d";
        public static final String ALL_LIST = "https://apis.naver.com/cafe-web/cafe2/ArticleList.json?search.clubid="+CAFE_ID+"&search.queryType=lastArticle&search.page=%d&search.perPage=100";
        //        public static final String SEARCH = "http://m.cafe.naver.com/ArticleSearchList.nhn?search.clubid="+CAFE_ID+"&search.sortBy=date&search.searchBy=0&search.menuid=%s&search.query=%s&userDisplay=50&search.page=%d";
        public static final String SEARCH = "https://apis.naver.com/cafe-web/cafe-mobile/CafeMobileWebArticleSearchListV3?cafeId="+CAFE_ID+"&menuId=%s&query=%s&searchBy=0&sortBy=date&page=1&perPage=50&adUnit=MW_CAFE_BOARD&ad=true";
        public static final String LOGIN_CHECK = "https://m.cafe.naver.com/cafe-home/cafes/join";
        public static final String LOGIN = "https://nid.naver.com/nidlogin.login?svctype=262144&url=https%3A%2F%2Fm.cafe.naver.com%2Fcafe-home%2Fcafes%2Fjoin";

//        public static final String DETAIL_VIEW = "http://m.cafe.naver.com/ArticleRead.nhn?clubid="+CAFE_ID+"&articleid=%s";
public static final String DETAIL_VIEW = "https://m.cafe.naver.com/ca-fe/web/cafes/"+CAFE_ID+"/articles/%s?fromList=true";
        //public static final String MY_ARTICLE = "http://cafe.naver.com/CafeMemberNetworkArticleList.nhn?clubid="+CAFE_ID+"&search.clubid="+CAFE_ID+"&search.writerid=%s&search.page=%d";
//        public static final String MY_ARTICLE = "https://m.cafe.naver.com/CafeMemberArticleList.nhn?search.clubid="+CAFE_ID+"&search.writerid=%s&search.page=%d&search.perPage=100";
        public static final String MY_ARTICLE = "https://apis.naver.com/cafe-web/cafe-mobile/CafeMemberNetworkArticleList?search.cafeId="+CAFE_ID+"&search.writerid=%s&search.perPage=100&search.page=%d&requestFrom=B";
        //http://cafe.naver.com/CafeMemberNetworkReplyList.nhn?clubid=10050146&search.clubid=10050146&search.query=risma825&search.page=1
//        public static final String MY_COMMENT = "http://cafe.naver.com/CafeMemberNetworkReplyList.nhn?clubid="+CAFE_ID+"&search.clubid="+CAFE_ID+"&search.query=%s&search.page=%d";
        public static final String MY_COMMENT = "https://apis.naver.com/cafe-web/cafe-mobile/CafeMemberProfileCommentList?cafeId="+CAFE_ID+"&memberKey=jLZSNOxIdXqkm3wbhIearQ&perPage=40&page=1&requestFrom=B";
        //http://cafe.naver.com/CafeMemberNetworkCommentList.nhn?clubid=10050146&search.clubid=10050146&search.memberid=risma825&search.query=risma825&search.nickname=&search.writerid=risma825
        public static final String MY_COMMENT2 = "http://cafe.naver.com/CafeMemberNetworkCommentList.nhn?clubid="+CAFE_ID+"&search.clubid="+CAFE_ID+"&search.memberid=%s&search.query=%s&search.nickname=&search.writerid=%s&search.page=%d";
//http://m.cafe.naver.com/CommentView.nhn?search.clubid=10050146&search.articleid=315815859
        public static final String COMMENT_VIEW = "http://m.cafe.naver.com/CommentView.nhn?search.clubid="+CAFE_ID+"&search.articleid=%s";
        public static final String REUP_ARTICLE = "http://cafe.naver.com/ArticleWrite.nhn?articleid=%d&clubid="+CAFE_ID+"&m=modify";
    }

    interface Intents{
        public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
        public static final String START_APP = "START_APP";
        public static final String OBSERVING = "com.azazel.cafecrawler.OBSERVING";
        public static final String RE_UPLOAD = "com.azazel.cafecrawler.RE_UPLOAD";
    }

    interface Keys{
        public static final String LIST_POSITION = "LIST_POSITION";
    }
}
