package com.note.utils;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class QiniuUtils {
    // 通过 @Value 注解从配置文件中读取值
    @Value("${qiniu.accessKey}")
    private String accessKey;
 
    @Value("${qiniu.secretKey}")
    private String secretKey;
 
    @Value("${qiniu.bucket}")
    private String bucket;
 
    @Value("${qiniu.hostName}")
    private String hostName;

    /**
     * 上传图片
     * @param bytes 字节数组
     * @param folderName 上传到的文件夹名称
     * @param fileName 文件名称
     * @return 返回 url 地址
     */
    public String uploadByBytes(byte[] bytes,String folderName,String fileName){
 
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.huanan());
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;// 指定分片上传版本
        //...其他参数参考类注释
        UploadManager uploadManager = new UploadManager(cfg);

        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);

        String key = folderName + "/" + fileName;
        try {
            Response response = uploadManager.put(bytes, key, upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            System.out.println(putRet.key);
            System.out.println(putRet.hash);
            return hostName+putRet.key;
        } catch (QiniuException ex) {
            ex.printStackTrace();
            if (ex.response != null) {
                System.err.println(ex.response);
                try {
                    String body = ex.response.toString();
                    System.err.println(body);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
 
    /**
     *删除图片
     * @param key
     * @return
     */
    public String delete(String key){
        Auth auth = Auth.create(accessKey,secretKey);
        Configuration cfg = new Configuration(Region.huanan());
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            Response delete = bucketManager.delete(bucket, key);
            if(delete.statusCode == 200) {
                return "success";
            }
        }catch (QiniuException Q){
            return Q.toString();
        }
        return "fail";
    }

    /**
     * 从完整访问 URL 反解出七牛云存储 key（去掉 hostName 前缀）
     * @param fileUrl 完整访问 URL，如 http://host/image/xxx.png
     * @return 存储 key，如 image/xxx.png；解析失败返回 null
     */
    public String resolveKey(String fileUrl){
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }
        if (fileUrl.startsWith(hostName)) {
            return fileUrl.substring(hostName.length());
        }
        // 回退：取协议域名之后的路径部分
        int schemeIdx = fileUrl.indexOf("://");
        int pathIdx = fileUrl.indexOf('/', schemeIdx < 0 ? 0 : schemeIdx + 3);
        return pathIdx >= 0 ? fileUrl.substring(pathIdx + 1) : fileUrl;
    }

}
