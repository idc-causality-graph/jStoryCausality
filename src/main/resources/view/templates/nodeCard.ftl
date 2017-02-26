<div class="card">
    <div class="card-header">
        <b>Id:</b>&nbsp;<a name="${node.id}">${node.id}</a>
    <#if node.leaf>&nbsp;&nbsp;&nbsp;<b>LEAF</b></#if>
    </div>
    <div class="card-block">
    <#assign isRoot=true />
    <#if node.parent??>
        <#assign isRoot=false />
        <div class="form-group row mb-0">
            <label class="col-sm-2 col-form-label">Parent</label>
            <div class="col-sm-10">
                <p class="form-control-static">
                    <a href="#${node.parent.id}">${node.parent.id}</a>
                </p>
            </div>
        </div>
        <#if (!node.leaf)>
            <div class="form-group row mb-0">
                <label class="col-sm-2 col-form-label">Up phase done?</label>
                <div class="col-sm-10">
                    <p class="form-control-static">${node.isUpPhaseDone(repFactor)?c}</p>
                </div>
            </div>
        </#if>
    </#if>
        <div class="form-group row mb-0">
            <label class="col-sm-2 col-form-label">Down phase done?</label>
            <div class="col-sm-10">
                <p class="form-control-static">${node.isDownPhaseDone(repFactor)?c}</p>
            </div>
        </div>
    <#if node.upHitId??>
        <div class="form-group row mb-0">
            <label class="col-sm-2 col-form-label">Up HIT</label>
            <div class="col-sm-10">
                <p class="form-control-static">${node.upHitId}: (${node.completedUpAssignmentsIds?size}/${repFactor})
                ${node.completedUpAssignmentsIds?join(', ')}
                </p>
            </div>
        </div>
    </#if>
    <#if isRoot>
    <form id='root_node_form'></#if>
    <#comment>This part shows input buttons only in root node.
        The edit button available only if something was already picked
        </#comment>
    <#assign bestSummaryIdx=-1>
    <#if node.bestSummaryIdx??>
        <#assign bestSummaryIdx=node.bestSummaryIdx>
    </#if>
        <div class="form-group row mb-0">
        <#list node.summaries as summary>
            <!--TODO add chosing mechanism -->
            <label class="col-sm-2 col-form-label">
                <#if isRoot>
                    <span class="ro_input <#if (bestSummaryIdx==-1)>hdn</#if>">
                        <#if (summary?index==bestSummaryIdx)>X<#else>&nbsp;</#if>
                    </span>
                    <input type='radio' name='rootChosen' <#if (summary?index==bestSummaryIdx)>checked</#if>
                           <#if (bestSummaryIdx>-1)>class="hdn"</#if>
                           <#if (bestSummaryIdx>-1)>readonly</#if>
                           value='${summary?index}'>
                </#if>
                <#if (!isRoot && summary?index==bestSummaryIdx)><u></#if>
                Summary #{summary?index + 1}
                <#if (!isRoot && summary?index==bestSummaryIdx)></u></#if>
            </label>
            <div class="col-sm-10">
                <p class="form-control-static">${summary}</p>
            </div>

        </#list>
        <#if (bestSummaryIdx>-1 && isRoot)>
            <small id="dblclick_message" class="form-text text-muted col-sm-12">Double click radio button to change
            </small>
        </#if>
        </div>
    <#if isRoot></form></#if>
    <#if (node.downHitId??)>
        <div class="form-group row">
            <label class="col-sm-2 col-form-label">Down HIT</label>
            <div class="col-sm-10">
                <p class="form-control-static">${node.downHitId}: (${node.completedDownAssignmentsIds?size}/${repFactor})
                ${node.completedDownAssignmentsIds?join(', ')}
                </p>
            </div>
        </div>
    </#if>
    <#if (node.eventImportanceScores?size>0)>
        <div class="form-group row">
            <label class="col-sm-2 col-form-label">Importance scores</label>
            <div class="col-sm-10">
                <p class="form-control-static">${node.eventImportanceScores?join(", ")}</p>
            </div>
        </div>
        <div class="form-group row">
            <label class="col-sm-2 col-form-label">Average score</label>
            <div class="col-sm-10">
                <p class="form-control-static">${node.averageImportanceScore?string["0.##"]}
                    (${node.eventImportanceWorkerNormalizedScores?join(", ")})</p>
            </div>
        </div>
    </#if>
    <#if node.normalizedImportanceScore??>
        <div class="form-group row">
            <label class="col-sm-2 col-form-label">Normalized importance score</label>
            <div class="col-sm-10">
                <p class="form-control-static">${node.normalizedImportanceScore?string["0.###"]}</p>
            </div>
        </div>
    </#if>

    <#if !node.leaf>
        <div class="form-group row">
            <label class="col-sm-2 col-form-label">Children</label>
            <div class="col-sm-10">
                <p class="form-control-static">
                    <#list node.children as child><a href="#${child.id}">${child.id}</a><#sep >, </#list>
                </p>
            </div>
        </div>
    </#if>

    <#if (node.causalityData.targetNodeIdEntrySet?size>0)>
        <div class="form-group row">
            <label class="col-sm-2 col-form-label">This node causes:</label>
            <div class="col-sm-10">
                <p class="form-control-static">
                    <#list node.causalityData.targetNodeIdEntrySet as entry>
                        <a href="#${entry.element}">${entry.element}</a> (${entry.count})
                        <#sep >,&nbsp;
                    </#list>
                </p>
            </div>
        </div>
    </#if>
    </div>
</div>