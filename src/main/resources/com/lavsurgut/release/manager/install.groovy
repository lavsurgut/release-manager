package com.lavsurgut.release.manager

import groovy.sql.Sql
import oracle.jdbc.pool.OracleDataSource

import org.apache.log4j.Logger

import com.lavsurgut.release.manager.lib.config.ReleaseManagerContext
//


/***********************************************************************
 * 
 * Main variables configuration section
 * 
 ***********************************************************************/

OracleDataSource testSource

ReleaseManagerContext ctx = new ReleaseManagerContext()

ctx.setupContext(args, getClass())

Binding binding = ctx.binding

Closure setupDbConnections = {
	
		Closure setupConnection = { obj, username, pass ->
			obj.user = username
			obj.password = pass
			obj.driverType = "thin"
			obj.serverName = binding.getVariable("db_host")
			obj.portNumber = binding.getVariable("db_port")
			obj.databaseName = binding.getVariable("db")
		}
	
		testSource = new OracleDataSource()
		testSource.with {
			setupConnection(it, binding.getVariable("relman_user"),binding.getVariable("relman_user_pass"))
		}
		binding.setVariable("testSource", testSource)
	}
	

setupDbConnections()

Sql sql = new Sql(testSource)

ctx.installMetaDataTables(sql)

String scriptDir = ctx.scriptDir
GroovyShell groovyShell = ctx.groovyShell
Logger log = ctx.scriptLogger

Map tasks = ctx.tasks
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



groovyShell.evaluate(new File(scriptDir + "scripts/test/modify.groovy")).with{ctx.registerTask(it)}

ctx.run(sql)

log.info "Run has been completed"




