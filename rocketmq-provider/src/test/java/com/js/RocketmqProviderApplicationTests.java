package com.js;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RocketmqProviderApplicationTests {

    @Test
    void contextLoads() {
    }


    static String baseSql = "INSERT INTO `fund_mgmt_datapush_task` (`CHANNEL`,`SCENE`,`FUND_CODE`,`GUARANTEE_CODE`,`TASK_CONFIG_ID`,`TASK_ID`,`NAME`,`PARAM`,`PUSH_STATUS`,`PUSH_FINISHED_TIME`,`DESCRIPTION`,`CREATE_TIME`,`UPDATE_TIME`,`DEL_FLAG`) VALUES ('weibo','weibo_cashloan','QINNONG-BK','','10000000000000000012','";
    //            "PT583502778180653056" +
    static String midSql = "','秦农担保方数据推送','{\"guaranteeContractType\":\"entrustGuaranteeAgreementQinNongBk\",\"pushDate\":\"2022-03-22 18:00:14\",\"pushDateContent\":\"0 0 13 * * ?\",\"pushDateType\":\"cron\",\"targetDate\":\"";
    static String finshSql = "\"}',0,now(),'',now(),now(),0);";

    public static void main(String[] args) {
        Snowflake snowflake = IdUtil.getSnowflake(1, 1);
        String beginDate = "2020-12-20 00:00:00";
        String endDate = "2022-03-04 00:00:00";
        String targetDate = beginDate;
        while (DateUtil.parse(targetDate,"yyyy-MM-dd HH:mm:ss").before(DateUtil.parse(endDate,"yyyy-MM-dd HH:mm:ss"))) {
            System.out.println(baseSql + "PT" + snowflake.nextIdStr() + midSql + targetDate + finshSql);
            targetDate = DateFormatUtils.format(DateUtils.addDays(DateUtil.parse(targetDate,"yyyy-MM-dd HH:mm:ss"),1),"yyyy-MM-dd HH:mm:ss");
        }
    }

}
