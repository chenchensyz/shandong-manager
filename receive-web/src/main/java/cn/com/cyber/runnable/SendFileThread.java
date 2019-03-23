package cn.com.cyber.runnable;

import cn.com.cyber.fileUpload.FileUploadFile;
import cn.com.cyber.fileUpload.UploadClient;
import cn.com.cyber.socket.SpringUtil;
import cn.com.cyber.util.CodeEnv;
import cn.com.cyber.util.CodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.Set;

public class SendFileThread implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendFileThread.class);

    @Override
    public void run() {
        JedisPool jedisPool = SpringUtil.getBean(JedisPool.class);
        Jedis jedis = jedisPool.getResource();
        CodeEnv codeEnv = SpringUtil.getBean(CodeEnv.class);
        try {
            while (true) {
                Set<String> keys = jedis.keys(CodeUtil.FILE_JEDIS_PREFIX + "*");
                if (keys != null && keys.size() > 0) {
                    for (String key : keys) {
                        try {
                            Map<String, String> map = jedis.hgetAll(key);
                            if (Integer.valueOf(map.get("state")) < 2 && Integer.valueOf(map.get("times")) < 3) {
                                FileUploadFile uploadFile = new FileUploadFile();
                                uploadFile.setFilePath(map.get("filePath"));
                                uploadFile.setFileName((map.get("fileName")));
                                uploadFile.setIntroduction(map.get("introduction"));
                                uploadFile.setUuid(map.get("uuid"));
                                uploadFile.setUpUrl(map.get("upUrl"));
                                uploadFile.setFileSize(Integer.valueOf(map.get("fileSize")));
                                MyThreadPool.getThreadPool().execute(new UploadClient(codeEnv.getFile_sever_port(), codeEnv.getSocket_url(), uploadFile));
                                map.put("state", "1");
                                jedis.hmset(key, map);  //修改状态，保存到redis
                                jedis.expire(key, 604800);
                                int i = 0;
                                long maxTime = (uploadFile.getFileSize() / 1000000) * 30 * 1000;
                                long defaultTime = 3 * 60 * 1000;
                                if (maxTime < defaultTime) {
                                    maxTime = defaultTime;
                                }
                                LOGGER.info("等待时间:{}", maxTime);
                                Map<String, String> success;
                                do {
                                    success = jedis.hgetAll(key);
                                    Thread.sleep(500);
                                    i += 500;
                                } while (!"2".equals(success.get("state")) && i < maxTime);
                                LOGGER.info("结束循环i:{}", i);
                                Integer times = Integer.valueOf(success.get("times")) + 1;
                                success.put("times", times + "");
                                jedis.hmset(key, success);
                            } else {
                                map.put("state", "3"); //上传失败
                                jedis.hmset(key, map);
                            }
                        } catch (NumberFormatException ne) {
                            LOGGER.error("解析异常:{}", key);
                            continue;
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                            continue;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
    }

    public static void main(String[] args) {
        int size = 5453908;
        System.out.println(size / 1000000);
    }
}
