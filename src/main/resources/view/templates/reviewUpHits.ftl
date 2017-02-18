<!DOCTYPE html>
<html>
<head>
    <title>Review Up Hits</title>
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
        })
    </script>
</head>
<body>
<div class="container-fluid">
    <h1>Review UP hits</h1>
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
                    <label class="col-sm-1 form-control-label">Is done?</label>
                    <div class="col-sm-11 form-control-static">${hitForReview.hitDone?c}</div>
                </div>

                <#if hitForReview.hitDone>
                    <div class="form-group row">
                        <label class="col-sm-1 form-control-label">Task text</label>
                        <div class="col-sm-11">
                            <span class="form-control-static">${hitForReview.taskText}</span>
                        </div>
                    </div>
                    <hr/>
                    <div class="form-group row">
                        <label class="col-sm-1 form-control-label">Summary</label>
                        <div class="col-sm-11">
                            <span class="form-control-static">${hitForReview.summary}</span>
                        </div>
                    </div>
                    <div class="form-group row">
                        <label class="col-sm-1 form-control-label">Approve:</label>
                        <div class="col-sm-11 form-control">
                            <label><input type="radio" name="${hitForReview.hitId}_approve" value="1"
                                          data-hitid="${hitForReview.hitId}"> Yes</label>
                            <label><input type="radio" name="${hitForReview.hitId}_approve" value="0"
                                          data-hitid="${hitForReview.hitId}"> No</label>
                            <div id="${hitForReview.hitId}_reason" style="display: none">
                                <textarea autocomplete="off" name="${hitForReview.hitId}_reason"
                                          placeholder="Reason"></textarea>
                            </div>
                        </div>
                    </div>
                </#if>
                <input type="hidden" name="${hitForReview.hitId}_chosenChildrenSummaries"
                       value="${hitForReview.chosenChildrenSummariesJsonBase64}">
                <input type="hidden" name="${hitForReview.hitId}_nodeid" value="${hitForReview.nodeId}">
                <input type="hidden" name="${hitForReview.hitId}_summary" value="${hitForReview.summaryBase64}">
            </div>
        </div>
        <#sep><br></#sep>
    </#list>

    </form>
</div>
</body>
</html>