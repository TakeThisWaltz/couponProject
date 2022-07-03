package kr.azazel.barcode.service;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

import com.azazel.framework.AzApplication;
import com.azazel.framework.AzSimpleWorker;
import com.azazel.framework.network.AzHttpRequestConfig;
import com.azazel.framework.network.HttpRequestBuilder;
import com.azazel.framework.network.NetworkUtil;
import com.azazel.framework.util.LOG;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kr.azazel.barcode.MetaManager;
import kr.azazel.barcode.vo.BarcodeResponse;

public class TextExtractorUtil {
    private static final String TAG = "TextExtractorUtil";

    private interface TextSupplier {
        String extract(Matcher matcher);
    }

    private static final List<Pair<Pattern, TextSupplier>> PATTERN_EXPIRE =
            List.of(
                    Pair.create(Pattern.compile("(유효기간|사용기한)\\~?((20)?([0-9]\\d))[년|\\-|\\_|\\/|\\.]([01]\\d)[월|\\-|\\_|\\/|\\.]([0-3]\\d)일?"),
                            (matcher) -> "20" + matcher.group(4) + "-" + matcher.group(5) + "-" + matcher.group(6)),
                    Pair.create(Pattern.compile("(유효기간|사용기한)?((20)?([0-9]\\d))[년|\\-|\\_|\\/|\\.]([01]\\d)[월|\\-|\\_|\\/|\\.]([0-3]\\d)일?까지"),
                            (matcher) -> "20" + matcher.group(4) + "-" + matcher.group(5) + "-" + matcher.group(6)),
                    Pair.create(Pattern.compile("\\~?((20)?([0-9]\\d))[년|\\-|\\_|\\/|\\.]([01]\\d)[월|\\-|\\_|\\/|\\.]([0-3]\\d)(일|까지)?"),
                            (matcher) -> "20" + matcher.group(3) + "-" + matcher.group(4) + "-" + matcher.group(5))
            );

    public static String extractExpireDate(String text) {
        text = text.replaceAll("\\s", "");
        text = text.replaceAll("、", "");

        for (Pair<Pattern, TextSupplier> item : PATTERN_EXPIRE) {
            Matcher matcher = item.first.matcher(text);
            if (matcher.find()) {
                return item.second.extract(matcher);
            }
        }
        return null;
    }

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static void extractExpireDateInBackground(Uri uri) {
        MetaManager.getInstance().setExtractedExpireDate(null);
        AzApplication.executeJobOnBackground(new AzSimpleWorker() {
            @Override
            public void doInBackgroundAndResult() {
                try {
                    String text = OcrUtil.extractText(uri);

                    String apiUrl = "http://az-elb-49526221.ap-northeast-2.elb.amazonaws.com/api/barcode/extract/expireDate";
                    HttpRequestBuilder.create(apiUrl, apiUrl, AzHttpRequestConfig.HTTP_AZ_CONFIG)
                            .setMethod(HttpRequestBuilder.HttpMethod.POST)
                            .setPayload("application/json",
                                    objectMapper.writeValueAsString(Map.of("rawText", text)))
                            .execute(new NetworkUtil.StringResponseHandler() {
                                @Override
                                public void handleResponse(int statusCode, String body) {
                                    if (statusCode == 200 && body != null) {
                                        try {
                                            String expiredate = objectMapper.readTree(body).get("content").textValue();
                                            setResult(true, expiredate);
                                        } catch (JsonProcessingException e) {
                                            LOG.e(TAG, "extractExpireDateInBackground json err", e);
                                        }
                                    }
                                }
                            });

//                    String date = TextExtractorUtil.extractExpireDate(text);
//                    setResult(true, date);
                } catch (Exception e) {
                    LOG.e(TAG, "extractExpireDateInBackground http err", e);
                    setResult(false, null);
                }
            }

            @Override
            public void postOperationWithResult(boolean result, Object value) {
                String date = null;
                if (result && value != null) {
                    date = (String) value;
                }
                MetaManager.getInstance().setExtractedExpireDate(date);
            }
        });
    }

    public static void extractBarcodeInfoInBackground(Uri uri) {
        MetaManager.getInstance().setTempBarcode(null);
        AzApplication.executeJobOnBackground(new AzSimpleWorker() {
            @Override
            public void doInBackgroundAndResult() {
                try {
                    String text = OcrUtil.extractText(uri);

                    String apiUrl = "http://az-elb-49526221.ap-northeast-2.elb.amazonaws.com/api/barcode/extract";
                    HttpRequestBuilder.create(apiUrl, apiUrl, AzHttpRequestConfig.HTTP_AZ_CONFIG)
                            .setMethod(HttpRequestBuilder.HttpMethod.POST)
                            .setPayload("application/json",
                                    objectMapper.writeValueAsString(Map.of("rawText", text)))
                            .execute(new NetworkUtil.StringResponseHandler() {
                                @Override
                                public void handleResponse(int statusCode, String body) {
                                    if (statusCode == 200 && body != null) {
                                        try {
                                            JsonNode barcode = objectMapper.readTree(body).get("content");
                                            BarcodeResponse barcodeResponse = new BarcodeResponse();
                                            if(barcode.has("expireDate") && !TextUtils.isEmpty(barcode.get("expireDate").textValue())){
                                                barcodeResponse.setExpireDate(barcode.get("expireDate").textValue()
                                                        .split("T")[0]);
                                            }
                                            if(barcode.has("type")){
                                                barcodeResponse.setType(barcode.get("type").textValue());
                                            }
                                            if(barcode.has("store")){
                                                barcodeResponse.setStore(barcode.get("store").textValue());
                                            }
                                            if(barcode.has("itemName")){
                                                barcodeResponse.setItem(barcode.get("itemName").textValue());
                                            }

                                            setResult(true, barcodeResponse);
                                        } catch (JsonProcessingException jsone) {
                                            LOG.e(TAG, "extractExpireDateInBackground json err", jsone);
                                        } catch (Exception e) {
                                            LOG.e(TAG, "extractExpireDateInBackground err", e);
                                        }
                                    }
                                }
                            });

//                    String date = TextExtractorUtil.extractExpireDate(text);
//                    setResult(true, date);
                } catch (Exception e) {
                    LOG.e(TAG, "extractExpireDateInBackground http err", e);
                    setResult(false, null);
                }
            }

            @Override
            public void postOperationWithResult(boolean result, Object value) {
                BarcodeResponse data = null;
                if (result && value != null) {
                    data = (BarcodeResponse) value;
                }
                MetaManager.getInstance().setTempBarcode(data);
            }
        });
    }
}
