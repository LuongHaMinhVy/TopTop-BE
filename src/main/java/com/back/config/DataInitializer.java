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
                ReportReason nudity = ReportReason.builder()
                        .code("NUDITY_SEXUAL_CONTENT")
                        .labelEn("Nudity or sexual content")
                        .labelVi("Hình ảnh khỏa thân hoặc nội dung tình dục")
                        .type(ReportReasonType.NUDITY_SEXUAL_CONTENT)
                        .sortOrder(1)
                        .active(true)
                        .build();
                reasonRepo.save(nudity);

                ReportReason violence = ReportReason.builder()
                        .code("VIOLENCE_ABUSE_EXPLOITATION")
                        .labelEn("Violence, abuse, and criminal exploitation")
                        .labelVi("Bạo lực, lạm dụng và bóc lột để phạm tội")
                        .type(ReportReasonType.VIOLENCE_ABUSE_EXPLOITATION)
                        .sortOrder(2)
                        .active(true)
                        .build();
                reasonRepo.save(violence);


                ReportReason minorExploitation = ReportReason.builder()
                        .code("MINOR_SEXUAL_EXPLOITATION")
                        .labelEn("Minor sexual exploitation")
                        .labelVi("Bóc lột hoặc lạm dụng người dưới 18 tuổi")
                        .policyTextEn("Shows or promotes sexual exploitation of under-18s...|Shows or promotes physical abuse...|Promotes or facilitates child marriage")
                        .policyTextVi("Cho thấy hoặc cổ xúy hành vi bóc lột tình dục người dưới 18 tuổi...|Cho thấy hoặc cổ xúy hành vi bạo hành thể chất...|Cổ xúy hoặc tạo điều kiện cho tục tảo hôn")
                        .type(ReportReasonType.NUDITY_SEXUAL_CONTENT)
                        .parent(nudity)
                        .sortOrder(1)
                        .active(true)
                        .build();
                reasonRepo.save(minorExploitation);
            }
        };
    }
}