package ru.axl.probeproject.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.axl.probeproject.model.entities.Process;
import ru.axl.probeproject.model.entities.ProcessStatus;
import ru.axl.probeproject.model.enums.ProcessStatusEnum;
import ru.axl.probeproject.repositories.ProcessRepository;
import ru.axl.probeproject.services.ComplianceService;
import ru.axl.probeproject.services.ProcessStatusService;

import javax.transaction.Transactional;
import java.util.List;

import static ru.axl.probeproject.model.enums.ProcessStatusEnum.*;
import static ru.axl.probeproject.utils.Utils.getNowOffsetDateTime;

@Slf4j
@Component
@AllArgsConstructor
public class ProcessScheduler {

    private final ProcessRepository processRepo;
    private final ProcessStatusService processStatusService;
    private final ComplianceService complianceService;

    @Transactional
    @Scheduled(fixedDelayString = "${scheduler-config.fixed-delay}",
            initialDelayString = "${scheduler-config.initial-delay}")
    public void scheduleProcessStatus(){
        ProcessStatus newStatus = processStatusService.findByName(NEW.name());

        List<Process> processes = processRepo.findAllWithStatusAndLastUpdateDateBefore(newStatus,
                getNowOffsetDateTime().minusSeconds(10));

        processes.forEach(process -> {
            if(complianceService.checkClient(process.getClient())){
                setStatus(process, COMPLIANCE_SUCCESS);
            } else{
                setStatus(process, COMPLIANCE_ERROR);
            }
        });
    }

    private void setStatus(Process process, ProcessStatusEnum processStatusEnum){
        ProcessStatus complianceSuccess = processStatusService.findByName(processStatusEnum.name());
        process.setProcessStatus(complianceSuccess);
        process.setLastUpdateDate(getNowOffsetDateTime());
        processRepo.save(process);

        log.info("???????????????? ???????????? ???????????????? {} ?? {}", process.getIdProcess(), processStatusEnum.name());
    }

}
