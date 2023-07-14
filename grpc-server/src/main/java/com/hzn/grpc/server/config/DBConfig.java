package com.hzn.grpc.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class DBConfig {

	private final ApplicationContext applicationContext;

	@Bean(name = "hikariConfig")
	@ConfigurationProperties(prefix = "spring.datasource.hikari")
	public HikariConfig hikariConfig () {
		return new HikariConfig ();
	}

	@Bean(name = "dataSource")
	public DataSource dataSource () {
		return new HikariDataSource (hikariConfig ());
	}

	@Bean(name = "sqlSessionFactory")
	public SqlSessionFactory sqlSessionFactory (@Qualifier("dataSource") DataSource dataSource) throws Exception {
		SqlSessionFactoryBean bean = new SqlSessionFactoryBean ();

		bean.setDataSource (dataSource);
		bean.setMapperLocations (applicationContext.getResources ("classpath:/mapper/**.xml"));
		org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration ();
		configuration.setMapUnderscoreToCamelCase (true);
		configuration.setCallSettersOnNulls (false);
		configuration.setJdbcTypeForNull (JdbcType.NULL);
		bean.setConfiguration (configuration);
		bean.setTypeAliasesPackage ("com.hzn.grpc.server");

		return bean.getObject ();
	}

	@Bean(name = "sqlSessionTemplate")
	public SqlSessionTemplate sqlSessionTemplate (@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate (sqlSessionFactory);
	}
}
