package com.volunteer.system.controller;

import com.volunteer.system.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件上传与存储模块集成测试 (已集成身份验证绕过)
 *
 * 上传目录显式指到 target/test-uploads：以前用例会把测试文件写进真实的 files/ 目录，
 * 每跑一次测试就多几个残留 png，和用户上传的文件混在一起。
 * 现在上传根目录是配置项（app.upload.dir），测试用独立目录，mvn clean 一并清掉。
 */
@SpringBootTest(properties = "app.upload.dir=target/test-uploads")
@AutoConfigureMockMvc
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        // 模拟 JWT 拦截器通过逻辑
        Claims claims = mock(Claims.class);
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
    }

    @Test
    @DisplayName("场景1：合法图片上传 - 验证路径分发逻辑")
    void uploadImage_Success() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", 
                "test.png", 
                MediaType.IMAGE_PNG_VALUE, 
                "png-data".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                .file(mockFile)
                .header("Authorization", "valid-token") // 注入 Header
                .param("type", "activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("/files/activity/")));
    }

    @Test
    @DisplayName("场景2：非法文件上传拦截 - 验证安全过滤")
    void uploadFile_InvalidType() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", 
                "danger.txt", 
                MediaType.TEXT_PLAIN_VALUE, 
                "text-content".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                .file(mockFile)
                .header("Authorization", "valid-token"))
                .andExpect(status().isBadRequest()) // 🚨 修改为 400
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("仅允许上传图片文件"));
    }

    @Test
    @DisplayName("场景3：空文件上传 - 验证鲁棒性")
    void uploadFile_Empty() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", 
                "empty.jpg", 
                MediaType.IMAGE_JPEG_VALUE, 
                new byte[0]
        );

        mockMvc.perform(multipart("/api/file/upload")
                .file(mockFile)
                .header("Authorization", "valid-token"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("场景4：路径穿越防护 - type=../../ 被白名单收敛到 common，不会跳出上传根目录")
    void uploadFile_PathTraversalInTypeIsNeutralized() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "evil.png",
                MediaType.IMAGE_PNG_VALUE,
                "png-data".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(mockFile)
                        .header("Authorization", "valid-token")
                        .param("type", "../../../tmp"))     // 旧实现会把它直接拼进磁盘路径
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(
                        org.hamcrest.Matchers.containsString("/files/common/")))
                .andExpect(jsonPath("$.data").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(".."))));
    }

    @Test
    @DisplayName("场景5：扩展名白名单 - 伪装成图片的 .jsp 被拒绝")
    void uploadFile_DisallowedExtensionIsRejected() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "shell.jsp",
                MediaType.IMAGE_PNG_VALUE,   // Content-Type 可以随便伪造
                "jsp-code".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(mockFile)
                        .header("Authorization", "valid-token")
                        .param("type", "avatar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("场景6：文件无扩展名时返回 400 而不是 500")
    void uploadFile_WithoutExtension() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "noextension",
                MediaType.IMAGE_PNG_VALUE,
                "png-data".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(mockFile)
                        .header("Authorization", "valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
