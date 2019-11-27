package io.shardingsphere;



import io.shardingsphere.core.keygen.DefaultKeyGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Test {

   public static void main(String[] args) {
      final DefaultKeyGenerator defaultKeyGenerator=new DefaultKeyGenerator();//类锁 请求都创建一次
      final Map map=new ConcurrentHashMap();
      YmUtil.timeTasks(1000, 1, new Runnable() {
         @Override
         public void run() {
           // System.out.println(defaultKeyGenerator.generateKey());
            map.put(defaultKeyGenerator.generateKey(),defaultKeyGenerator.generateKey());;
         }
      });
      System.out.println(map.size());


   }
}
