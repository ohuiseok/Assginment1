package com.coupon.assignment.strategy;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class FileStreamingProcessorFactory {

    private final List<FileStreamingProcessor> processors;
    private final Map<String, FileStreamingProcessor> processorMap = new HashMap<>();

    @Autowired
    public FileStreamingProcessorFactory(List<FileStreamingProcessor> processors) {
        this.processors = processors;
    }

    @PostConstruct
    public void init() {
        for (FileStreamingProcessor processor : processors) {
            String extension = processor.getSupportedExtension().toLowerCase();
            processorMap.put(extension, processor);
        }
    }

    /**
     * 파일 확장자에 해당하는 프로세서 반환
     */
    public Optional<FileStreamingProcessor> getProcessor(String fileExtension) {
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalizedExtension = fileExtension.toLowerCase().trim();
        FileStreamingProcessor processor = processorMap.get(normalizedExtension);

        return Optional.ofNullable(processor);
    }

    /**
     * 지원되는 파일 확장자 목록 반환
     */
    public List<String> getSupportedExtensions() {
        return processorMap.keySet().stream().sorted().toList();
    }

    /**
     * 파일 확장자 지원 여부 확인
     */
    public boolean isSupported(String fileExtension) {
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            return false;
        }

        String normalizedExtension = fileExtension.toLowerCase().trim();
        return processorMap.containsKey(normalizedExtension);
    }

}
