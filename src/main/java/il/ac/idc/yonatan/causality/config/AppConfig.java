package il.ac.idc.yonatan.causality.config;

import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.File;

/**
 * Created by ygraber on 1/28/17.
 */
@Component
@ConfigurationProperties(prefix = "causality")
@Data
@ToString(exclude = "awsSecretKey")
public class AppConfig {

    @NotNull
    private File contextFile;

    private String inputResource;

    @NotEmpty
    private String awsKey;

    @NotEmpty
    private String awsSecretKey;

    private boolean sandbox;

    @Min(1)
    private int branchFactor;

    @Min(1)
    private int leafBranchFactor;

    @Min(1)
    private int replicationFactor;

    private double importanceThreshold;

    @Min(1)
    private int causalityReplicaFactor;

    private double upHitReward;
    private double downHitReward;
    private double causalityHitReward;

    private int hitLifetimeInMinutes;
    private int hitDurationInMinutes;

    /**
     * in Metaleaf mode, we use summaries of meta leaf (the text of the parent of the leaf, which has only one child
     * in that mode)
     * @return
     */
    public boolean isUseMetaLeafs() {
        return leafBranchFactor == 1;
    }
}
