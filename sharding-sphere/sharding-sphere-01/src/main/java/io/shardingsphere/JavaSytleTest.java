package io.shardingsphere;


import io.shardingsphere.api.algorithm.sharding.PreciseShardingValue;
import io.shardingsphere.api.algorithm.sharding.standard.PreciseShardingAlgorithm;

import io.shardingsphere.api.config.rule.ShardingRuleConfiguration;
import io.shardingsphere.api.config.rule.TableRuleConfiguration;
import io.shardingsphere.api.config.strategy.InlineShardingStrategyConfiguration;
import io.shardingsphere.api.config.strategy.StandardShardingStrategyConfiguration;
import io.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory;
import org.apache.commons.dbcp.BasicDataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

public class JavaSytleTest {

   public static void main(String[] args) throws SQLException {
      // 配置真实数据源
      Map<String, DataSource> dataSourceMap = new HashMap<>();//size 2 为两个数据库的datasource
      // 配置第一个数据源
      BasicDataSource dataSource1 = new BasicDataSource();
      dataSource1.setDriverClassName("com.mysql.jdbc.Driver");
      dataSource1.setUrl("jdbc:mysql://localhost:3306/orders_0");
      dataSource1.setUsername("root");
      dataSource1.setPassword("root");
      dataSourceMap.put("orders_0", dataSource1);
      // 配置第二个数据源
      BasicDataSource dataSource2 = new BasicDataSource();
      dataSource2.setDriverClassName("com.mysql.jdbc.Driver");
      dataSource2.setUrl("jdbc:mysql://localhost:3306/orders_1");
      dataSource2.setUsername("root");
      dataSource2.setPassword("root");
      dataSourceMap.put("orders_1", dataSource2);

      ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
      shardingRuleConfig.getTableRuleConfigs().add(getOrderTableRuleConfiguration());
      shardingRuleConfig.getTableRuleConfigs().add(getOrderItemTableRuleConfiguration());
      shardingRuleConfig.getBindingTableGroups().add("t_order,t_order_item");
      //采用user_id进行分库
      shardingRuleConfig.setDefaultDatabaseShardingStrategyConfig(new InlineShardingStrategyConfiguration("user_id", "orders_${user_id % 2}"));//分别得出orders_0 orders1
      //采用order_id进行分表
      shardingRuleConfig.setDefaultTableShardingStrategyConfig(new StandardShardingStrategyConfiguration("order_id", new PreciseShardingAlgorithm<Long>() {

         @Override
         public String doSharding(Collection<String> collection, final PreciseShardingValue<Long> preciseShardingValue) {
            for (String each : collection) {
               System.out.println(each+"--------"+preciseShardingValue.getValue()+"---------"+preciseShardingValue.getValue() % 2);
               if (each.endsWith(preciseShardingValue.getValue() % 2 + "")) {
                  return each;
               }
            }
            throw new UnsupportedOperationException();
         }
      }));
      // 获取数据源对象
      Properties properties = new Properties();
      properties.setProperty("sql.show", "true");
      DataSource dataSource = ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingRuleConfig, new ConcurrentHashMap(), properties);
      JavaSytleTest test = new JavaSytleTest();
      test.drop(dataSource);//删除表
      test.create(dataSource);//创建表
      test.insertData(dataSource);//插入数据

   }


   private static TableRuleConfiguration getOrderTableRuleConfiguration() {
      TableRuleConfiguration orderTableRuleConfig = new TableRuleConfiguration();
      orderTableRuleConfig.setLogicTable("t_order");//逻辑表
      orderTableRuleConfig.setActualDataNodes("orders_${0..1}.t_order_${[0, 1]}");//物理表
      orderTableRuleConfig.setKeyGeneratorColumnName("order_id");
      return orderTableRuleConfig;
   }

   private static TableRuleConfiguration getOrderItemTableRuleConfiguration() {
      TableRuleConfiguration orderItemTableRuleConfig = new TableRuleConfiguration();
      orderItemTableRuleConfig.setLogicTable("t_order_item");
      orderItemTableRuleConfig.setActualDataNodes("orders_${0..1}.t_order_item_${[0, 1]}");
      return orderItemTableRuleConfig;
   }

   public void drop(DataSource dataSource) throws SQLException {
      execute(dataSource, "DROP TABLE IF EXISTS t_order");
      execute(dataSource, "DROP TABLE IF EXISTS t_order_item;");
   }

   public void create(DataSource dataSource) throws SQLException {
      execute(dataSource, "CREATE TABLE IF NOT EXISTS t_order (order_id BIGINT AUTO_INCREMENT, user_id INT NOT NULL, status VARCHAR(50), PRIMARY KEY (order_id ))");
      execute(dataSource, " CREATE TABLE IF NOT EXISTS t_order_item (order_item_id BIGINT AUTO_INCREMENT, order_id BIGINT, user_id INT NOT NULL, status VARCHAR(50), PRIMARY KEY (order_item_id));");
   }

   /***
    * 用户为中心 10 和11 偶数 和奇数
    * @param dataSource
    * @throws SQLException
    */
   public void insertData(DataSource dataSource) throws SQLException {
      for (int i = 1; i < 10; i++) {
         long orderId = executeAndGetGeneratedKey(dataSource, "INSERT INTO t_order (user_id, status) VALUES (10, 'INIT')");
         execute(dataSource, String.format("INSERT INTO t_order_item (order_id, user_id) VALUES (%d, 10)", orderId));
         orderId = executeAndGetGeneratedKey(dataSource, "INSERT INTO t_order (user_id, status) VALUES (11, 'INIT')");
         execute(dataSource, String.format("INSERT INTO t_order_item (order_id, user_id) VALUES (%d, 11)", orderId));
      }
   }

   private void select(DataSource dataSource, long orderId) throws SQLException {

      executeQuery(dataSource, String.format("SELECT * FROM t_order WHERE order_id=%d", orderId));

   }

   private void executeQuery(final DataSource dataSource, final String sql) throws SQLException {
      try (
              Connection conn = dataSource.getConnection();
              Statement statement = conn.createStatement()) {
         ResultSet resultSet = statement.executeQuery(sql);
         Long orderid = null, userid = null;
         String status = null;
         while (resultSet.next()) {
            orderid = resultSet.getLong("order_id");
            userid = resultSet.getLong("user_id");
            status = resultSet.getString("status");
            System.out.println(orderid + "," + userid + "," + status);
         }


      }
   }

   private void execute(final DataSource dataSource, final String sql) throws SQLException {
      try (
              Connection conn = dataSource.getConnection();
              Statement statement = conn.createStatement()) {
         statement.execute(sql);

      }
   }

   private long executeAndGetGeneratedKey(final DataSource dataSource, final String sql) throws SQLException {
      long result = -1;
      try (
              Connection conn = dataSource.getConnection();
              Statement statement = conn.createStatement()) {
         statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
         ResultSet resultSet = statement.getGeneratedKeys();
         if (resultSet.next()) {
            result = resultSet.getLong(1);
         }
      }
      return result;
   }
}
