package io.shardingsphere;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class 抽奖活动 {


    static int num=1;//多少个人获奖
    public static void main(String[] args) {
        Set<Integer> set=new HashSet<>();
        for(;;)
        {
            if(set.size()==num) {
                break;
            }
            int random = new SecureRandom().nextInt(99);
            set.add(random);
        }
        Iterator<Integer> iterator = set.iterator();
        System.out.println("------------源码学院10月VIP抽奖活动直播开奖------------");
        System.out.println("开中奖名单:");
        while (iterator.hasNext()){
            System.out.printf("获得是第二等奖中奖序号:%s\r\n",iterator.next());

        }
        System.out.println("恭喜中奖的朋友，请和班主任联系");

    }



}
