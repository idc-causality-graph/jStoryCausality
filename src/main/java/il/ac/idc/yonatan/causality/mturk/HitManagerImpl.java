package il.ac.idc.yonatan.causality.mturk;

import com.fasterxml.jackson.databind.ObjectMapper;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by ygraber on 1/28/17.
 */
@Service
@Controller
@Slf4j
public class HitManagerImpl implements HitManager {

    private static final File DB = new File("./hit-storage.json");

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info("Using mock db in {}", DB);

    }

    @SneakyThrows
    private HitStorage readDb() {
        if (!DB.exists()) {
            return new HitStorage();
        }
        try (InputStream is = new FileInputStream(DB)) {
            return objectMapper.readValue(is, HitStorage.class);
        }
    }

    @SneakyThrows
    private void saveDb(HitStorage hitStorage) {
        try (OutputStream os = new FileOutputStream(DB)) {
            objectMapper.writeValue(os, hitStorage);
        }
    }


    @Data
    public static class HitStorage {
        private Map<String, UpHitResultStorage> upHits = new HashMap<>();
        private Map<String, DownHitResultStorage> downHits = new HashMap<>();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpHitResultStorage {
        private UpHitResult upHitResult;
        private LinkedHashMap<String, List<String>> childIdToSummaries;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DownHitResultStorage {
        private DownHitResult downHitResult;
        private boolean leaf;
        private List<String> allRootSummaries;
        private List<String> nodeSummaries;
    }

    public String createUpHit(LinkedHashMap<String, List<String>> childIdToSummaries) {
        String hitId = "HIT-" + RandomStringUtils.randomAlphanumeric(8);
        HitStorage hitStorage = readDb();
        UpHitResult upHitResult = new UpHitResult();
        upHitResult.setHitDone(false);
        UpHitResultStorage upHitResultStorage = new UpHitResultStorage(upHitResult, childIdToSummaries);
        hitStorage.getUpHits().put(hitId, upHitResultStorage);
        saveDb(hitStorage);
        return hitId;
    }


    public UpHitResult getUpHitForReview(String hitId) {
        return readDb().getUpHits().get(hitId).getUpHitResult();
    }

    public void submitReviewUpHit(String hitId, boolean hitApproved, String reason) {
        log.info("HitId {} approved? {}", hitId, hitApproved);
        HitStorage db = readDb();
        if (hitApproved) {
            db.getUpHits().remove(hitId);
        } else {
            UpHitResult hitResult = db.getUpHits().get(hitId).getUpHitResult();
            hitResult.setHitDone(false);
            hitResult.setHitSummary(null);
        }
        saveDb(db);
    }

    public DownHitResult getDownHitForReview(String hitId) {
        return readDb().getDownHits().get(hitId).getDownHitResult();
    }

    public void submitDownHitReview(String hitId, boolean hitApproved, String reason) {
        HitStorage db = readDb();
        if (hitApproved) {
            db.getDownHits().remove(hitId);
        } else {
            DownHitResult hitResult = db.getDownHits().get(hitId).getDownHitResult();
            hitResult.setHitDone(false);
            hitResult.setImportanceScore(null);
            hitResult.setMostImportantEvent(null);
        }
        saveDb(db);
    }

    @Override
    public String createDownHit(List<String> allRootSummaries, List<String> nodeSummaries, boolean isLeaf) {
        String hitId = "HIT_D-" + RandomStringUtils.randomAlphanumeric(8);
        HitStorage db = readDb();
        DownHitResult downHitResult = new DownHitResult();
        downHitResult.setHitDone(false);
        DownHitResultStorage downHitResultStorage = new DownHitResultStorage(
                downHitResult, isLeaf, allRootSummaries, nodeSummaries);
        db.getDownHits().put(hitId, downHitResultStorage);
        saveDb(db);
        return hitId;
    }

    @GetMapping("/hits")
    public String generateHitsView(Model model) {
        HitStorage hitStorage = readDb();
        model.addAttribute("hitStorage", hitStorage);
        return "hits";
    }

    @PostMapping("/hits")
    public String saveHits(@RequestParam Map<String, String> result) throws IOException {
        HitStorage hitStorage = readDb();

        processUpHits(hitStorage, result);
        processDownHits(hitStorage, result);
        System.out.println(result);
        saveDb(hitStorage);
        return "redirect:/hits";
    }

    private void processUpHits(HitStorage hitStorage, @RequestParam Map<String, String> result) {
        Set<String> hitIds = result.keySet().stream().filter(x -> x.endsWith("_hitsummary"))
                .map(x -> StringUtils.substringBeforeLast(x, "_hitsummary"))
                .collect(Collectors.toSet());

        for (String hitId : hitIds) {
            UpHitResultStorage upHitResultStorage = hitStorage.getUpHits().get(hitId);
            UpHitResult upHitResult = upHitResultStorage.getUpHitResult();
            Set<String> childIds = upHitResultStorage.childIdToSummaries.keySet();
            boolean good = true;
            String summary = result.get(hitId + "_hitsummary");
            if (StringUtils.isEmpty(summary)) {
                continue;
            }
            upHitResult.setHitSummary(summary);
            for (String childId : childIds) {
                String idx = result.get(hitId + "_" + childId);
                if (idx != null) {
                    upHitResult.getChosenChildrenSummaries().put(childId, Integer.valueOf(idx));
                } else {
                    good = false;
                }
            }
            if (good) {
                upHitResult.setHitDone(true);
            }
        }
    }

    private void processDownHits(HitStorage hitStorage, Map<String, String> result) {

        Set<String> hitIds = result.keySet().stream()
                .filter(x -> x.startsWith("DOWN_"))
                .map(s -> StringUtils.substringAfter(s, "DOWN_"))
                .map(s -> StringUtils.substringBeforeLast(s, "_"))
                .collect(Collectors.toSet());

        for (String hitId : hitIds) {
            DownHitResultStorage downHitResultStorage = hitStorage.getDownHits().get(hitId);
            Integer importanceScore = NumberUtils.createInteger(result.get("DOWN_" + hitId + "_score"));
            String mostImportantEvent = result.get("DOWN_" + hitId + "_event");
            if (importanceScore != null && StringUtils.isNotEmpty(mostImportantEvent)) {
                DownHitResult downHitResult = downHitResultStorage.getDownHitResult();
                downHitResult.setHitDone(true);
                downHitResult.setMostImportantEvent(mostImportantEvent);
                downHitResult.setImportanceScore(importanceScore);
            }
        }
    }

}
