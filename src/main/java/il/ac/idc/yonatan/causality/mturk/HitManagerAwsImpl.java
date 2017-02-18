package il.ac.idc.yonatan.causality.mturk;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.data.CausalityHitResult;
import il.ac.idc.yonatan.causality.mturk.data.CausalityQuestion;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ygraber on 2/18/17.
 */
//@Service
@Slf4j
@Controller
@Profile("aws")
//@Conditional(HitManagerConfig.AwsHitManager.class)
public class HitManagerAwsImpl implements HitManager {

    private final Configuration freeMarkerConfig;
    private final AppConfig appConfig;

    public HitManagerAwsImpl(Configuration freeMarkerConfig, AppConfig appConfig) {
        this.freeMarkerConfig = freeMarkerConfig;
        this.appConfig = appConfig;
    }

    private String submitUrl;

    @PostConstruct
    public void init() {
        submitUrl = appConfig.isSandbox() ? "https://workersandbox.mturk.com/mturk/externalSubmit" : "https://www.mturk.com/mturk/externalSubmit";
        log.info("Using AWS Hit manager to url {}", submitUrl);
        log.debug("Sandbox? {}", appConfig.isSandbox());
        LinkedHashMap<String, List<String>> input = new LinkedHashMap<>();
        input.put("1234", Lists.newArrayList("Summary1.1", "Summary1.2", "Summary1.3"));
        input.put("1235", Lists.newArrayList("Summary2.1", "Summary2.2", "Summary2.3"));
        input.put("1236", Lists.newArrayList("Summary3.1", "Summary3.2", "Summary3.3"));
        createUpHit(input);

        List<String> summaries = Lists.newArrayList("summary-root", "summary1", "summary2");
        List<Pair<String, String>> childrenIdsAndSummaries = Lists.newArrayList(
                Pair.of("1234", "summary for 1234"),
                Pair.of("1235", "summary for 1235"),
                Pair.of("1236", "summary for 1236")
        );
        createDownHit(summaries, childrenIdsAndSummaries, true);
    }

//    @Autowired
//    ContextTree contextTree;
//
//    @Autowired
//    Configuration freeMarkerConfig;

    @SneakyThrows({IOException.class, TemplateException.class})
    private String renderHitTemplate(String templateName, Map<String, Object> inputDataModel) {
        Template template = freeMarkerConfig.getTemplate(templateName);
        Map<String, Object> dataModel = new HashMap<>(inputDataModel);
        dataModel.put("submitUrl", submitUrl);
        StringWriter stringWriter = new StringWriter();
        template.process(dataModel, stringWriter);
        String output = stringWriter.toString();
        FileUtils.write(new File("./tmp/" + templateName + ".html"), output, Charset.defaultCharset());
        return output;
    }

    @Override
    public String createUpHit(LinkedHashMap<String, List<String>> childIdToSummaries) {
        String upHitHtml = renderHitTemplate("upHit.ftl",
                ImmutableMap.of("childIdToSummaries", childIdToSummaries));
        return "hit-id";
    }

    @Override
    public String createDownHit(List<String> parentsSummaries, List<Pair<String, String>> childrenIdsAndSummaries,
                                boolean isLeaf) {
        String downHitHtml = renderHitTemplate("downHit.ftl",
                ImmutableMap.of("parentsSummaries", parentsSummaries, "childrenIdsAndSummaries", childrenIdsAndSummaries));
        return "hit-id";
    }

    @Override
    public UpHitResult getUpHitForReview(String hitId) {
        return null;
    }

    @Override
    public DownHitResult getDownHitForReview(String hitId) {
        return null;
    }

    @Override
    public CausalityHitResult getCausalityHitForReview(String hitId) {
        return null;
    }

    @Override
    public void submitReviewUpHit(String hitId, boolean hitApproved, String reason) {

    }

    @Override
    public void submitDownHitReview(String hitId, boolean hitApproved, String reason) {

    }

    @Override
    public void submitCausalityHitReview(String hitId, boolean hitApproved, String reason) {

    }

    @Override
    public void reset() {

    }

    @Override
    public String createCausalityHit(String globalSummary, List<CausalityQuestion> causalityHitQuestions) {
        return null;
    }
}
