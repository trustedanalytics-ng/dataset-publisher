/**
 * Copyright (c) 2015 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trustedanalytics.datasetpublisher;

import static org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.trustedanalytics.cloud.auth.AuthTokenRetriever;
import org.trustedanalytics.cloud.auth.OAuth2TokenRetriever;
import org.trustedanalytics.datasetpublisher.boundary.ExternalTool;
import org.trustedanalytics.hadoop.config.client.AppConfiguration;
import org.trustedanalytics.hadoop.config.client.Property;
import org.trustedanalytics.hadoop.config.client.ServiceInstanceConfiguration;
import org.trustedanalytics.hadoop.config.client.ServiceType;
import org.trustedanalytics.hadoop.config.client.SimpleAppConfiguration;
import org.trustedanalytics.hadoop.config.client.SimpleInstanceConfiguration;
import org.trustedanalytics.hadoop.config.client.helper.Hive;
import org.trustedanalytics.hadoop.config.client.oauth.JwtToken;
import org.trustedanalytics.hadoop.config.client.oauth.TapOauthToken;

import com.google.common.collect.ImmutableSet;

@Configuration
@EnableConfigurationProperties({Config.Hue.class, Config.Arcadia.class})
public class Config {

  @Value("${hive.uri}")
  private String hiveUri;
  
  @Value("${hadoop.conf.dir}")
  private String hadoopConfDir;
  
  @Value("${krb.kdc}")
  private String krbKdc;
  
  @Value("${krb.realm}")
  private String krbRealm;
  
  @Profile("cloud")
  @Bean
  public Hive hiveClient() throws IOException {
    return Hive.newInstance();
  }

  @Profile("kubernetes")
  @Bean
  public Hive hiveClientK8s() throws IOException {
      return Hive.newInstance(appConfig());
  }

  @Bean
  public AppConfiguration appConfig() throws IOException {
    Map<Property, String> properties = new HashMap<>();
    properties.put(Property.KRB_KDC, krbKdc);
    properties.put(Property.KRB_REALM, krbRealm);
    properties.put(Property.HIVE_URL, hiveUri);
    
    SimpleInstanceConfiguration configuration = new SimpleInstanceConfiguration("config", getHadoopConfiguration(hadoopConfDir), properties);

    Map<ServiceType, ServiceInstanceConfiguration> svcMap = new HashMap<>();
    svcMap.put(ServiceType.HIVE_TYPE, configuration);
    svcMap.put(ServiceType.KERBEROS_TYPE, configuration);
    
    return new SimpleAppConfiguration(svcMap);
  }
  
  private static org.apache.hadoop.conf.Configuration getHadoopConfiguration(String confDir) throws IOException {
    return Arrays.asList("core-site.xml", "hdfs-site.xml").stream()
      .collect(org.apache.hadoop.conf.Configuration::new, (c, f) -> c.addResource(new Path(confDir, f)), (c, d) -> {});
  }

  @Bean
  public AuthTokenRetriever authTokenRetriever() {
    return new OAuth2TokenRetriever();
  }

  @Bean
  @Scope(value = SCOPE_REQUEST, proxyMode = ScopedProxyMode.INTERFACES)
  public JwtToken userIdentity(AuthTokenRetriever tokenRetriever) {
    final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return new TapOauthToken(tokenRetriever.getAuthToken(auth));
  }

  @Bean
  public Supplier<Set<String>> restrictedKeywords() {
    return () -> ImmutableSet.<String>builder().add("add", "aggregate", "all", "alter",
                                                    "and", "api_version", "as", "asc", "avro",
                                                    "between", "bigint", "binary", "boolean", "by",
                                                    "cached",
                                                    "case", "cast", "change", "char", "class",
                                                    "close_fn", "column", "columns", "comment",
                                                    "compute",
                                                    "create", "cross", "data", "database",
                                                    "databases", "date", "datetime", "decimal",
                                                    "delimited",
                                                    "desc", "describe", "distinct", "div", "double",
                                                    "drop", "else", "end", "escaped", "exists",
                                                    "explain", "external", "false", "fields",
                                                    "fileformat", "finalize_fn", "first", "float",
                                                    "format",
                                                    "formatted", "from", "full", "function",
                                                    "functions", "group", "having", "if", "in",
                                                    "incremental",
                                                    "init_fn", "inner", "inpath", "insert", "int",
                                                    "integer", "intermediate", "interval", "into",
                                                    "invalidate", "is", "join", "last", "left",
                                                    "like", "limit", "lines", "load", "location",
                                                    "merge_fn",
                                                    "metadata", "not", "null", "nulls", "offset",
                                                    "on", "or", "order", "outer", "overwrite",
                                                    "parquet",
                                                    "parquetfile", "partition", "partitioned",
                                                    "partitions", "prepare_fn", "produced",
                                                    "rcfile", "real",
                                                    "refresh", "regexp", "rename", "replace",
                                                    "returns", "right", "rlike", "row", "schema",
                                                    "schemas",
                                                    "select", "semi", "sequencefile",
                                                    "serdeproperties", "serialize_fn", "set",
                                                    "show", "smallint",
                                                    "stats", "stored", "straight_join", "string",
                                                    "symbol", "table", "tables", "tblproperties",
                                                    "terminated", "textfile", "then", "timestamp",
                                                    "tinyint", "to", "true", "uncached", "union",
                                                    "update_fn", "use", "using", "values", "view",
                                                    "when", "where", "with").build();
  }

  @ConfigurationProperties(prefix = "hue")
  public static class Hue extends ExternalTool {

  }

  @ConfigurationProperties(prefix = "arcadia")
  public static class Arcadia extends ExternalTool {

  }
}
