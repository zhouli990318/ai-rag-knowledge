package com.silver.mcpserver.csdn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.mcpserver.domain.service.ArticlePublishService;
import com.silver.mcpserver.infrastructure.gateway.ICSDNService;
import com.silver.mcpserver.infrastructure.gateway.dto.ArticleRequestDTO;
import com.silver.mcpserver.infrastructure.gateway.dto.ArticleResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@SpringBootTest
public class ApiTest {


    @Autowired
    private ICSDNService csdnService;

    @Autowired
    private ArticlePublishService articlePublishService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void test_saveArticle() throws IOException {
        // 1. 构建请求对象
        ArticleRequestDTO request = new ArticleRequestDTO();
        request.setTitle("测试文章标题01");
        request.setMarkdowncontent("# 测试文章内容\n这是一篇测试文章");
        request.setContent("<h1>测试文章内容</h1><p>这是一篇测试文章</p>");
        request.setReadType("public");
        request.setLevel("0");
        request.setTags("测试,文章");
        request.setStatus(0);
        request.setCategories("后端");
        request.setType("original");
        request.setOriginal_link("");
        request.setAuthorized_status(true);
        request.setDescription("这是一篇测试文章的描述");
        request.setResource_url("");
        request.setNot_auto_saved("0");
        request.setSource("pc_mdeditor");
        request.setCover_images(Collections.emptyList());
        request.setCover_type(0);
        request.setIs_new(1);
        request.setVote_id(0);
        request.setResource_id("");
        request.setPubStatus("draft");
        request.setSync_git_code(0);

        // 2. 调用接口
        String cookie = "UN=weixin_55120064; uuid_tt_dd=10_9888162550-1716356373111-264677; Hm_ct_6bcd52f51e9b3dce32bec4a3997715ac=6525*1*10_9888162550-1716356373111-264677!5744*1*weixin_55120064; fid=20_57195807324-1723529161366-071792; c_dl_fpage=/download/Yao__Shun__Yu/14932325; c_dl_prid=1732694406524_218434; c_dl_rid=1732694450928_599339; c_dl_fref=https://download.csdn.net/download/Yao__Shun__Yu/14932325; c_dl_um=distribute.pc_relevant_download.none-task-download-2%7Edefault%7Ebaidujs%7Edefault-0-85509949-download-14932325.257%5Ev16%5Epc_dl_relevant_base1_a; csdn_newcert_weixin_55120064=1; c_segment=5; HMACCOUNT=3BF34FD1D56B5A87; dc_sid=5ed901b74ad3828a088873754809a708; _ga=GA1.2.1672505531.1681787321; _ga_7W1N0GEY1P=GS2.1.s1750124243$o31$g0$t1750124243$j60$l0$h0; FCNEC=%5B%5B%22AKsRol9b4UQucgcLmcO6pR-JBShGgFfyGGTqGuhvQ0X9dpen-RjRXGl7MtFi9-XljBBvZywnz5bgmsp8_1OJD9r0iIKq9s3HPKMBjdz6XYtCeP-J-LKu-WnwD0oQk8zZitPaB5PZ6_3RBBhaDQbOf_THj6RvqOkCOA%3D%3D%22%5D%5D; redpack_close_time=1; Hm_lvt_6bcd52f51e9b3dce32bec4a3997715ac=1749799202; creative_btn_mp=3; hide_login=1; _clck=7eayua%7C2%7Cfx1%7C0%7C1547; __gads=ID=b5fa180cb0da9fae:T=1749431778:RT=1750730533:S=ALNI_Ma6WGG6irAuPbvcDx01JME4gKBVqg; __gpi=UID=000010e8ce11d119:T=1749431778:RT=1750730533:S=ALNI_Mam9mn6rs53sdPa2L6cAaUs6u-r5g; __eoi=ID=7a4fb190108aaa8f:T=1738824563:RT=1750730533:S=AA-AfjZFTnWMURuZiCkDWHsXOx3J; loginbox_strategy=%7B%22taskId%22%3A317%2C%22abCheckTime%22%3A1750730485739%2C%22version%22%3A%22ExpA%22%2C%22nickName%22%3A%22%E5%A6%AF%E5%A8%8C%22%2C%22blog-threeH-dialog-expa%22%3A1750730530349%7D; dc_session_id=10_1750732741609.787446; c_ab_test=1; SESSION=8bf6bf54-4f85-4d9d-a7fc-18ec3770aebf; UserName=weixin_55120064; UserInfo=6c984d86651640d2a2bcf43fe192a787; UserToken=6c984d86651640d2a2bcf43fe192a787; UserNick=%E5%A6%AF%E5%A8%8C; AU=40E; BT=1750732764078; p_uid=U010000; c_first_ref=default; c_first_page=https%3A//mpbeta.csdn.net/%3Fspm%3D1000.2115.3001.10461; c_dsid=11_1750732806875.590431; c_ins_prid=-; c_ins_rid=1750732818369_437481; c_ins_fref=https://mpbeta.csdn.net/; c_ins_fpage=/index.html; c_ins_um=-; c_utm_source=636161750; utm_source=636161750; ins_first_time=1750732818833; x_inscode_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJjcmVkZW50aWFsIjoiIiwiY3NkblVzZXJuYW1lIjoid2VpeGluXzU1MTIwMDY0IiwidXNlcklkIjoiNjg1YTEwMTMyOTM3ZGU3ZGRiOTIzNzQ4IiwidXNlcm5hbWUiOiJ3ZWl4aW5fNTUxMjAwNjQifQ.z5Lc-w2NpTnDV2L21dQLBlloSXemDOe78VL5p8PfE9k; toolbar_remind_num=1; _clsk=1trffx5%7C1750732874712%7C1%7C0%7Cb.clarity.ms%2Fcollect; c-sidebar-collapse=0; creativeSetApiNew=%7B%22toolbarImg%22%3A%22https%3A//img-home.csdnimg.cn/images/20231011044944.png%22%2C%22publishSuccessImg%22%3A%22https%3A//img-home.csdnimg.cn/images/20240229024608.png%22%2C%22articleNum%22%3A0%2C%22type%22%3A0%2C%22oldUser%22%3Afalse%2C%22useSeven%22%3Atrue%2C%22oldFullVersion%22%3Afalse%2C%22userName%22%3A%22weixin_55120064%22%7D; c_pref=https%3A//mpbeta.csdn.net/mp_blog/creation/success/148866622; c_ref=https%3A//mpbeta.csdn.net/; c_page_id=default; Hm_lpvt_6bcd52f51e9b3dce32bec4a3997715ac=1750733143; log_Id_pv=22; log_Id_view=369; dc_tos=sycacr; log_Id_click=23";
        Call<ArticleResponseDTO> call = csdnService.saveArticle(request, cookie);
        Response<ArticleResponseDTO> response = call.execute();
        System.out.println("\r\n测试结果" + objectMapper.writeValueAsString(response));

        // 3. 验证结果
        if (response.isSuccessful()) {
            ArticleResponseDTO articleResponseDTO = response.body();
            log.info("发布文章成功 {}", articleResponseDTO);
        }
    }


}