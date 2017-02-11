package il.ac.idc.yonatan.causality.mturk.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CauseAndAffect {
    private String causeNodeId;
    private String affectNodeId;
}
