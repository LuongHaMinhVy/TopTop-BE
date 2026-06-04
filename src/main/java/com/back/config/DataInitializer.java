package com.back.config;

import com.back.report.model.entity.ReportReason;
import com.back.report.model.enums.ReportReasonType;
import com.back.report.repo.IReportReasonRepo;
import com.back.user.model.entity.Role;
import com.back.user.model.enums.RoleName;
import com.back.user.repo.IRoleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final IRoleRepo roleRepo;

    @Bean
    CommandLineRunner initRoles() {
        return args -> {
            if (roleRepo.count() == 0) {
                roleRepo.save(Role.builder()
                        .name(RoleName.ROLE_USER)
                        .description("Default user role")
                        .build());

                roleRepo.save(Role.builder()
                        .name(RoleName.ROLE_ADMIN)
                        .description("Administrator role")
                        .build());

                roleRepo.save(Role.builder()
                        .name(RoleName.ROLE_MODERATOR)
                        .description("Moderator role")
                        .build());
            }
        };
    }

    @Bean
    CommandLineRunner initReportReasons(IReportReasonRepo reasonRepo) {
        return args -> {
            if (reasonRepo.count() == 0) {

                // =========================
                // 1. Nudity / Sexual content
                // =========================
                ReportReason nudity = reasonRepo.save(ReportReason.builder()
                        .code("NUDITY_SEXUAL_CONTENT")
                        .labelEn("Nudity or sexual content")
                        .labelVi("Hình ảnh khỏa thân hoặc nội dung tình dục")
                        .type(ReportReasonType.NUDITY_SEXUAL_CONTENT)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("MINOR_SEXUAL_EXPLOITATION")
                        .labelEn("Minor sexual exploitation")
                        .labelVi("Bóc lột hoặc lạm dụng tình dục người dưới 18 tuổi")
                        .policyTextEn("Shows, promotes, or requests sexual exploitation involving minors")
                        .policyTextVi("Hiển thị, cổ xúy hoặc yêu cầu nội dung bóc lột tình dục liên quan đến người dưới 18 tuổi")
                        .type(ReportReasonType.NUDITY_SEXUAL_CONTENT)
                        .parent(nudity)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("ADULT_NUDITY")
                        .labelEn("Adult nudity")
                        .labelVi("Khỏa thân người lớn")
                        .policyTextEn("Shows exposed private body parts or explicit nudity")
                        .policyTextVi("Hiển thị bộ phận nhạy cảm hoặc hình ảnh khỏa thân rõ ràng")
                        .type(ReportReasonType.NUDITY_SEXUAL_CONTENT)
                        .parent(nudity)
                        .sortOrder(2)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("SEXUAL_ACTIVITY")
                        .labelEn("Sexual activity")
                        .labelVi("Hoạt động tình dục")
                        .policyTextEn("Shows, describes, or promotes explicit sexual activity")
                        .policyTextVi("Hiển thị, mô tả hoặc cổ xúy hoạt động tình dục rõ ràng")
                        .type(ReportReasonType.NUDITY_SEXUAL_CONTENT)
                        .parent(nudity)
                        .sortOrder(3)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("SEXUAL_SOLICITATION")
                        .labelEn("Sexual solicitation")
                        .labelVi("Gạ gẫm tình dục")
                        .policyTextEn("Requests, offers, or arranges sexual services or sexual contact")
                        .policyTextVi("Yêu cầu, đề nghị hoặc sắp xếp dịch vụ/hành vi tình dục")
                        .type(ReportReasonType.NUDITY_SEXUAL_CONTENT)
                        .parent(nudity)
                        .sortOrder(4)
                        .active(true)
                        .build());


                // =========================
                // 2. Violence / Abuse / Exploitation
                // =========================
                ReportReason violence = reasonRepo.save(ReportReason.builder()
                        .code("VIOLENCE_ABUSE_EXPLOITATION")
                        .labelEn("Violence, abuse, and criminal exploitation")
                        .labelVi("Bạo lực, lạm dụng và bóc lột để phạm tội")
                        .type(ReportReasonType.VIOLENCE_ABUSE_EXPLOITATION)
                        .sortOrder(2)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("PHYSICAL_ABUSE")
                        .labelEn("Physical abuse")
                        .labelVi("Bạo hành thể chất")
                        .policyTextEn("Shows or promotes hitting, beating, torture, or physical abuse")
                        .policyTextVi("Hiển thị hoặc cổ xúy đánh đập, tra tấn hoặc bạo hành thể chất")
                        .type(ReportReasonType.VIOLENCE_ABUSE_EXPLOITATION)
                        .parent(violence)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("ANIMAL_ABUSE")
                        .labelEn("Animal abuse")
                        .labelVi("Ngược đãi động vật")
                        .policyTextEn("Shows or promotes cruelty, torture, or harm against animals")
                        .policyTextVi("Hiển thị hoặc cổ xúy hành vi tàn ác, tra tấn hoặc làm hại động vật")
                        .type(ReportReasonType.VIOLENCE_ABUSE_EXPLOITATION)
                        .parent(violence)
                        .sortOrder(2)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("CRIMINAL_EXPLOITATION")
                        .labelEn("Criminal exploitation")
                        .labelVi("Bóc lột để phạm tội")
                        .policyTextEn("Promotes, instructs, or facilitates criminal activity")
                        .policyTextVi("Cổ xúy, hướng dẫn hoặc hỗ trợ hành vi phạm tội")
                        .type(ReportReasonType.VIOLENCE_ABUSE_EXPLOITATION)
                        .parent(violence)
                        .sortOrder(3)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("THREATS_OF_VIOLENCE")
                        .labelEn("Threats of violence")
                        .labelVi("Đe dọa bạo lực")
                        .policyTextEn("Threatens violence or serious physical harm")
                        .policyTextVi("Đe dọa bạo lực hoặc gây tổn hại thể chất nghiêm trọng")
                        .type(ReportReasonType.VIOLENCE_ABUSE_EXPLOITATION)
                        .parent(violence)
                        .sortOrder(4)
                        .active(true)
                        .build());


                // =========================
                // 3. Hate / Harassment
                // =========================
                ReportReason hateHarassment = reasonRepo.save(ReportReason.builder()
                        .code("HATE_HARASSMENT")
                        .labelEn("Hate and harassment")
                        .labelVi("Thù ghét và quấy rối")
                        .type(ReportReasonType.HATE_HARASSMENT)
                        .sortOrder(3)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("BULLYING_HARASSMENT")
                        .labelEn("Bullying or harassment")
                        .labelVi("Bắt nạt hoặc quấy rối")
                        .policyTextEn("Targets a person with insults, humiliation, threats, or repeated unwanted contact")
                        .policyTextVi("Nhắm vào cá nhân bằng lăng mạ, sỉ nhục, đe dọa hoặc liên hệ không mong muốn nhiều lần")
                        .type(ReportReasonType.HATE_HARASSMENT)
                        .parent(hateHarassment)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("HATE_SPEECH")
                        .labelEn("Hate speech")
                        .labelVi("Ngôn từ thù ghét")
                        .policyTextEn("Attacks or dehumanizes people based on protected characteristics")
                        .policyTextVi("Tấn công hoặc hạ thấp nhân phẩm người khác dựa trên đặc điểm được bảo vệ")
                        .type(ReportReasonType.HATE_HARASSMENT)
                        .parent(hateHarassment)
                        .sortOrder(2)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("SEXUAL_HARASSMENT")
                        .labelEn("Sexual harassment")
                        .labelVi("Quấy rối tình dục")
                        .policyTextEn("Targets someone with unwanted sexual comments, images, or behavior")
                        .policyTextVi("Nhắm vào người khác bằng bình luận, hình ảnh hoặc hành vi tình dục không mong muốn")
                        .type(ReportReasonType.HATE_HARASSMENT)
                        .parent(hateHarassment)
                        .sortOrder(3)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("CYBERBULLYING")
                        .labelEn("Cyberbullying")
                        .labelVi("Bắt nạt trên mạng")
                        .policyTextEn("Repeatedly targets someone online to shame, intimidate, or humiliate")
                        .policyTextVi("Liên tục nhắm vào người khác trên mạng để bêu xấu, uy hiếp hoặc sỉ nhục")
                        .type(ReportReasonType.HATE_HARASSMENT)
                        .parent(hateHarassment)
                        .sortOrder(4)
                        .active(true)
                        .build());


                // =========================
                // 4. Suicide / Self-harm
                // =========================
                ReportReason selfHarm = reasonRepo.save(ReportReason.builder()
                        .code("SUICIDE_SELF_HARM")
                        .labelEn("Suicide or self-harm")
                        .labelVi("Tự tử hoặc tự gây hại")
                        .type(ReportReasonType.SUICIDE_SELF_HARM)
                        .sortOrder(4)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("SELF_HARM_PROMOTION")
                        .labelEn("Self-harm promotion")
                        .labelVi("Cổ xúy tự gây hại")
                        .policyTextEn("Shows, promotes, or encourages self-injury or suicide")
                        .policyTextVi("Hiển thị, cổ xúy hoặc khuyến khích tự gây thương tích hoặc tự tử")
                        .type(ReportReasonType.SUICIDE_SELF_HARM)
                        .parent(selfHarm)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("SUICIDE_CONTENT")
                        .labelEn("Suicide content")
                        .labelVi("Nội dung liên quan đến tự tử")
                        .policyTextEn("Promotes, instructs, or romanticizes suicide")
                        .policyTextVi("Cổ xúy, hướng dẫn hoặc lãng mạn hóa hành vi tự tử")
                        .type(ReportReasonType.SUICIDE_SELF_HARM)
                        .parent(selfHarm)
                        .sortOrder(2)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("SELF_HARM_INSTRUCTIONS")
                        .labelEn("Self-harm instructions")
                        .labelVi("Hướng dẫn tự gây hại")
                        .policyTextEn("Provides instructions for self-injury or suicide")
                        .policyTextVi("Cung cấp hướng dẫn tự gây thương tích hoặc tự tử")
                        .type(ReportReasonType.SUICIDE_SELF_HARM)
                        .parent(selfHarm)
                        .sortOrder(3)
                        .active(true)
                        .build());


                // =========================
                // 5. Dangerous activities
                // =========================
                ReportReason dangerousActivities = reasonRepo.save(ReportReason.builder()
                        .code("DANGEROUS_ACTIVITIES")
                        .labelEn("Dangerous activities")
                        .labelVi("Hoạt động nguy hiểm")
                        .type(ReportReasonType.DANGEROUS_ACTIVITIES)
                        .sortOrder(5)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("DANGEROUS_CHALLENGE")
                        .labelEn("Dangerous challenge")
                        .labelVi("Thử thách nguy hiểm")
                        .policyTextEn("Promotes risky stunts, dangerous challenges, or activities likely to cause serious harm")
                        .policyTextVi("Cổ xúy trò mạo hiểm, thử thách nguy hiểm hoặc hành vi dễ gây tổn hại nghiêm trọng")
                        .type(ReportReasonType.DANGEROUS_ACTIVITIES)
                        .parent(dangerousActivities)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("DANGEROUS_INSTRUCTIONS")
                        .labelEn("Dangerous instructions")
                        .labelVi("Hướng dẫn nguy hiểm")
                        .policyTextEn("Provides instructions for dangerous acts that could cause serious harm")
                        .policyTextVi("Cung cấp hướng dẫn thực hiện hành vi nguy hiểm có thể gây tổn hại nghiêm trọng")
                        .type(ReportReasonType.DANGEROUS_ACTIVITIES)
                        .parent(dangerousActivities)
                        .sortOrder(2)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("EXTREME_STUNTS")
                        .labelEn("Extreme stunts")
                        .labelVi("Trò mạo hiểm cực đoan")
                        .policyTextEn("Shows or promotes extreme stunts without proper safety context")
                        .policyTextVi("Hiển thị hoặc cổ xúy trò mạo hiểm cực đoan thiếu an toàn")
                        .type(ReportReasonType.DANGEROUS_ACTIVITIES)
                        .parent(dangerousActivities)
                        .sortOrder(3)
                        .active(true)
                        .build());


                // =========================
                // 6. Regulated goods
                // =========================
                ReportReason regulatedGoods = reasonRepo.save(ReportReason.builder()
                        .code("REGULATED_GOODS")
                        .labelEn("Regulated goods")
                        .labelVi("Hàng hóa bị kiểm soát")
                        .type(ReportReasonType.REGULATED_GOODS)
                        .sortOrder(6)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("DRUGS_CONTROLLED_SUBSTANCES")
                        .labelEn("Drugs or controlled substances")
                        .labelVi("Ma túy hoặc chất bị kiểm soát")
                        .policyTextEn("Promotes, sells, or instructs the use of illegal drugs or controlled substances")
                        .policyTextVi("Cổ xúy, mua bán hoặc hướng dẫn sử dụng ma túy hay chất bị kiểm soát")
                        .type(ReportReasonType.REGULATED_GOODS)
                        .parent(regulatedGoods)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("WEAPONS")
                        .labelEn("Weapons")
                        .labelVi("Vũ khí")
                        .policyTextEn("Promotes, sells, or instructs the use of weapons")
                        .policyTextVi("Cổ xúy, mua bán hoặc hướng dẫn sử dụng vũ khí")
                        .type(ReportReasonType.REGULATED_GOODS)
                        .parent(regulatedGoods)
                        .sortOrder(2)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("ALCOHOL_TOBACCO")
                        .labelEn("Alcohol or tobacco")
                        .labelVi("Rượu bia hoặc thuốc lá")
                        .policyTextEn("Promotes or sells alcohol, tobacco, or age-restricted products")
                        .policyTextVi("Quảng bá hoặc mua bán rượu bia, thuốc lá hoặc sản phẩm giới hạn độ tuổi")
                        .type(ReportReasonType.REGULATED_GOODS)
                        .parent(regulatedGoods)
                        .sortOrder(3)
                        .active(true)
                        .build());


                // =========================
                // 7. Scam / Spam
                // =========================
                ReportReason scamSpam = reasonRepo.save(ReportReason.builder()
                        .code("SCAM_SPAM")
                        .labelEn("Scam or spam")
                        .labelVi("Lừa đảo hoặc spam")
                        .type(ReportReasonType.SCAM_SPAM)
                        .sortOrder(7)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("SPAM")
                        .labelEn("Spam")
                        .labelVi("Spam")
                        .policyTextEn("Posts repetitive, misleading, irrelevant, or low-quality content")
                        .policyTextVi("Đăng nội dung lặp lại, gây hiểu nhầm, không liên quan hoặc chất lượng thấp")
                        .type(ReportReasonType.SCAM_SPAM)
                        .parent(scamSpam)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("SCAM")
                        .labelEn("Scam")
                        .labelVi("Lừa đảo")
                        .policyTextEn("Attempts to deceive people for money, accounts, personal data, or other benefits")
                        .policyTextVi("Cố gắng lừa người khác để lấy tiền, tài khoản, dữ liệu cá nhân hoặc lợi ích khác")
                        .type(ReportReasonType.SCAM_SPAM)
                        .parent(scamSpam)
                        .sortOrder(2)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("IMPERSONATION")
                        .labelEn("Impersonation")
                        .labelVi("Mạo danh")
                        .policyTextEn("Pretends to be another person, brand, organization, or public figure")
                        .policyTextVi("Giả làm người khác, thương hiệu, tổ chức hoặc người nổi tiếng")
                        .type(ReportReasonType.SCAM_SPAM)
                        .parent(scamSpam)
                        .sortOrder(3)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("PHISHING")
                        .labelEn("Phishing")
                        .labelVi("Lừa đảo lấy thông tin")
                        .policyTextEn("Attempts to steal accounts, passwords, payment information, or private data")
                        .policyTextVi("Cố gắng đánh cắp tài khoản, mật khẩu, thông tin thanh toán hoặc dữ liệu riêng tư")
                        .type(ReportReasonType.SCAM_SPAM)
                        .parent(scamSpam)
                        .sortOrder(4)
                        .active(true)
                        .build());


                // =========================
                // 8. Misinformation
                // =========================
                ReportReason misinformation = reasonRepo.save(ReportReason.builder()
                        .code("MISINFORMATION")
                        .labelEn("Misinformation")
                        .labelVi("Thông tin sai lệch")
                        .type(ReportReasonType.MISINFORMATION)
                        .sortOrder(8)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("HEALTH_MISINFORMATION")
                        .labelEn("Health misinformation")
                        .labelVi("Thông tin sai lệch về sức khỏe")
                        .policyTextEn("Spreads false or misleading health information that may cause harm")
                        .policyTextVi("Lan truyền thông tin sức khỏe sai lệch hoặc gây hiểu nhầm có thể gây hại")
                        .type(ReportReasonType.MISINFORMATION)
                        .parent(misinformation)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("CIVIC_MISINFORMATION")
                        .labelEn("Civic misinformation")
                        .labelVi("Thông tin sai lệch về xã hội hoặc bầu cử")
                        .policyTextEn("Spreads false information about elections, civic processes, or public safety")
                        .policyTextVi("Lan truyền thông tin sai về bầu cử, quy trình xã hội hoặc an toàn cộng đồng")
                        .type(ReportReasonType.MISINFORMATION)
                        .parent(misinformation)
                        .sortOrder(2)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("MANIPULATED_MEDIA")
                        .labelEn("Manipulated media")
                        .labelVi("Nội dung bị chỉnh sửa gây hiểu nhầm")
                        .policyTextEn("Uses edited or synthetic media to mislead viewers")
                        .policyTextVi("Sử dụng nội dung chỉnh sửa hoặc giả tạo để gây hiểu nhầm cho người xem")
                        .type(ReportReasonType.MISINFORMATION)
                        .parent(misinformation)
                        .sortOrder(3)
                        .active(true)
                        .build());


                // =========================
                // 9. Body image / Eating
                // =========================
                ReportReason bodyImageEating = reasonRepo.save(ReportReason.builder()
                        .code("BODY_IMAGE_EATING")
                        .labelEn("Body image and eating disorders")
                        .labelVi("Hình ảnh cơ thể và rối loạn ăn uống")
                        .type(ReportReasonType.BODY_IMAGE_EATING)
                        .sortOrder(9)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("EATING_DISORDER_PROMOTION")
                        .labelEn("Eating disorder promotion")
                        .labelVi("Cổ xúy rối loạn ăn uống")
                        .policyTextEn("Promotes extreme dieting, starvation, purging, or eating disorder behavior")
                        .policyTextVi("Cổ xúy ăn kiêng cực đoan, nhịn ăn, nôn ép hoặc hành vi rối loạn ăn uống")
                        .type(ReportReasonType.BODY_IMAGE_EATING)
                        .parent(bodyImageEating)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("BODY_SHAMING")
                        .labelEn("Body shaming")
                        .labelVi("Miệt thị ngoại hình")
                        .policyTextEn("Targets someone with insults or humiliation about body shape, weight, or appearance")
                        .policyTextVi("Lăng mạ hoặc sỉ nhục người khác về vóc dáng, cân nặng hoặc ngoại hình")
                        .type(ReportReasonType.BODY_IMAGE_EATING)
                        .parent(bodyImageEating)
                        .sortOrder(2)
                        .active(true)
                        .build());


                // =========================
                // 10. Shocking / Graphic
                // =========================
                ReportReason shockingGraphic = reasonRepo.save(ReportReason.builder()
                        .code("SHOCKING_GRAPHIC")
                        .labelEn("Shocking or graphic content")
                        .labelVi("Nội dung gây sốc hoặc phản cảm")
                        .type(ReportReasonType.SHOCKING_GRAPHIC)
                        .sortOrder(10)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("GRAPHIC_VIOLENCE")
                        .labelEn("Graphic violence")
                        .labelVi("Bạo lực máu me")
                        .policyTextEn("Shows severe injury, blood, gore, or graphic physical harm")
                        .policyTextVi("Hiển thị thương tích nặng, máu me hoặc tổn hại thể chất nghiêm trọng")
                        .type(ReportReasonType.SHOCKING_GRAPHIC)
                        .parent(shockingGraphic)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("DISTURBING_CONTENT")
                        .labelEn("Disturbing content")
                        .labelVi("Nội dung gây khó chịu")
                        .policyTextEn("Shows disturbing or shocking scenes that may upset viewers")
                        .policyTextVi("Hiển thị cảnh gây sốc hoặc gây khó chịu cho người xem")
                        .type(ReportReasonType.SHOCKING_GRAPHIC)
                        .parent(shockingGraphic)
                        .sortOrder(2)
                        .active(true)
                        .build());


                // =========================
                // 11. Counterfeit
                // =========================
                ReportReason counterfeit = reasonRepo.save(ReportReason.builder()
                        .code("COUNTERFEIT")
                        .labelEn("Counterfeit goods")
                        .labelVi("Hàng giả")
                        .type(ReportReasonType.COUNTERFEIT)
                        .sortOrder(11)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("FAKE_BRAND_GOODS")
                        .labelEn("Fake brand goods")
                        .labelVi("Hàng giả thương hiệu")
                        .policyTextEn("Promotes or sells counterfeit branded products")
                        .policyTextVi("Quảng bá hoặc mua bán sản phẩm giả mạo thương hiệu")
                        .type(ReportReasonType.COUNTERFEIT)
                        .parent(counterfeit)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("FAKE_DOCUMENTS")
                        .labelEn("Fake documents")
                        .labelVi("Giấy tờ giả")
                        .policyTextEn("Promotes, sells, or provides fake identity documents or certificates")
                        .policyTextVi("Quảng bá, mua bán hoặc cung cấp giấy tờ hay chứng chỉ giả")
                        .type(ReportReasonType.COUNTERFEIT)
                        .parent(counterfeit)
                        .sortOrder(2)
                        .active(true)
                        .build());


                // =========================
                // 12. Shopping
                // =========================
                ReportReason shopping = reasonRepo.save(ReportReason.builder()
                        .code("SHOPPING")
                        .labelEn("Shopping violation")
                        .labelVi("Vi phạm liên quan đến mua bán")
                        .type(ReportReasonType.SHOPPING)
                        .sortOrder(12)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("MISLEADING_SHOPPING")
                        .labelEn("Misleading shopping content")
                        .labelVi("Nội dung mua bán gây hiểu nhầm")
                        .policyTextEn("Uses misleading claims, fake discounts, or deceptive shopping information")
                        .policyTextVi("Sử dụng tuyên bố gây hiểu nhầm, giảm giá giả hoặc thông tin mua bán lừa dối")
                        .type(ReportReasonType.SHOPPING)
                        .parent(shopping)
                        .sortOrder(1)
                        .active(true)
                        .build());

                reasonRepo.save(ReportReason.builder()
                        .code("UNSAFE_PRODUCT")
                        .labelEn("Unsafe product")
                        .labelVi("Sản phẩm không an toàn")
                        .policyTextEn("Promotes products that may be unsafe, harmful, or prohibited")
                        .policyTextVi("Quảng bá sản phẩm có thể không an toàn, gây hại hoặc bị cấm")
                        .type(ReportReasonType.SHOPPING)
                        .parent(shopping)
                        .sortOrder(2)
                        .active(true)
                        .build());
            }
        };
    }
}