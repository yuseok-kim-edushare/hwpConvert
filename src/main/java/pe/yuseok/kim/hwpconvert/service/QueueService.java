package pe.yuseok.kim.hwpconvert.service;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import pe.yuseok.kim.hwpconvert.model.ConversionTask;

@Slf4j
@Service
public class QueueService {

    private static final String TASK_QUEUE_KEY = "conversion:task:queue";
    private static final String TASK_KEY_PREFIX = "conversion:task:";
    private static final String FILE_PATH_KEY_PREFIX = "conversion:file:";
    
    private final RedisTemplate<String, ConversionTask> conversionTaskRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ConversionService conversionService;

    public QueueService(
            RedisTemplate<String, ConversionTask> conversionTaskRedisTemplate,
            RedisTemplate<String, Object> redisTemplate,
            @Lazy ConversionService conversionService) {
        this.conversionTaskRedisTemplate = conversionTaskRedisTemplate;
        this.redisTemplate = redisTemplate;
        this.conversionService = conversionService;
    }

    public void enqueueTask(ConversionTask task) {
        String taskKey = TASK_KEY_PREFIX + task.getId();
        
        // Store task details
        conversionTaskRedisTemplate.opsForValue().set(taskKey, task);
        
        // Set expiration (7 days)
        conversionTaskRedisTemplate.expire(taskKey, 7, TimeUnit.DAYS);
        
        // Add to queue
        redisTemplate.opsForList().rightPush(TASK_QUEUE_KEY, task.getId());
        
        log.info("Task enqueued: {}", task.getId());
    }
    
    public ConversionTask getTask(String taskId) {
        return conversionTaskRedisTemplate.opsForValue().get(TASK_KEY_PREFIX + taskId);
    }
    
    public void updateTask(ConversionTask task) {
        String taskKey = TASK_KEY_PREFIX + task.getId();
        conversionTaskRedisTemplate.opsForValue().set(taskKey, task);
    }
    
    public void storeFilePath(String taskId, String filePath) {
        String filePathKey = FILE_PATH_KEY_PREFIX + taskId;
        redisTemplate.opsForValue().set(filePathKey, filePath);
        redisTemplate.expire(filePathKey, 7, TimeUnit.DAYS);
    }
    
    public String getFilePath(String taskId) {
        String filePathKey = FILE_PATH_KEY_PREFIX + taskId;
        Object filePath = redisTemplate.opsForValue().get(filePathKey);
        return filePath != null ? filePath.toString() : null;
    }
    
    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    public void processQueue() {
        String taskId = (String) redisTemplate.opsForList().leftPop(TASK_QUEUE_KEY);
        
        if (taskId != null) {
            ConversionTask task = getTask(taskId);
            
            if (task != null && "PENDING".equals(task.getStatus())) {
                log.info("Processing task: {}", taskId);
                conversionService.processTask(task);
            }
        }
    }
} 