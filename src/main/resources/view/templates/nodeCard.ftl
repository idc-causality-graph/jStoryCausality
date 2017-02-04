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
    </#if>
        <div class="form-group row mb-0">
            <label class="col-sm-2 col-form-label">Up phase done?</label>
            <div class="col-sm-10">
                <p class="form-control-static">${node.upPhaseDone?c}</p>
            </div>
        </div>
        <div class="form-group row mb-0">
            <label class="col-sm-2 col-form-label">Down phase done?</label>
            <div class="col-sm-10">
                <p class="form-control-static">${node.downPhaseDone?c}</p>
            </div>
        </div>
    <#if (node.upHitIds?size>0)>
        <div class="form-group row mb-0">
            <label class="col-sm-2 col-form-label">Up HITs</label>
            <div class="col-sm-10">
                <p class="form-control-static">
                    <#list node.upHitIds as upHitId>
                        <span class="<#if node.completedUpHitIds?seq_contains(upHitId)>hit_id_done</#if>">
                        ${upHitId}
                        </span><#sep>,  </#sep>
                    </#list>
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
                Summary #{summary?index + 1}
            </label>
            <div class="col-sm-10">
                <p class="form-control-static">${summary}</p>
            </div>

        </#list>
        <#if (bestSummaryIdx>-1 && isRoot)>
            <small id="dblclick_message" class="form-text text-muted col-sm-12">Double click radio button to change</small>
        </#if>
        </div>
    <#if isRoot></form></#if>
    <#if (node.downHitIds?size>0)>
        <div class="form-group row">
            <label class="col-sm-2 col-form-label">Down HITs</label>
            <div class="col-sm-10">
                <p class="form-control-static">
                    <#list node.downHitIds as downHitId>
                        <span class="<#if node.completedDownHitIds?seq_contains(downHitId)>hit_id_done</#if>">
                            ${downHitId}<#sep>,  </#sep>
                        </span>
                    </#list>
                </p>
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

    <#--<b>up phase done?</b>&nbsp;true<br>-->
    <#--<b>Down phase done?</b>&nbsp;false<br>-->
    <#--<b>Up HIT ids:</b>&nbsp;[HIT-qObho0uS, HIT-Ufb2zU1O, HIT-ipWceiZb, HIT-mnyd67rl]<br>-->
    <#--<b>Completed Up HIT ids:</b>&nbsp;[HIT-mnyd67rl, HIT-ipWceiZb, HIT-Ufb2zU1O, HIT-qObho0uS]<br>-->
    <#--<b>Down HIT ids:</b>&nbsp;[HIT_D-C4Ncubd9, HIT_D-acJKxwW5, HIT_D-J9yStMUF, HIT_D-YyUz293I]<br>-->
    <#--<b>Completed Up HIT ids:</b>&nbsp;[]<br>-->
    <#--<b>Down phase rates:</b>&nbsp;[]<br>-->
    <#--<b>Average rate:</b>&nbsp;0.0<br><b>Norm average rate:&nbsp;0.00</b><br>-->
    <#--<b><u>Summary #1</u>:</b>&nbsp;d<br>-->
    <#--<b>Summary-->
    <#--#2:</b>&nbsp;c<br>-->
    <#--<b>Summary #3:</b>&nbsp;b<br>-->
    <#--<b>Summary #4:</b>&nbsp;a<br>-->
    <#--<b>Children:</b>&nbsp;-->
    <#--<a href="#Yw00mTzR8T7b">Yw00mTzR8T7b</a>&nbsp;-->
    <#--<a href="#bybXUpfLDXQQ">bybXUpfLDXQQ</a>&nbsp;-->
    <#--<a href="#mTcE85oxciMe">mTcE85oxciMe</a>&nbsp;-->
    </div>
</div>