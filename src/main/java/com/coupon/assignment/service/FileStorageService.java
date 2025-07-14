package com.coupon.assignment.service;

import com.coupon.assignment.domain.FileDomain;
import com.coupon.assignment.entity.FileMeta;
import com.coupon.assignment.repository.FileMetaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final FileMetaRepository fileMetaRepository;
    @Value("${file.dir}")
    private String fileDir;
    private final String HEADER_NAME = "customer_id";

    @Transactional
    public FileDomain store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("file.notExist.fail");
        }

        isFileCheck(HEADER_NAME, file);
        String newFileName = UUID.randomUUID() + "." + StringUtils.getFilenameExtension(file.getOriginalFilename());

        try {
            File dest = new File(fileDir + newFileName);
            file.transferTo(dest);
        } catch (IOException e) {
            throw new IllegalArgumentException("file.invalid.fail");
        }

        FileMeta storedFileMeta = fileMetaRepository.save(
                FileMeta.builder()
                        .fileId(UUID.randomUUID().toString())
                        .originalFileName(file.getOriginalFilename())
                        .storedFileName(newFileName)
                        .contentType(file.getContentType())
                        .status(false)
                        .uploadTime(LocalDateTime.now())
                        .build()
        );

        return FileDomain.fromEntity(storedFileMeta);
    }

    @Transactional
    public FileDomain retrieve(String fileId) {
        FileMeta fileInfo = fileMetaRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("file.notExist.fail"));

        Resource resource = new FileSystemResource(fileDir + fileInfo.getStoredFileName());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("file.invalid.fail");
        }

        fileInfo.setDownloadTime(LocalDateTime.now());

        return FileDomain.fromResource(resource, fileInfo);

    }

    private void isFileCheck(String headerName, MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        switch (Objects.requireNonNull(extension)) {
            case "xlsx", "xls" -> isExcelFileDataCheck(headerName, file);
            case "csv" -> isCsvFileDataCheck(headerName, file);
            default -> throw new IllegalArgumentException("file.invalid.fail");
        }
        ;
    }

    private void isCsvFileDataCheck(String headerName, MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream())) {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader(headerName));
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            if (headerMap == null || !headerMap.containsKey(headerName)) {
                throw new IllegalArgumentException("");
            }

            for (CSVRecord record : csvParser) {
                if (record.getRecordNumber() == 1 && record.isConsistent()) {
                    continue;
                }
                String customerId = record.get(headerName);
                if (ObjectUtils.isEmpty(customerId)) {
                    throw new IllegalArgumentException("");
                }
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("CSV 파일 파싱 중 오류가 발생했습니다.");
        }
    }

    private void isExcelFileDataCheck(String headerName, MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                throw new IllegalArgumentException("Excel 파일이 비어있습니다.");
            }

            Row headerRow = rowIterator.next();
            Cell headerCell = headerRow.getCell(0);
            if (headerCell == null || !headerName.equalsIgnoreCase(headerCell.getStringCellValue())) {
                throw new IllegalArgumentException("");
            }


            while (rowIterator.hasNext()) {
                Row currentRow = rowIterator.next();
                Cell customerIdCell = currentRow.getCell(0);

                if (customerIdCell != null) {
                    String customerId;
                    switch (customerIdCell.getCellType()) {
                        case STRING:
                            customerId = customerIdCell.getStringCellValue().trim();
                            break;
                        case NUMERIC:
                            customerId = String.valueOf((long) customerIdCell.getNumericCellValue()).trim();
                            break;
                        default:
                            customerId = "";
                            break;
                    }

                    if (customerId.isEmpty()) {
                        throw new IllegalArgumentException("");
                    }
                }
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("");
        }
    }

}