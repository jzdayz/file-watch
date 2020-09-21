package com.wonders;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings({"SpringJavaAutowiredFieldsWarningInspection", "SpringJavaInjectionPointsAutowiringInspection", "ResultOfMethodCallIgnored"})
@Service
@Slf4j
@EnableConfigurationProperties(WfProperties.class)
public class WFJob {

    @Autowired
    private WfProperties wfProperties;

    @Autowired
    private List<Exclude> excludeList;

    private static final String FILE_LOCK_NAME = "FILE_LOCK_NAME";

    @PostConstruct
    public void check(){
        if (!Files.exists(Paths.get(wfProperties.getPath()))){
            log.error("无法监控的path:{}",wfProperties.getPath());
            System.exit(1);
        }
    }


    /**
     * 单线程去删除不必须的数据库备份
     * 2020-09-19 05:00:00
     * 2020-09-20 05:00:00
     * 2020-09-21 05:00:00
     */
    @Scheduled(cron = "0 0 5 1/1 * ?")
    public void wf() throws Exception {

        lock();
        try {
            String path = wfProperties.getPath();
            File file = new File(path);
            if (!file.exists()) {
                log.error("文件不存在!!! {}", path);
                System.exit(1);
            }
            List<File> deleteFile = new ArrayList<>();

            Arrays.stream(Objects.requireNonNull(file.listFiles((dir, name) -> !excludeFile(name))))
                    .collect(Collectors.groupingBy(k -> extractTableName(k.getName())))
                    .forEach((g, files) -> {
                        // 根据时间戳倒序
                        List<File> rs = files.stream().sorted((a, b) -> {
                            String aT = extractTimestamp(a.getName());
                            String bT = extractTimestamp(b.getName());
                            return Long.compare(Long.parseLong(bT), Long.parseLong(aT));
                        }).collect(Collectors.toList());

                        if (rs.size() > wfProperties.getSavedDays()) {
                            deleteFile.addAll(rs.subList(wfProperties.getSavedDays(), rs.size()));
                        }
                    });
            log.info("starting delete file => {}", deleteFile);
            deleteFile.forEach(File::delete);
            log.info("delete success");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            unlock();
        }
    }

    // TBL_PEOPLEINFO_20200918.sql
    @SuppressWarnings("SpellCheckingInspection")
    private String extractTableName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("_"));
    }

    // TBL_PEOPLEINFO_20200918.sql
    @SuppressWarnings("SpellCheckingInspection")
    private String extractTimestamp(String fileName) {
        return fileName.substring(fileName.lastIndexOf("_") + 1, fileName.lastIndexOf("."));
    }

    private void unlock() {
        File file = new File(wfProperties.getPath() + File.separator + FILE_LOCK_NAME);
        file.delete();
    }

    private void lock() throws IOException {
        File file = new File(wfProperties.getPath() + File.separator + FILE_LOCK_NAME);
        file.createNewFile();
    }

    @Bean
    public Exclude exclude1() {
        return (name) -> {
            if (name.startsWith(".")) {
                return true;
            }
            if (FILE_LOCK_NAME.equals(name)) {
                return true;
            }
            return false;
        };
    }

    private boolean excludeFile(String file) {

        if (CollectionUtils.isEmpty(excludeList)) {
            return false;
        }

        for (Exclude exclude : excludeList) {
            if (exclude.exclude(file)) {
                return true;
            }
        }
        return false;
    }

}
