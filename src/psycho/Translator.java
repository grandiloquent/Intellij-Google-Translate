package psycho;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class Translator {
    // [[["We are all good kids. ","我们都是好孩子。",null,null,1],["You are a pig","你是猪",null,null,1]],null,"zh-CN",null,null,null,0.94138455,null,[["zh-CN"],null,[0.94138455],["zh-CN"]]]

    private static final String TRANSLATE_URL = "http://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=%s&dt=t&q=%s";

    private static Pattern sPattern = Pattern.compile("(?<=\\[)\\\"([^\\\"]*?)\\\"(?!\\])");

    private static Translator mTranslator;


    public Translator() {
    }

    public static Translator get() {
        if (mTranslator == null) {
            mTranslator = new Translator();
        }
        return mTranslator;
    }

    public void query(int start, int end, String query, String targetLanguage, Document document) {
        // https://translate.google.cn/translate_a/single?client=gtx&sl=auto&tl=zh&dt=t&dt=bd&ie=UTF-8&oe=UTF-8&dj=1&source=icon&q=我嫩
        String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" + targetLanguage + "&dt=t&q=" + URLEncoder.encode(query);
        CloseableHttpClient httpclient = HttpClients.createMinimal();

        String result;
        try {


            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            httpGet.addHeader("Accept-Encoding", "gzip, deflate");
            httpGet.addHeader("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6");
            httpGet.addHeader("Cache-Control", "max-age=0");
            httpGet.addHeader("Connection", "keep-alive");
            httpGet.addHeader("Referer", "https://www.google.com.hk/");
            httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36");

            result = httpclient.execute(httpGet, new ResponseHandler<String>() {

                @Override
                public String handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                    HttpEntity entity = httpResponse.getEntity();
                    //BufferedInputStream bi = new BufferedInputStream(entity.getContent());
                    GZIPInputStream gzipInputStream = new GZIPInputStream(entity.getContent());

//                    byte[] buffer = new byte[512];
//                    int copySize;
//                    FileOutputStream output = new FileOutputStream("1.txt");
//                    while ((copySize = gzipInputStream.read(buffer)) > 0) {
//                        output.write(buffer, 0, copySize);
//                        output.flush();
//                    }
                    BufferedReader br = new BufferedReader(new InputStreamReader(gzipInputStream, Charset.forName("utf-8")));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    String result = "";
                    Matcher matcher = sPattern.matcher(sb.toString());
                    while (matcher.find()) {
                        result += matcher.group(1);
                    }

                    return result;

                }
            });

//                @Override
//                public String handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
//

//                }


        } catch (Exception e) {

            result = null;
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final String translateResult = result;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                document.replaceString(start, end, translateResult);

            }
        };
        Application application = ApplicationManager.getApplication();

        if (application.isDispatchThread()) {

            application.runWriteAction(runnable);

        } else {
            application.invokeLater(() -> application.runWriteAction(runnable));
        }


    }


    public interface Callback {
        void onQuery(String query, String result);
    }

}
