<!DOCTYPE html>
<html>
<head>
    <title>Review Down Hits</title>
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

    <#list hitsForReview?sort_by('hitDone')?reverse as hitForReview>
        <div class="card mb-2">
            <div class="card-header ${hitForReview.hitDone?then('card-primary card-inverse','')}">
                HitId: ${hitForReview.hitId}
            </div>
            <div class="card-block">
                <div class="form-group row">
                    <label class="col-sm-3 col-form-label">Is done?</label>
                    <div class="col-sm-9">
                        <span class="form-control-static">${hitForReview.hitDone?c}</span>
                    </div>
                </div>
                <#if hitForReview.hitDone>
                    <#assign hiddenInput = []>
                    <#list hitForReview.causalityDataList as causalityData>
                        <div class="form-group row">
                            <label class="col-sm-3 col-form-label">Query node (${causalityData.queryNodeId})</label>
                            <div class="col-sm-9">
                                <span class="form-control-static">${causalityData.queryText}</span>
                            </div>
                        </div>
                        <label>Causes</label><br>
                        <#list causalityData.causeNodesTextRelations as nodeTextRel>
                            <#if nodeTextRel.right>
                                <#assign hiddenInput = hiddenInput + [ causalityData.queryNodeId + ':' + nodeTextRel.left]>
                            </#if>
                            <div class="form-group row">
                                <label class="col-sm-2 col-form-label">
                                    <input readonly disabled type="checkbox" ${nodeTextRel.right?then('checked','')}>
                                    (${nodeTextRel.left})
                                </label>
                                <div class="col-sm-10">
                                    <span class="form-control-static">${nodeTextRel.middle}</span>
                                </div>
                            </div>
                        </#list>
                    </#list>

                    <!-- TODO refactor that into common code -->
                    <fieldset class="form-group row">
                        <legend class="col-form-legend col-sm-2">Approve</legend>
                        <div class="form-check form-check-inline">
                            <label class="form-check-label">
                                <input type="radio" name="${hitForReview.hitId}_approve" value="1"
                                       data-hitid="${hitForReview.hitId}"> Yes
                            </label>
                            <label class="form-check-label">
                                <input type="radio" name="${hitForReview.hitId}_approve" value="0"
                                       data-hitid="${hitForReview.hitId}"> No
                            </label>
                        </div>
                    </fieldset>
                    <div class="form-group row" id="${hitForReview.hitId}_reason" style="display: none">
                        <label class="col-sm-3 col-form-label">Reason</label>
                        <div class="col-sm-9">
                                <textarea name="${hitForReview.hitId}_reason" placeholder="Reason"
                                          class="form-control"></textarea>
                        </div>
                    </div>

                    <input type="text" name="${hitForReview.hitId}_pairs" value="${hiddenInput?join(';')}">
                </#if>
            </div>
        </div>
    </#list>
    </form>
</div>
</body>
</html>