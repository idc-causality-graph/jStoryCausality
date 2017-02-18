<#macro unaccept_banner>
<div id="unaccept_banner"
     class="alert alert-danger"
     style="display: none"
     role="alert">You must ACCEPT the HIT before you can submit the results.
</div>
</#macro>

<#macro assignment_hidden>
<input type='hidden' value='' name='assignmentId' id='assignmentId'/>
</#macro>

<#macro submit_button>
<p class="text-center">
    <input type='submit' id='submitButton' value='Submit' class="btn btn-primary"/>
</p>
</#macro>

<#macro aws_script>
<script language='Javascript'>turkSetAssignmentID();</script>
</#macro>

<#macro external_scripts>
<script src='https://s3.amazonaws.com/mturk-public/externalHIT_v1.js'
        type='text/javascript'></script>
<script src="https://code.jquery.com/jquery-3.1.1.slim.min.js"
        integrity="sha256-/SIrNqv8h6QGKDuNoLGA4iret+kyesCkHGzVUUV0shc="
        crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-validate/1.16.0/jquery.validate.min.js"
        integrity="sha256-UOSXsAgYN43P/oVrmU+JlHtiDGYWN2iHnJuKY9WD+Jg="
        crossorigin="anonymous"></script>
<link href="https://s3.amazonaws.com/mturk-public/bs30/css/bootstrap.min.css" rel="stylesheet"/>
<style type="text/css">
    section#other {
        margin-bottom: 15px;
        padding: 10px 10px;
        font-family: Verdana, Geneva, sans-serif;
        color: #333333;
        font-size: 0.9em;
    }
</style>
</#macro>

<#macro bootup_script>
<script type="text/javascript">
    $(function () {
        var assignmentID = turkGetParam("assignmentId", "");
        if (assignmentID == "ASSIGNMENT_ID_NOT_AVAILABLE") {
            if (disable_form){
                disable_form();
            }
            $("#unaccept_banner").show();
        }
        $("#mturk_form").validate({
            errorPlacement: function (error, element) {
                var placement = $(element).data('error');
                if (placement) {
                    $(placement).append(error);
                    $(placement).show();
                } else {
                    error.insertAfter(element);
                }
            }
        });
    });
</script>
</#macro>