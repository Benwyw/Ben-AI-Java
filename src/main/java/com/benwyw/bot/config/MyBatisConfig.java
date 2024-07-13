package com.benwyw.bot.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
public class MyBatisConfig {

    @Autowired
    private MybatisProperties properties;

    @Autowired
    private DataSource dataSource;

    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources(properties.getMapperLocations()[0]));
        SqlSessionFactory factory = factoryBean.getObject();
        if (factory != null) {
            factory.getConfiguration().setMapUnderscoreToCamelCase(true);
            factory.getConfiguration().setJdbcTypeForNull(JdbcType.NULL);
        }
        // other configurations
        return factory;
    }
}
