import datetime
import json

from boto.mturk.connection import MTurkConnection
from boto.mturk.qualification import Qualifications, PercentAssignmentsApprovedRequirement, \
    NumberHitsApprovedRequirement, Requirement
from boto.mturk.question import HTMLQuestion
import jsonpickle
import sys


input_file_name = sys.argv[1]
output_file_name = sys.argv[2]

input_file = open(input_file_name, "r")
raw_input_json = input_file.read()
input_file.close()

input_json = json.loads(raw_input_json)

aws_key = input_json['aws_key']
aws_secret_key = input_json['aws_secret_key']
is_sandbox = input_json['is_sandbox']

request = input_json['request'].lower()

mtc = MTurkConnection(
    aws_access_key_id=aws_key,
    aws_secret_access_key=aws_secret_key,
    host="mechanicalturk.sandbox.amazonaws.com" if is_sandbox else "not-working-mechanicalturk.amazonaws.com")


# question_html = raw_input()
# title = raw_input()
# description = raw_input()
# reward = raw_input()

def get_qualification(is_sandbox):
    qualifications = Qualifications()
    if not is_sandbox:
        qualifications.add(PercentAssignmentsApprovedRequirement("GreaterThanOrEqualTo", integer_value=95))
        qualifications.add(NumberHitsApprovedRequirement("GreaterThanOrEqualTo", integer_value=1000))
        qualifications.add(MasterRequirement(sandbox=is_sandbox))

    return qualifications


class MasterRequirement(Requirement):
    def __init__(self, sandbox=False, required_to_preview=False):
        comparator = "Exists"
        sandbox_qualification_type_id = "2ARFPLSP75KLA8M8DH1HTEQVJT3SY6"
        production_qualification_type_id = "2F1QJWKUDD8XADTFD2Q0G6UTO95ALH"
        qualification_type_id = production_qualification_type_id if not sandbox else sandbox_qualification_type_id
        super(MasterRequirement, self).__init__(qualification_type_id=qualification_type_id, comparator=comparator,
                                                required_to_preview=required_to_preview)


request_data = input_json['request_data']

if request == 'create_hit':
    result_set = mtc.create_hit(
        question=HTMLQuestion(request_data['question_html'], 600),
        title=request_data['title'],
        description=request_data['description'],
        reward=request_data['reward'],
        duration=datetime.timedelta(minutes=request_data['duration_minutes']),
        lifetime=datetime.timedelta(minutes=request_data['lifetime_minutes']),
        max_assignments=request_data['number_of_duplications'],
        keywords=request_data['keywords_array'],
        qualifications=get_qualification(is_sandbox),
        annotation=request_data['annotation']
    )
    result = result_set[0]

if request == 'get_hit':
    result_set = mtc.get_hit(request_data['hit_id'])
    result = result_set[0]

if request == 'get_results':
    result_set = mtc.get_assignments(request_data['hit_id'])
    result = result_set # ['GetAssignmentsForHITResponse']

if request == 'dispose':
    result_set = mtc.dispose_hit(request_data['hit_id'])
    result = result_set['DisposeHITResponse']['DisposeHITResult']

if request == 'disable':
    result_set = mtc.disable_hit(request_data['hit_id'])
    result = result_set['DisableHITResponse']['DisableHITResult']

if request == 'start_review':
    result_set = mtc.set_reviewing(request_data['hit_id'])
    result = result_set

if request == 'stop_review':
    result_set = mtc.set_reviewing(request_data['hit_id'], revert=True)
    result = result_set

if request == 'allow_more_assignment':
    result_set = mtc.extend_hit(request_data['hit_id'], 1)
    result = result_set


j = json.loads(jsonpickle.encode(result))
del j['py/object']
output_str = json.dumps(j)
output_file = open(output_file_name, "w")
output_file.write(output_str)
output_file.close()
# print json.dumps(j)
