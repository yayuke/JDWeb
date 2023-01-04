package com.mxin.jdweb.network;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by 周旭 on 2017/1/18.
 * OKHttp工具类(上传，下载文件)
 */

public class OKHttpUtils {

    private static OkHttpClient client;
    private static OkHttpClient clientFile;

    /**
     * 创建一个OkHttpClient的对象的单例
     *
     * @return
     */
    public synchronized static OkHttpClient getOkHttpClientInstance(boolean isFile) {
        if (!isFile && client == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    //设置连接超时等属性,不设置可能会报异常
                    .connectTimeout(6, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS);
            // 日志拦截器
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging); //
            client = builder.build();
        }
        if (isFile && clientFile == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    //设置连接超时等属性,不设置可能会报异常
                    .connectTimeout(6, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS);
            // 日志拦截器
//            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
//            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
//            builder.addInterceptor(logging); //
            clientFile = builder.build();
        }
        return isFile ? clientFile : client;
    }

    /**
     * 获取文件MimeType
     *
     * @param filename 文件名
     * @return
     */
    private static String getMimeType(String filename) {
        FileNameMap filenameMap = URLConnection.getFileNameMap();
        String contentType = filenameMap.getContentTypeFor(filename);
        if (contentType == null) {
            contentType = "application/octet-stream"; //* exe,所有的可执行程序
        }
        return contentType;
    }

    /**
     * 上传文件
     * 获得Request实例(不带进度)
     *
     * @param url       上传文件到服务器的地址
     * @param fileNames 完整的文件名（带完整路径）
     * @return
     */
    private static Request getRequest(String url, List<String> fileNames) {
        Request.Builder builder = new Request.Builder();
        builder.url(url)
                .post(getRequestBody(fileNames))
                .tag(url) //设置请求的标记，可在取消时使用
        ;
        return builder.build();
    }



    /**
     * 通过Url地址和表单的键值对来创建Request实例
     *
     * @param url 上传表单数据到服务器的地址
     * @param map 由提交的表单的每一项组成的Map
     *            （如用户名，key:username,value:zhangsan）
     * @return
     */
    private static Request getRequest(String url, Map<String, String> map) {
        Request.Builder builder = new Request.Builder();
        builder.url(url)
                .post(getRequestBody(map))
                .tag(url) //设置请求的标记，可在取消时使用
        ;
        return builder.build();
    }

    /**
     * 通过Url地址和表单的键值对来创建Request实例
     *
     * @param url       上传表单数据到服务器的地址
     * @param map       由提交的表单的每一项组成的Map
     *                  （如用户名，key:username,value:zhangsan）
     * @param fileNames 完整的文件路径名
     * @return
     */
    private static Request getRequest(String url, Map<String, String> map, List<String> fileNames) {
        Request.Builder builder = new Request.Builder();
        builder.url(url)
                .post(getRequestBody(map, fileNames))
                .tag(url) //设置请求的标记，可在取消时使用
        ;
        return builder.build();
    }

    /**
     * 通过下载的URL地址构建equest实例
     *
     * @param downloadUrl 文件下载的地址
     * @return
     */
    private static Request getRequest(String downloadUrl) {
        Request.Builder builder = new Request.Builder();
        builder.url(downloadUrl).tag(downloadUrl);
        return builder.build();
    }

    /**
     * 通过键值对(表单中的name-value)创建RequestBody
     *
     * @param map 由提交的表单的每一项组成的Map
     *            （如用户名，key:username,value:zhangsan）
     * @return
     */
    private static RequestBody getRequestBody(Map<String, String> map) {
        FormBody.Builder builder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    /**
     * 根据表单的键值对和上传的文件生成RequestBody
     *
     * @param map       由提交的表单的每一项组成的Map
     *                  （如用户名，key:username,value:zhangsan）
     * @param fileNames 完整的文件路径名
     * @return
     */
    private static RequestBody getRequestBody(Map<String, String> map, List<String> fileNames) {
        MultipartBody.Builder builder = new MultipartBody.Builder(); //创建MultipartBody.Builder，用于添加请求的数据
        for (Map.Entry<String, String> entry : map.entrySet()) { //对键值对进行遍历
            builder.addFormDataPart(entry.getKey(), entry.getValue()); //把键值对添加到Builder中
        }
        for (int i = 0; i < fileNames.size(); i++) { //对文件进行遍历
            File file = new File(fileNames.get(i)); //生成文件
            String fileType = getMimeType(file.getName()); //根据文件的后缀名，获得文件类型
            builder.addFormDataPart( //给Builder添加上传的文件
                    "image",  //请求的名字
                    file.getName(), //文件的文字，服务器端用来解析的
                    RequestBody.create(MediaType.parse(fileType), file) //创建RequestBody，把上传的文件放入
            );
        }
        return builder.build(); //根据Builder创建请求
    }


    /**
     * 通过上传的文件的完整路径生成RequestBody
     *
     * @param fileNames 完整的文件路径
     * @return
     */
    private static RequestBody getRequestBody(List<String> fileNames) {
        //创建MultipartBody.Builder，用于添加请求的数据
        MultipartBody.Builder builder = new MultipartBody.Builder();
        for (int i = 0; i < fileNames.size(); i++) { //对文件进行遍历
            File file = new File(fileNames.get(i)); //生成文件
            //根据文件的后缀名，获得文件类型
            String fileType = getMimeType(file.getName());
            builder.addFormDataPart( //给Builder添加上传的文件
                    "image",  //请求的名字
                    file.getName(), //文件的文字，服务器端用来解析的
                    RequestBody.create(MediaType.parse(fileType), file) //创建RequestBody，把上传的文件放入
            );
        }
        return builder.build(); //根据Builder创建请求
    }



    /**
     * 只上传文件
     * 根据url，发送异步Post请求(不带进度)
     *
     * @param url       提交到服务器的地址
     * @param fileNames 完整的上传的文件的路径名
     * @param callback  OkHttp的回调接口
     */
    public static void doPostRequest(String url, List<String> fileNames, Callback callback) {
        Call call = getOkHttpClientInstance(true).newCall(getRequest(url, fileNames));
        call.enqueue(callback);
    }

    /**
     * 只提交表单
     * 根据url和键值对，发送异步Post请求
     *
     * @param url      提交到服务器的地址
     * @param map      提交的表单的每一项组成的Map
     *                 （如用户名，key:username,value:zhangsan）
     * @param callback OkHttp的回调接口
     */
    public static Call doPostRequest(String url, Map<String, String> map, Callback callback) {
        Call call = getOkHttpClientInstance(false).newCall(getRequest(url, map));
        call.enqueue(callback);
        return call;
    }


    /**
     * 可同时提交表单，和多文件
     * 根据url和键值对，发送异步Post请求
     *
     * @param url       提交到服务器的地址
     * @param map       提交的表单的每一项组成的Map
     *                  （如用户名，key:username,value:zhangsan）
     * @param fileNames 完整的上传的文件的路径名
     * @param callback  OkHttp的回调接口
     */
    public static Call doPostRequest(String url, Map<String, String> map, List<String> fileNames, Callback callback) {
        Call call = getOkHttpClientInstance(true).newCall(getRequest(url, map, fileNames));
        call.enqueue(callback);
        return call;
    }


    //在本地保存下载的文件
    private static void saveDownloadFile(Response response, String savePath) throws IOException {
        InputStream inputStream = getInputStreamFromResponse(response);
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        File parentFile = new File(savePath).getParentFile();
        if(!parentFile.exists()){
            parentFile.mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(savePath);
        byte[] data = new byte[10 * 1024];
        int len;
        while ((len = bis.read(data)) != -1) {
            fos.write(data, 0, len);
        }
        Log.i("TAG", "保存文件"+savePath+"成功");
        fos.flush();
        fos.close();
        bis.close();
    }

    //获取字符串
    public static String getString(Response response) throws IOException {
        if (response != null && response.isSuccessful()) {
            return response.body().string();
        }
        return null;
    }


    /**
     * 根据响应获得字节数组
     *
     * @param response
     * @return
     * @throws IOException
     */
    public static byte[] getBytesFromResponse(Response response) throws IOException {
        if (response != null && response.isSuccessful()) {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                return responseBody.bytes();
            }
        }
        return null;
    }


    /**
     * 根据响应获得输入流
     *
     * @param response
     * @return
     * @throws IOException
     */
    public static InputStream getInputStreamFromResponse(Response response) throws IOException {
        if (response != null && response.isSuccessful()) {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                return responseBody.byteStream();
            }
        }
        return null;
    }


    /**
     * 取消所有为tag的Call
     *
     * @param tag 请求的标记
     */
    public static void cancelCallsWithTag(Object tag) {

        if (tag == null) {
            return;
        }

        synchronized (client.dispatcher().getClass()) {
            for (Call call : client.dispatcher().queuedCalls()) {
                if (tag.equals(call.request().tag())) call.cancel();
            }

            for (Call call : client.dispatcher().runningCalls()) {
                if (tag.equals(call.request().tag())) call.cancel();
            }
        }
    }
}