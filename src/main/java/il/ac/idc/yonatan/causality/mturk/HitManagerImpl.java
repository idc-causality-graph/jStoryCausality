package il.ac.idc.yonatan.causality.mturk;

import com.fasterxml.jackson.databind.ObjectMapper;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
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
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpHitResultStorage {
        private UpHitResult upHitResult;
        private LinkedHashMap<String, List<String>> childIdToSummaries;
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
//        UpHitResult upHitResult=new UpHitResult();
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("Accept? ");
//        String accept = scanner.nextLine();
//        if (accept.toLowerCase().equals("y")){
//            upHitResult.setHitDone(true);
//        } else {
//            upHitResult.setHitDone(false);
//            return upHitResult;
//        }
//        System.out.println("Summary: ");
//        upHitResult.setHitSummary(scanner.nextLine());
//        //TODO simulate chosen children ...
//        return upHitResult;
    }

    @GetMapping("/hits")
    public String generateHitsView(Model model) {
        HitStorage hitStorage = readDb();
        model.addAttribute("hitStorage", hitStorage);
//        System.out.println(hitStorage.getUpHits().get("HIT-Tpoi9Ikk").childIdToSummaries.keySet());
        return "hits";
    }

    @PostMapping("/hits")
    public String saveHits(@RequestParam Map<String, String> result) throws IOException {
        HitStorage hitStorage = readDb();

        Set<String> hitIds = result.keySet().stream().filter(x -> x.endsWith("_hitsummary"))
                .map(x -> StringUtils.substringBeforeLast(x, "_hitsummary"))
                .collect(Collectors.toSet());

        for (String hitId : hitIds) {
//            System.out.println("hitId: "+hitId);
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
        saveDb(hitStorage);
        return "redirect:/hits";
    }

}
