package com.coupon.assignment.service;

import com.coupon.assignment.domain.FileDomain;
import com.coupon.assignment.entity.FileMeta;
import com.coupon.assignment.repository.FileMetaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.hssf.eventusermodel.*;
import org.apache.poi.hssf.record.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final FileMetaRepository fileMetaRepository;
    
    @Value("${file.dir}")
    private String fileDir;
    
    private final String HEADER_NAME = "customer_id";

    @Transactional
    public FileDomain store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("file.notExist.fail");
        }

        // 1. 기본 파일 정보 검증
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (!isValidFileExtension(extension)) {
            throw new IllegalArgumentException("file.invalid.fail");
        }

        // 2. 파일 크기 검증 (메모리 로드 전 사전 검증)
        validateFileSize(file, extension);

        // 3. 파일을 임시 저장 (메모리 로드하지 않고 디스크에 직접 저장)
        String newFileName = UUID.randomUUID() + "." + extension;
        File tempFile = new File(fileDir + newFileName);
        
        try {
            file.transferTo(tempFile);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw e;
        }

//        // 4. 저장된 파일에 대해 스트리밍 검증 수행
//        try {
//            validateSavedFileStreaming(tempFile, HEADER_NAME, extension);
//        } catch (Exception e) {
//            // 검증 실패 시 임시 파일 삭제
//            if (tempFile.exists()) {
//                tempFile.delete();
//            }
//            throw e;
//        }

        // 5. 검증 완료 후 DB에 메타데이터 저장
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

    private boolean isValidFileExtension(String extension) {
        return extension != null && 
               ("xlsx".equalsIgnoreCase(extension) || 
                "xls".equalsIgnoreCase(extension) || 
                "csv".equalsIgnoreCase(extension));
    }

    private void validateFileSize(MultipartFile file, String extension) {
        long maxSize;
        switch (extension.toLowerCase()) {
            case "xlsx":
                maxSize = 1024 * 1024 * 1024L; // 1GB
                break;
            case "xls":
                maxSize = 500 * 1024 * 1024L; // 500MB
                break;
            case "csv":
                maxSize = 200 * 1024 * 1024L; // 200MB
                break;
            default:
                maxSize = 100 * 1024 * 1024L; // 100MB
        }
        
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("파일 크기가 너무 큽니다. 최대 허용 크기: " + (maxSize / 1024 / 1024) + "MB");
        }
    }

    private void validateSavedFileStreaming(File file, String headerName, String extension) {
        try (InputStream inputStream = new FileInputStream(file)) {
            switch (extension.toLowerCase()) {
                case "xlsx":
                    validateXlsxHeaderOnly(inputStream, headerName);
                    break;
                case "xls":
                    validateXlsHeaderOnly(inputStream, headerName);
                    break;
                case "csv":
                    validateCsvHeaderStreaming(inputStream, headerName);
                    break;
                default:
                    throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("파일 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void validateXlsxHeaderOnly(InputStream inputStream, String expectedHeader) {
        try {
            OPCPackage opcPackage = OPCPackage.open(inputStream);
            XSSFReader xssfReader = new XSSFReader(opcPackage);
            
            // 첫 번째 시트만 검증
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
            if (sheets.hasNext()) {
                try (InputStream sheetStream = sheets.next()) {
                    XMLReader xmlReader = XMLReaderFactory.createXMLReader();
                    
                    HeaderValidationHandler handler = new HeaderValidationHandler(expectedHeader);
                    xmlReader.setContentHandler(handler);
                    
                    org.xml.sax.InputSource inputSource = new org.xml.sax.InputSource(sheetStream);
                    try {
                        xmlReader.parse(inputSource);
                    } catch (SAXException e) {
                        if (!"EARLY_TERMINATION".equals(e.getMessage())) {
                            throw e;
                        }
                    }
                    
                    if (!handler.isHeaderValid()) {
                        throw new IllegalArgumentException("헤더가 일치하지 않습니다. 예상: " + expectedHeader + ", 실제: " + handler.getActualHeader());
                    }
                    
                    if (!handler.hasValidData()) {
                        throw new IllegalArgumentException("유효한 데이터가 없습니다.");
                    }
                }
            }
            
            opcPackage.close();
            
        } catch (Exception e) {
            throw new IllegalArgumentException("XLSX 파일 헤더 검증 실패: " + e.getMessage());
        }
    }

    private void validateXlsHeaderOnly(InputStream inputStream, String expectedHeader) {
        try {
            POIFSFileSystem fs = new POIFSFileSystem(inputStream);
            
            HeaderValidationXlsListener listener = new HeaderValidationXlsListener(expectedHeader);
            MissingRecordAwareHSSFListener missingRecordListener = new MissingRecordAwareHSSFListener(listener);
            FormatTrackingHSSFListener formatListener = new FormatTrackingHSSFListener(missingRecordListener);
            
            HSSFEventFactory factory = new HSSFEventFactory();
            HSSFRequest request = new HSSFRequest();
            request.addListenerForAllRecords(formatListener);
            
            factory.processWorkbookEvents(request, fs);
            
            if (!listener.isHeaderValid()) {
                throw new IllegalArgumentException("헤더가 일치하지 않습니다. 예상: " + expectedHeader);
            }
            
            if (!listener.hasValidData()) {
                throw new IllegalArgumentException("유효한 데이터가 없습니다.");
            }
            
            fs.close();
            
        } catch (Exception e) {
            throw new IllegalArgumentException("XLS 파일 헤더 검증 실패: " + e.getMessage());
        }
    }

    private void validateCsvHeaderStreaming(InputStream inputStream, String headerName) {
        try (Reader reader = new InputStreamReader(inputStream)) {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            
            if (!csvParser.getHeaderMap().containsKey(headerName)) {
                throw new IllegalArgumentException("헤더가 일치하지 않습니다. 예상: " + headerName);
            }
            
            // 첫 번째 레코드만 검증
            Iterator<CSVRecord> iterator = csvParser.iterator();
            if (iterator.hasNext()) {
                CSVRecord record = iterator.next();
                String value = record.get(headerName);
                if (ObjectUtils.isEmpty(value)) {
                    throw new IllegalArgumentException("유효한 데이터가 없습니다.");
                }
            }
            
        } catch (IOException e) {
            throw new IllegalArgumentException("CSV 파일 검증 실패: " + e.getMessage());
        }
    }

    private static class HeaderValidationHandler extends DefaultHandler {
        private final String expectedHeader;
        private String actualHeader = "";
        private String currentCellValue = "";
        private boolean isFirstRow = true;
        private boolean isFirstCell = true;
        private boolean headerValid = false;
        private boolean hasValidData = false;
        private int rowCount = 0;
        
        public HeaderValidationHandler(String expectedHeader) {
            this.expectedHeader = expectedHeader;
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if ("row".equals(qName)) {
                rowCount++;
                if (rowCount > 2) {
                    // 헤더와 첫 번째 데이터 행만 검증하면 충분
                    throw new SAXException("EARLY_TERMINATION"); // 조기 종료
                }
            } else if ("c".equals(qName)) {
                String cellRef = attributes.getValue("r");
                isFirstCell = cellRef != null && cellRef.startsWith("A");
                currentCellValue = "";
            } else if ("v".equals(qName) || "t".equals(qName)) {
                currentCellValue = "";
            }
        }
        
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            currentCellValue += new String(ch, start, length);
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("row".equals(qName)) {
                if (isFirstRow) {
                    isFirstRow = false;
                    // 헤더 검증
                    headerValid = expectedHeader.equalsIgnoreCase(actualHeader.trim());
                }
            } else if ("c".equals(qName) && isFirstCell) {
                if (isFirstRow && currentCellValue != null && !currentCellValue.trim().isEmpty()) {
                    actualHeader = currentCellValue.trim();
                } else if (!isFirstRow && currentCellValue != null && !currentCellValue.trim().isEmpty()) {
                    hasValidData = true;
                    throw new SAXException("EARLY_TERMINATION"); // 데이터 확인 완료, 조기 종료
                }
            }
        }
        
        public boolean isHeaderValid() {
            return headerValid;
        }
        
        public boolean hasValidData() {
            return hasValidData;
        }
        
        public String getActualHeader() {
            return actualHeader;
        }
    }

    private static class HeaderValidationXlsListener implements HSSFListener {
        private final String expectedHeader;
        private boolean headerValid = false;
        private boolean hasValidData = false;
        private int currentRow = 0;
        private boolean isFirstRow = true;
        private SSTRecord sstRecord;
        
        public HeaderValidationXlsListener(String expectedHeader) {
            this.expectedHeader = expectedHeader;
        }
        
        @Override
        public void processRecord(org.apache.poi.hssf.record.Record record) {
            switch (record.getSid()) {
                case SSTRecord.sid:
                    sstRecord = (SSTRecord) record;
                    break;
                    
                case RowRecord.sid:
                    RowRecord rowRecord = (RowRecord) record;
                    currentRow = rowRecord.getRowNumber();
                    if (currentRow > 1) {
                        // 헤더와 첫 번째 데이터 행만 검증
                        return;
                    }
                    break;
                    
                case LabelSSTRecord.sid:
                    LabelSSTRecord labelRecord = (LabelSSTRecord) record;
                    if (labelRecord.getColumn() == 0) { // 첫 번째 컬럼만
                        if (currentRow == 0 && sstRecord != null) {
                            // 헤더 검증
                            String headerValue = sstRecord.getString(labelRecord.getSSTIndex()).getString();
                            headerValid = expectedHeader.equalsIgnoreCase(headerValue.trim());
                            isFirstRow = false;
                        } else if (currentRow == 1) {
                            // 첫 번째 데이터 행 확인
                            hasValidData = true;
                        }
                    }
                    break;
                    
                case NumberRecord.sid:
                    NumberRecord numberRecord = (NumberRecord) record;
                    if (numberRecord.getColumn() == 0 && currentRow == 1) {
                        hasValidData = true;
                    }
                    break;
            }
        }
        
        public boolean isHeaderValid() {
            return headerValid;
        }
        
        public boolean hasValidData() {
            return hasValidData;
        }
    }
}
