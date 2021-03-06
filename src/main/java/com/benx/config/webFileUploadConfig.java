package com.benx.config;

import com.google.gson.Gson;
import com.qiniu.common.Zone;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.MultipartProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;

/**
 * 文件上传配置
 *  @ConditionalOnClass
 *      @ConditionalOnClass会检查类加载器中是否存在对应的类，如果有的话被注解修饰的类就有资格被Spring容器所注册，否则会被skip。
 *
 *  @EnableConfigurationProperties
 *      指定需要用哪个实体类来装载配置信息
 *
 *  @EnableConfigurationProperties
 *      允许springboot帮我们自动配置一些属性
 */
@Configuration
@ConditionalOnClass({Servlet.class, StandardServletMultipartResolver.class, MultipartConfigElement.class})
@ConditionalOnProperty(prefix = "spring.http.multipart",name = "enabled",matchIfMissing = true)
@EnableConfigurationProperties(MultipartProperties.class)
public class webFileUploadConfig {
    //让spring帮我们自动配置,所以需要一个对象接收他
    private final MultipartProperties multipartProperties;

    public webFileUploadConfig(MultipartProperties multipartProperties){
        this.multipartProperties=multipartProperties;
    }

    /**
     * 上传配置
     */
    @Bean
    @ConditionalOnMissingBean
    public MultipartConfigElement multipartConfigElement(){
        return this.multipartProperties.createMultipartConfig();//自动创建
    }

    /**
     * 注册解析器(解析文件)
     */
    @Bean(name = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME)
    @ConditionalOnMissingBean(MultipartResolver.class)
    public StandardServletMultipartResolver multipartResolver(){
        StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();
        multipartResolver.setResolveLazily(this.multipartProperties.isResolveLazily());
        return multipartResolver;
    }

    /**
     * 华东机房
     *
     *  华东	Zone.zone0()
     *  华北	Zone.zone1()
     *  华南	Zone.zone2()
     *  北美	Zone.zoneNa0()
     *  东南亚	Zone.zoneAs0()
     *  refer https://developer.qiniu.com/kodo/sdk/1239/java
     */
    @Bean
    public com.qiniu.storage.Configuration qiniuConfig() {
        return new com.qiniu.storage.Configuration(Zone.zone0());
    }
    /**
     * 构建一个七牛上传工具实例
     */
    @Bean
    public UploadManager uploadManager() {
        return new UploadManager(qiniuConfig());
    }

    @Value("${qiniu.AccessKey}")
    private String accessKey;
    @Value("${qiniu.SecretKey}")
    private String secretKey;

    /**
     * 认证信息实例
     * @return
     */
    @Bean
    public Auth auth(){
        return Auth.create(accessKey,secretKey);
    }

    /**
     * 构建七牛空间管理实例
     */
    @Bean
    public BucketManager bucketManager(){
        return new BucketManager(auth(),qiniuConfig());
    }

    /**
     * 解析返回数据
     * @return
     */
    @Bean
    public Gson gson(){
        return new Gson();
    }
}
