package com.example.project.service;

import com.example.project.bot.TelegramBot;
import com.example.project.bot.TelegramService;
import com.example.project.dto.ApiResponse;
import com.example.project.dto.OrderDTO;
import com.example.project.entity.AttachmentContent;
import com.example.project.entity.Order;
import com.example.project.repository.AttachmentRepository;
import com.example.project.repository.OrderRepository;
import com.example.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author "ISMOIL NIGMATOV"
 * @created 2:49 PM on 11/15/2022
 * @project Project
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    private final AttachmentRepository attachmentRepository;

    private final UserRepository userRepository;

    private final TelegramBot telegramBot;

    private final TelegramService telegramService;

    @SneakyThrows
    public ApiResponse save(List<MultipartFile> files, String fromLanguage, String targetLanguage,String name, String email,String phone) {
        try {

            Order order = new Order();
            order.setFromLanguage(fromLanguage);
            order.setTargetLanguage(targetLanguage);
            order.setName(name);
            order.setEmail(email);
            order.setPhone(phone);

            List<AttachmentContent> attachmentContentList = new ArrayList<>();

            if (Objects.nonNull(files)) {
                for (MultipartFile file : files) {
                    AttachmentContent attachmentContent = new AttachmentContent();
                    attachmentContent.setFileName(file.getOriginalFilename());
                    attachmentContent.setContentType(file.getContentType());
                    attachmentContent.setSize(file.getSize());
                    attachmentContent.setBytes(file.getBytes());
                    attachmentContentList.add(attachmentContent);
                    attachmentRepository.save(attachmentContent);
                }
            }

            order.setAttachmentContent(attachmentContentList);
            Order save = orderRepository.save(order);

            telegramBot.execute(telegramService.sendOrder(save));
            log.info("order sent");
            try {
                if (!(files == null)) {
                    for (MultipartFile file : files) {
                        if (file.getContentType().startsWith("application"))
                            telegramBot.execute(telegramService.sendDocument(file));
                        if (file.getContentType().startsWith("image"))
                            telegramBot.execute(telegramService.sendPhoto(file));
                        if (file.getContentType().startsWith("video"))
                            telegramBot.execute(telegramService.sendVideo(file));
                    }
                }
            } catch (Exception e) {
                log.error(String.valueOf(e));
            }

            return ApiResponse.builder().success(true).message("Your application has been accepted and forwarded to our staff").build();
        }
        catch (Exception e){
            log.error(String.valueOf(e));
        }
        return ApiResponse.builder().success(false).build();
    }

    public ResponseEntity<?> download(Long id) {
        AttachmentContent attachmentContent = attachmentRepository.findById(id).orElseThrow(() -> new RuntimeException("Not Found"));


        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(attachmentContent.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachmentContent; filename=\"" + attachmentContent.getFileName() + "\"")
                .body(attachmentContent.getBytes());
        }
    }
