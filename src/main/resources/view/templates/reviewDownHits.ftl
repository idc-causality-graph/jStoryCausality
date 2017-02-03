<!DOCTYPE html>
<html>
<head>
    <title>Review Down Hits</title>
    <link rel="stylesheet" href="/css/bootstrap.min.css">
    <link rel="stylesheet" href="/css/bootstrap-grid.min.css">
    <script src="/jquery-3.1.1.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <script type="application/javascript">
        $(function () {
            $("input[type='radio']").click(function () {
                var $this = $(this);
                var hitId = $this.data().hitid;
                var approve = $this.val() == '1';
                $("#" + hitId + "_reason").toggle(!approve);
            });
        })
    </script>
</head>
<body>
<div class="container-fluid">
    <h1>Review DOWN hits</h1>
    <form method="post">
        <div class="sticky-top">
            <div class="d-flex flex-row">
                <button class="p-2 btn btn-info" type="submit">Submit</button>
                <a class="p-2 btn btn-info" href="">Reload</a>
            </div>
        </div>
    <#list hitsForReview as hitForReview>
        <div>
            <div class="card mb-2">
                <div class="card-header ${hitForReview.hitDone?then('card-primary card-inverse','')}">
                    HitId: ${hitForReview.hitId}
                </div>
                <div class="card-block">
                    <div class="form-group row">
                        <label class="col-sm-2 col-form-label">Is done?</label>
                        <div class="col-sm-10">
                            <span class="form-control-static">${hitForReview.hitDone?c}</span>
                        </div>
                    </div>
                    <#if hitForReview.hitDone>

                        <div class="form-group row">
                            <label class="col-sm-2 col-form-label">Summary</label>
                            <div class="col-sm-10">
                                <span class="form-control-static">${hitForReview.nodeSummary}</span>
                            </div>
                        </div>

                        <label>Ranks</label>
                        <#list hitForReview.ranks as rankPair>
                            <div class="row mb-2">
                                <div class="col-sm-10">${rankPair.left}</div>
                                <div class="col-sm-2">${rankPair.right}</div>
                            </div>
                        </#list>


                        <fieldset class="form-group row">
                            <legend class="col-form-legend col-sm-2">Approve</legend>
                            <div class="form-check form-check-inline">
                                <label class="form-check-label">
                                    <input type="radio" name="${hitForReview.hitId}_approve" value="1"
                                           data-hitid="${hitForReview.hitId}"> Yes
                                </label>
                            <#--</div>-->
                            <#--<div class="form-check form-check-inline">-->
                                <label class="form-check-label">
                                    <input type="radio" name="${hitForReview.hitId}_approve" value="0"
                                           data-hitid="${hitForReview.hitId}"> No
                                </label>
                            </div>
                        </fieldset>
                        <div class="form-group row" id="${hitForReview.hitId}_reason" style="display: none">
                            <label class="col-sm-2 col-form-label">Reason</label>
                            <div class="col-sm-10">
                                <textarea name="${hitForReview.hitId}_reason" placeholder="Reason"
                                          class="form-control"></textarea>
                            </div>
                        </div>

                    </#if>
                    <input type="hidden" name="${hitForReview.hitId}_grades" value="${hitForReview.rankList}">
                    <input type="hidden" name="${hitForReview.hitId}_nodeid" value="${hitForReview.nodeId}">
                </div>
            </div>
        </div>
        <#sep><br></#sep>
    </#list>

    </form>
</div>
</body>
</html>