package com.valorburst.service.impl;

import com.valorburst.model.local.User;
import com.valorburst.repository.local.UserRepository;
import com.valorburst.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportServiceImpl implements ExcelExportService {

    private final UserRepository userRepository;

    @Override
    public File exportRecords(LocalDate start, LocalDate end) throws IOException {
        List<User> records = userRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("收益数据");

        // 表头
        String[] headers = {
                "用户昵称", "业务ID", "Tiktok账号", "视频ID", "关键词", "播放量", "奖励金额", 
                "发布时间", "是否有效", "是否奖励", "是否已结算", "备注", "平台", "视频链接"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // 数据行
        int rowIdx = 1;
        for (User r : records) {
            Row row = sheet.createRow(rowIdx++);

            // row.createCell(0).setCellValue(r.getAgentName() != null ? r.getAgentName() : "");
            // row.createCell(1).setCellValue(r.getMyBusinessId() != null ? r.getMyBusinessId() : "");
            // row.createCell(2).setCellValue(r.getTiktokAccount() != null ? r.getTiktokAccount() : "");
            // row.createCell(3).setCellValue(r.getVideoId() != null ? r.getVideoId() : "");
            // row.createCell(4).setCellValue(r.getGuanjianziName() != null ? r.getGuanjianziName() : "");
            // row.createCell(5).setCellValue(r.getPlayCount() != null ? r.getPlayCount() : 0);
            // row.createCell(6).setCellValue(r.getReward() != null ? r.getReward() : 0);
            // row.createCell(7).setCellValue(r.getFabuDate() != null ? r.getFabuDate().toString() : "");
            // row.createCell(8).setCellValue(r.getIsValid() != null && r.getIsValid() ? "是" : "否");
            // row.createCell(9).setCellValue(r.getIsCandidate() != null && r.getIsCandidate() ? "是" : "否");
            // row.createCell(10).setCellValue(r.getIsSettled() != null && r.getIsSettled() ? "是" : "否");
            // row.createCell(11).setCellValue(r.getRemarks() != null ? r.getRemarks() : "");
            // row.createCell(12).setCellValue(r.getPingtai() != null ? r.getPingtai() : "");
            // row.createCell(13).setCellValue(r.getWorksUrl() != null ? r.getWorksUrl() : "");
        }

        // 自动宽度
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 保存为临时文件
        String filename = String.format("收益明细_%s_%s.xlsx",
                start.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                end.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        File tempFile = File.createTempFile("report_", filename);
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            workbook.write(out);
        }
        workbook.close();

        return tempFile;
    }
}
