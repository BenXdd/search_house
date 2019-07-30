package com.benx.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration // 配置类 相当于把该类作为spring的xml配置文件中的<beans>
@EnableJpaRepositories(basePackages ="com.benx.repository")  //扫描我们的仓库类
@EnableTransactionManagement // 允许事物管理
public class JPAConfig {

    @Bean //数据源
    @ConfigurationProperties(prefix = "spring.datasource")//属性前缀 会从配置文件映射到DataSource中来
    public DataSource dataSource(){

        return DataSourceBuilder.create().build();
    }

    @Bean //其他配置   实体类的管理工厂
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(){
        //集成的实现是hibernate
        HibernateJpaVendorAdapter jpaVendor = new HibernateJpaVendorAdapter();
        jpaVendor.setGenerateDdl(false);//是否生成sql

        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(dataSource());//设置数据源
        entityManagerFactory.setJpaVendorAdapter(jpaVendor);//设置jpa适配器
        entityManagerFactory.setPackagesToScan("com.benx.entity");//设置需要寻找的实体类包名

        return entityManagerFactory;

    }

    @Bean //上面有事物管理的注解,我们就需要实例化一个事物管理的类    注入之前的 映射管理类(此方法上面)
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory){

        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        return transactionManager;
    }

}
