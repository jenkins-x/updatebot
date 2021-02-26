## UpdateBot

A bot for updating dependencies on your projects automatically

[![Javadocs](http://www.javadoc.io/badge/io.jenkins.updatebot/updatebot-core.svg?color=blue)](http://www.javadoc.io/doc/io.jenkins.updatebot/updatebot-core)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.jenkins.updatebot/updatebot-core/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/io.jenkins.updatebot/updatebot-core/)
![Apache 2](http://img.shields.io/badge/license-Apache%202-red.svg)

### Configuration

UpdateBot takes a simple YAML file to define which git repositories and github organisations to search for repositories to update.

See [an example UpdateBot YAML file](updatebot-core/src/test/resources/maven/updatebot.yml)

## Using UpdateBot

### Jenkins Pipelines

A good place to use UpdateBot is in your Continuous Delivery pipelines when you've just created a release, tagged the source code and have waited for the artifacts to be in maven central or your nexus/artifactory; then you want to push those new versions into your downstream projects via Pull Requests.

To do that please use the [UpdateBot Jenkins Plugin](https://wiki.jenkins.io/display/JENKINS/Updatebot+Plugin) or checkout the [UpdateBot Jenkins Plugin documentation](https://github.com/jenkinsci/updatebot-plugin/blob/master/readme.md).

Essentially once you have installed the [UpdateBot Jenkins Plugin](https://wiki.jenkins.io/display/JENKINS/Updatebot+Plugin)  into your Jeknins you just use the `updateBotPush()` step in your pipeline like this:

```groovy
node {

    stage('Release') { 
        git 'https://github.com/jstrachan-testing/updatebot-npm-sample.git'

        // TODO do the actual release first...
        
        // TODO wait for the release to be in maven central or npm or whatever...
    }

    stage('UpdateBot') {
        // now lets update any dependent projects with this new release
        // using the local file system as the tagged source code with versions
        updateBotPush()
    }
}
``` 

### Command Line

The updatebot jar file is a fat executable jar so you can use: 

    java -jar updatebot-${version}.jar
  
But the jar is also a unix binary so you can just run the following:

    ./updatebot-${version}.jar

To install on a unix operating system just copy the updatebot-${version).jar to file called `updatebot` on your `PATH`

## Kinds of update

There are different kinds of updates that UpdateBot can do. Lets walk through the kinds of updates you might want to do...

### Pushing

When you release an artifact its good practice to eagerly update all of the projects that use your artifact to use the new version via a Pull Request. Using a Pull Request means that this version change will trigger any Continuous Integration tests to validate the version change which also gives good feedback upstream to your project. It also lets downstream projects review and approve any version change.

To push versions from a repository just run the `push` command passing in the git clone URL or a local directory that contains a git clone.

    updatebot push --repo https://github.com/foo/bar.git 
    
You can specify a particular git commit reference (sha, branch, tag) via `--ref`   

    updatebot push --repo https://github.com/foo/bar.git --ref 1.2.3

This will then grab the source code for that repository and update its version in the downstream dependent projects.

When doing a CD pipeline you will typically have the git repository cloned locally already so you can just point to a local clone:
    
    updatebot push --dir /foo/bar

Or specifying the tag as well:

    updatebot push --dir /foo/bar  --tag 1.2.3
    

#### Pushing other dependency versions

Often projects have other dependencies such as shared libraries or packages. e.g. an npm project may have dependencies on angular packages.  

You may want to use a single project as your _exemplar_ project so that it defines a set of dependency versions; so that if they change in one repository then updatebot will replicate those changes into other repositories.

To push other versions from a repository we use the `push` object below, then we include language/framework specific dependency set definitions. In the case of `npm` we can specify lists of includes or excludes dependencies for `dependencies`, `devDependencies` or `peerDependencies`. You can use `*` too for a wildcard to make this YAML more DRY.
 
e.g. here's an example `updatebot.yml` file that sets up a repo called `ngx-base` as the exemplar project for all of its dependencies:

```yaml
github:
  organisations:
  - name: jstrachan-testing
    repositories:
    - name: ngx-base
      push:
        npm:
          dependencies:
            includes:
            - "*"
          devDependencies:
            includes:
            - "*"
    - name: ngx-widgets
```

Then when we run this command:

    updatebot push --repo https://github.com/jstrachan-testing/ngx-base
    
updatebot will look at all of those matching dependencies in the `ngx-base/package.json` and if they are different to the downstream dependencies it will generate a Pull Request.

e.g. here's an [example generated Pull Request on the ngx-widgets project](https://github.com/jstrachan-testing/ngx-widgets/pull/13)  where it generated a [single commit to update all the changed versions](https://github.com/jstrachan-testing/ngx-widgets/pull/13/commits/a3ade936a21c0f4727bcbad52e6ca227607d86e6)  
    
    
#### Pushing specific versions

Sometimes you just want to upgrade a specific version through your projects. To do this use the `push-version` command:

    updatebot push-version -k npm myapp 1.2.3
    
This will then iterate through all the projects defined by the configuration file you give it and generate the necessary code changes to adopt the new version and submit pull requests.    



### Pulling

We recommend `pushing` version changes eagerly in your CI / CD pipelines.

However projects often depend on lots of dependencies that are released upstream by different teams. So to pull version changes from upstream releases you can use the pull command:

    updatebot push -k npm 

This will then update any dependencies in your projects.

### Requirements

UpdateBot requires the following binaries to be available on your `PATH`

* java
* git

#### Node

To be able to pull version changes into your npm packages we use the [ncu](https://www.npmjs.com/package/npm-check-updates) CLI tool. You can install it via [these instructions](https://www.npmjs.com/package/npm-check-updates) or typing

    npm install -g npm-check-updates


### Docker

If you want to use UpdateBot inside a docker image you can reuse the [fabric8/maven-builder](https://hub.docker.com/r/fabric8/maven-builder/) image


    
      
  
