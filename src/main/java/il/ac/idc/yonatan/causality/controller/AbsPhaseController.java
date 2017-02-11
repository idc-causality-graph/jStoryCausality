package il.ac.idc.yonatan.causality.controller;

import il.ac.idc.yonatan.causality.contexttree.PhaseManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbsPhaseController {
    Set<String> getHitIdsFromMap(Map<String, String> result) {
        return result.keySet().stream()
                .filter(x -> x.endsWith("_approve"))
                .map(x -> StringUtils.substringBeforeLast(x, "_approve"))
                .collect(Collectors.toSet());
    }

    String processHits(RedirectAttributes redir, PhaseManager phaseManager) throws IOException {
        log.info("Starting phase");
        List<String> errors = phaseManager.canCreateHits();
        if (errors.isEmpty()) {
            phaseManager.createHits();
        } else {
            redir.addFlashAttribute("errors", errors);
        }
        log.info("Errors: {}", errors);
        return "redirect:/contextTree";
    }

}
