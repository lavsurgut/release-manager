package com.lavsurgut.release.manager

import groovy.sql.Sql

import org.apache.log4j.Logger

import com.lavsurgut.release.manager.lib.config.ReleaseManagerContext
//


/***********************************************************************
 * 
 * Main variables configuration section
 * 
 ***********************************************************************/
ReleaseManagerContext ctx = new ReleaseManagerContext()

ctx.setupContext(args, getClass())

Logger log = ctx.scriptLogger
String runOption = ctx.runOption
String envName = ctx.envName
String version = ctx.version

/***********************************************************************
 * 
 * Scripts configuration section
 * 
 ***********************************************************************/


log.info "version: " + version

log.debug "runOption: " + runOption
log.debug "envName: " + envName

log.info "Running data load steps..."

ctx.registerTask("scripts/test/modify.groovy")
ctx.registerTask("scripts/test2/modify.groovy")
ctx.registerTask("scripts/test3/modify.groovy")
ctx.registerTask("scripts/test4/modify.groovy")

ctx.run()

log.info "Run has been completed"




