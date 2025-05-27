// 注释掉整个类
// public class RedisListProcessTask {

    // private final RedisTemplate<String, String> redisTemplate;
    // private final QinglongService qinglongService;
    // private final String baseUrl;

    // private static final String REDIS_KEY = "sf";
    // private static final int BATCH_SIZE = 5;
    // private static final String ENV_NAME = "sfsyUrl";
    // private final AtomicInteger currentBatchIndex = new AtomicInteger(0);

    // public RedisListProcessTask(
    //         RedisTemplate<String, String> redisTemplate,
    //         QinglongService qinglongService,
    //         @Value("${qinglong.url}") String baseUrl) {
    //     this.redisTemplate = redisTemplate;
    //     this.qinglongService = qinglongService;
    //     this.baseUrl = baseUrl;
    // }

    // @Scheduled(cron = "0 0 0 * * ?")
    // public void resetDailyCounter() {
    //     currentBatchIndex.set(0);
    //     log.info("每日凌晨重置批次计数器");
    // }

   // @Scheduled(cron = "0 */3 * * * *")
    // @Scheduled(cron = "0 0 * * * ?")
    // public void processRedisListData() {
     //   currentBatchIndex.set(0);
    //     try {
    //         Long listSize = redisTemplate.opsForList().size(REDIS_KEY);
    //         if (listSize == null || listSize == 0) {
    //             log.info("Redis列表为空，本次处理结束");
    //             return;
    //         }

    //         int startIndex = currentBatchIndex.get() * BATCH_SIZE;
    //         if (startIndex >= listSize) {
    //             log.info("当前批次索引{}已超过列表大小{}，等待明天重置", startIndex, listSize);
    //             return;
    //         }

    //         int endIndex = Math.min(startIndex + BATCH_SIZE - 1, listSize.intValue() - 1);
    //         log.info("开始处理第{}批次数据，索引范围：{} - {}", currentBatchIndex.get() + 1, startIndex, endIndex);

    //         List<String> batchData = Optional.ofNullable(
    //                         redisTemplate.opsForList().range(REDIS_KEY, startIndex, endIndex))
    //                 .orElse(Collections.emptyList());

    //         if (!batchData.isEmpty()) {
    //             processBatch(batchData);
    //             currentBatchIndex.incrementAndGet();
    //             log.info("完成第{}批次数据处理，处理数据量：{}", currentBatchIndex.get(), batchData.size());
    //         }

    //     } catch (Exception e) {
    //         log.error("处理Redis列表数据失败", e);
    //     }
    // }

    // private void processBatch(List<String> batchData) {
    //     if (batchData.isEmpty()) {
    //         return;
    //     }

    //     try {
    //         StringBuilder processedData = new StringBuilder();
    //         for (int i = 0; i < batchData.size(); i++) {
    //             String url = batchData.get(i)
    //                     .replaceAll("^\"|\"$", "")  // 移除首尾引号
    //                     .trim();
    //             processedData.append(url);
    //             if (i < batchData.size() - 1) {
    //                 processedData.append("\\n");  // 使用 \n 作为分隔符
    //             }
    //         }

    //         if (processedData.length() > 0) {
    //             qinglongService.updateEnv(ENV_NAME, processedData.toString(), "顺丰链接");
    //             log.info("成功更新顺丰链接环境变量，处理数据条数：{}", batchData.size());
    //             log.debug("更新的数据内容：\n{}", processedData);
    //         } else {
    //             log.warn("处理后的数据为空，跳过更新");
    //         }
    //     } catch (Exception e) {
    //         log.error("更新顺丰链接环境变量失败: {}", e.getMessage(), e);
    //     }
    // }
// }