package com.ubs.lem.release.manager

import groovy.lang.Binding
import groovy.lang.Closure
import groovy.lang.GroovyShell
import groovy.util.CliBuilder

import java.util.LinkedHashMap

import org.apache.log4j.Logger

import com.ubs.lem.release.manager.lib.config.ReleaseManagerContext

/***********************************************************************
 * 
 * Main variables configuration section
 * 
 ***********************************************************************/

ReleaseManagerContext context = new ReleaseManagerContext()
context.setupContext(args, getClass())

String scriptDir = context.scriptDir
GroovyShell groovyShell = context.groovyShell
Logger log = context.scriptLogger
Binding binding = context.binding

Map tasks = context.tasks
String runOption = context.runOption
String envName = context.envName
String version = context.version

Closure registerTask = context.registerTask
Closure runTask = context.runTask

/***********************************************************************
 * 
 * Scripts configuration section
 * 
 ***********************************************************************/

log.info "version: " + version

log.debug "runOption: " + runOption
log.debug "envName: " + envName

log.info "Running data load steps..."

groovyShell.evaluate(new File(scriptDir + "scripts/JIRA-ID-00001/modify.groovy")).with{registerTask(it)}



log.info tasks.size()


switch ( runOption ) {
	case "all":
		tasks.keySet().each {
			tasks[it].run()
		}
	default:
		if (tasks[runOption])
			tasks[runOption].run()
		else
			log.warn "Cannot find a task with '" + runOption + "' name"
}





