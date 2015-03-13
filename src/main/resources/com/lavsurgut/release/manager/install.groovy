package com.lavsurgut.release.manager

import java.util.HashMap

import groovy.sql.Sql

import org.apache.log4j.Logger

import com.lavsurgut.release.manager.lib.config.ReleaseManagerContext
import com.lavsurgut.release.manager.lib.task.Task
//


/***********************************************************************
 * 
 * Main variables configuration section
 * 
 ***********************************************************************/

ReleaseManagerContext ctx = new ReleaseManagerContext()
ctx.setupContext(args, getClass())

String scriptDir = ctx.scriptDir
GroovyShell groovyShell = ctx.groovyShell
Logger log = ctx.scriptLogger
Binding binding = ctx.binding

Map tasks = ctx.tasks
String runOption = ctx.runOption
String envName = ctx.envName
String version = ctx.version

Sql sql = new Sql(binding.getVariable("lemStgDataSource"))

/***********************************************************************
 * 
 * Scripts configuration section
 * 
 ***********************************************************************/


log.info "version: " + version

log.debug "runOption: " + runOption
log.debug "envName: " + envName

log.info "Running data load steps..."



groovyShell.evaluate(new File(scriptDir + "scripts/test/modify.groovy")).with{ctx.registerTask(it)}


if (runOption == "all")
	tasks.keySet().each {
		ctx.runTask(sql, it, tasks[it], version)
	}
else if (tasks[runOption])
	ctx.runTask(sql, runOption, tasks[runOption], version)
else
	log.warn "Cannot find a task with '" + runOption + "' name"


log.info "Run has been completed"




