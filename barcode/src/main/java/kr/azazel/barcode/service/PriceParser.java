package kr.azazel.barcode.service;

import com.azazel.framework.AzRuntimeException;
import com.azazel.framework.network.AzHttpRequestConfig;
import com.azazel.framework.network.HttpRequestBuilder;
import com.azazel.framework.network.NetworkUtil;
import com.azazel.framework.task.SingleTask;
import com.azazel.framework.task.TaskAsyncHelper;
import com.azazel.framework.util.LOG;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kr.azazel.barcode.joonggo.Article;
import kr.azazel.barcode.joonggo.CrawlConstants;

/**
 * Created by ji on 2016. 12. 10..
 */

public class PriceParser {
    private static final String TAG = "PriceParser";

    private List<Article> articles;

    public PriceParser(List<Article> articles) {
        this.articles = articles;
    }

    public void startParse(PriceFoundListener listener) {
        LOG.d(TAG, "startParse - cnt : " + articles.size());


        TaskAsyncHelper asyncHelper = new TaskAsyncHelper(5, true);

        for (Article article : articles) {
            asyncHelper.addTask(new SingleTask(article) {
                @Override
                public Object doJob(Object[] args) {
                    Article item = (Article) args[0];

                    String url = String.format(CrawlConstants.Urls.DETAIL_VIEW, item.articleId);
                    HttpRequestBuilder.create(url, url, AzHttpRequestConfig.HTTP_AZ_CONFIG_WITH_COOKIE)
                            .execute(new NetworkUtil.StringResponseHandler() {

                                @Override
                                public void handleResponse(int status, String body) {
                                    parse(body);
                                }
                            });
                    return null;
                }
            });
            break;
        }


        asyncHelper.runTask(new TaskAsyncHelper.TaskCompleteListener() {
            @Override
            public void onFinish(boolean isSuccess, Object[] resultArr, AzRuntimeException e) {
                LOG.i(TAG, "startParse - finished : " + resultArr.length);

            }
        });

    }

    private void parse(String body) {
        LOG.d(TAG, "parse body : " + body);
        Document doc = Jsoup.parse(body);
        Elements elPrice = doc.select("em.price");
        for (Element e : elPrice) {
            LOG.d(TAG, "price ele : " + e);
        }
        Elements elContent = doc.select("div#postContent");
        for (Element e : elContent) {
            LOG.d(TAG, "content ele : " + e);
        }
    }

    public interface PriceFoundListener {
        public void onPriceFound(Article article, int[] prices);
    }

    private static final String NUM = "1234567890일이삼사오육칠팔구만천백십,";
    private static final String UNIT = "만천백십";
    private static final String REG_EXP_NUM = "(\\d{1,3},?)?\\d{1,3},?\\d{3}원";
    private static final String REG_EXP_STR = "([" + NUM + "]+[" + UNIT + "])+원?";

    public static String[] parstPrice(String content) {
        String source = content.replaceAll("<[^>]*>", " ");
        LOG.d(TAG, "parsePrice : " + source);

        List<String> resultArr = new ArrayList<String>();

        {
            Pattern pattern = Pattern.compile(REG_EXP_NUM);
            Matcher match = pattern.matcher(source);

            while (match.find()) {
                String price = match.group();//.replaceAll(",","");
                LOG.d(TAG, "parsed - price1 : " + price);
                if (!resultArr.contains(price))
                    resultArr.add(price);
            }
        }
        {
            Pattern pattern = Pattern.compile(REG_EXP_STR);
            Matcher match = pattern.matcher(source);

            while (match.find()) {
                String price = match.group().replaceAll(",", "");
                LOG.d(TAG, "parsed - price2 : " + price);
                if (!resultArr.contains(price))
                    resultArr.add(price);
            }
        }

        return resultArr.toArray(new String[resultArr.size()]);
    }
}
