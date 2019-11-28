#
# Copyright 2016-2020 Micro Focus or one of its affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

java -Dstore.testcase.with.input=false -Dcaf.appname=test/${rootArtifactId} -Dconfig.path=test-configs -Dtask.template=template.yaml -Dinput.folder=test-data -Dexpected.folder=test-data -Ddocker.host.address=127.0.0.1 -Drabbitmq.node.port=5672 -Ddatastore.container.id=3e7a3c99b9a1486d9c20abe236cd1909 -Ddatastore.enabled=true -jar ../worker-tester.jar -w BatchWorker -g