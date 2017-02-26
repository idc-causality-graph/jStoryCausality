<!DOCTYPE html>
<html>
<head>
    <title>Review Causality Hits</title>
    <link rel="stylesheet" href="/css/bootstrap.min.css">
    <link rel="stylesheet" href="/css/bootstrap-grid.min.css">
    <script src="/js/jquery-3.1.1.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <script type="application/javascript">
        $(function () {
            $("input[type='radio']").click(function () {
                var $this = $(this);
                var hitId = $this.data().hitid;
                var approve = $this.val() == '1';
                $("#" + hitId + "_reason").toggle(!approve);
            });
        });
    </script>
    <style>
        input[type=checkbox][disabled]:checked {
            outline: 2px solid red;
        }
    </style>
</head>
<body>
<div class="container-fluid">
    <h1>Review CAUSALITY hits</h1>
    <form method="post">
        <div class="sticky-top">
            <div class="d-flex flex-row">
                <button class="p-2 btn btn-info" type="submit">Submit</button>
                <a class="p-2 btn btn-info" href="">Reload</a>
            </div>
        </div>

    <#list hitsForReview as hitForReview>
        <#assign assignmentId = hitForReview.assignmentId>
        <#assign assignmentHitId = hitForReview.hitId + ':' + hitForReview.assignmentId>
        <div class="card mb-2">
            <div class="card-header card-primary card-inverse">
                HitId: ${assignmentHitId}
            </div>
            <div class="card-block">
                <#assign hiddenInput = []>
                <#if !hitForReview.consistentAnswers>
                    <div class="form-group row">
                        <div class="col-sm-12 text-danger">Non consistent answer</div>
                    </div>
                </#if>
                <#list hitForReview.causalityDataList as causalityData>
                    <div class="form-group row">
                        <label class="col-sm-2 form-control-label">Query node (${causalityData.queryNodeId})</label>
                        <div class="col-sm-10">
                            <span class="form-control-static">${causalityData.queryText}</span>
                        </div>
                    </div>
                    <label>Causes</label><br>
                    <ul>
                        <#list causalityData.causeNodesTextRelations as nodeTextRel>
                            <#if nodeTextRel.right>
                                <#assign hiddenInput = hiddenInput + [ causalityData.queryNodeId + ':' + nodeTextRel.left]>
                            </#if>
                            <div class="form-group row">
                                <label class="col-sm-2 form-control-label">
                                    <input readonly disabled type="checkbox" ${nodeTextRel.right?then('checked','')}>
                                    (${nodeTextRel.left})
                                </label>
                                <div class="col-sm-10">
                                    <span class="form-control-static">${nodeTextRel.middle}</span>
                                </div>
                            </div>
                        </#list>
                    </ul>
                    <#sep >
                        <hr/></#sep>
                </#list>

                <!-- TODO refactor that into common code -->
                <fieldset class="form-group row">
                    <legend class="col-form-legend col-sm-2">Approve</legend>
                    <div class="form-check form-check-inline">
                        <label class="form-check-label">
                            <input type="radio" name="${assignmentHitId}_approve" value="1"
                                   data-hitid="${assignmentId}"> Yes
                        </label>
                        <label class="form-check-label">
                            <input type="radio" name="${assignmentHitId}_approve" value="0"
                                   data-hitid="${assignmentId}"> No
                        </label>
                    </div>
                </fieldset>
                <div class="form-group row" id="${assignmentId}_reason" style="display: none">
                    <label class="col-sm-2 form-control-label">Reason</label>
                    <div class="col-sm-10">
                                <textarea name="${assignmentId}_reason" placeholder="Reason"
                                          autocomplete="off"
                                          class="form-control"></textarea>
                    </div>
                </div>

                <input type="hidden" name="${assignmentHitId}_pairs" value="${hiddenInput?join(';')}">
            </div>
        </div>
    </#list>
    </form>
</div>
</body>
</html>