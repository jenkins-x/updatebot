#!/usr/bin/groovy
/**
 * Copyright (C) 2011 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
def stage(){
  return stageProject{
    project = 'fabric8io/updatebot'
    useGitTagForNextVersion = true
  }
}

def approveRelease(project){
  def releaseVersion = project[1]
  approve{
    room = null
    version = releaseVersion
    console = null
    environment = 'fabric8'
  }
}

def release(project){
  releaseProject{
    stagedProject = project
    useGitTagForNextVersion = true
    helmPush = false
    groupId = 'io.fabric8.updatebot'
    githubOrganisation = 'fabric8io'
    artifactIdToWatchInCentral = 'updatebot'
    artifactExtensionToWatchInCentral = 'pom'
  }
}

def mergePullRequest(prId){
  mergeAndWaitForPullRequest{
    project = 'fabric8io/updatebot'
    pullRequestId = prId
  }
}

def updateDownstreamDependencies(stagedProject) {
  pushDockerfileEnvVarChangePR {
    propertyName = 'UPDATEBOT_VERSION'
    projects = [
            'fabric8io/updatebot'
    ]
    version = stagedProject[1]
  }
}
return this;
