/**
 * 
 */
package com.lavsurgut.release.manager.lib.config

import groovy.sql.Sql
import groovy.util.logging.Log4j
import oracle.jdbc.pool.OracleDataSource

import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import org.yaml.snakeyaml.Yaml

import com.lavsurgut.release.manager.lib.task.Task

/**
 * @author Valery Lavrentiev, lavsurgut@gmail.com
 *
 */
@Log4j
class ReleaseManagerContext {
	/*Main Release Manager properties described here*/

	final Binding binding
	final GroovyShell groovyShell
	final Logger scriptLogger
	final CliBuilder cli

	String scriptDir
	LinkedHashMap tasks

	String runOption
	String envName
	String version

	Boolean noPromptOption

	OracleDataSource lemStgDataSource
	OracleDataSource lemExternalDataSource
	OracleDataSource lemDboDataSource
	OracleDataSource lemMdmDataSource
	OracleDataSource lemRptDataSource

	ReleaseManagerContext() {

		this.binding = new Binding()
		this.scriptLogger = Logger.getLogger(this.class)
		this.groovyShell = new GroovyShell(binding)
		this.cli = new CliBuilder()
	}

	void installMetaDataTables (Sql sql) {
		def tableExists = sql.firstRow """select count(1) cnt
													from user_tables 
													where table_name = 'RELEASE_REGISTER'"""
		def seqExists = sql.firstRow """select count(1) cnt
													from user_sequences
													where sequence_name = 'SEQ_RELEASE_REGISTER_ID'"""
		if (tableExists.cnt == 0) {
			sql.execute """
							create table RELEASE_REGISTER
							(release_register_id number
							,script_name varchar2(100 char) not null
							,version 	 varchar2(5 char) not null
							,update_date date default sysdate not null 
							,constraint PK_RELEASE_REGISTER primary key (release_register_id))
							"""
			sql.execute """
							create index IDX_RELEASE_REGISTER_DT on RELEASE_REGISTER(update_date)
							"""
			sql.execute """
							create unique index UDX_RELEASE_REGISTER_NAME on RELEASE_REGISTER(script_name, version)
							"""
		}
		if (seqExists.cnt == 0)
			sql.execute """
							create sequence SEQ_RELEASE_REGISTER_ID 
							"""
	}

	/*
	 * Method sets up all necessary user variables from yaml properties file and populates them in
	 * binding.
	 * */
	private void setEnvVariablesFromYaml(File file, String envName) {
		Yaml yaml = new Yaml();
		InputStream input = new FileInputStream(file)
		Closure setVars = { i, value ->
			binding.setVariable(i, value)
		}
		def src = (yaml.load(input))

		HashMap defVars = src.getAt("default") as HashMap
		HashMap envVars = src.getAt("environments").getAt(envName) as HashMap

		defVars.keySet().each {
			setVars(it, defVars[it])
		}
		envVars.keySet().each {
			setVars(it, envVars[it])
		}
	}



	private void setupInputOptions (String[] args, Class<Object> installClass) {



		File properties = new File(scriptDir + "properties/properties.yml")
		cli.setUsage("groovy install.groovy -r <taskname> -e <envname> [-np]")
		cli.r(longOpt: "run", args:1, argName:"taskname", "specify either 'all' or task name to run", required: true )
		cli.e(longOpt: "env", args:1, argName:"envname",  "specify env name to run on", required: true )
		cli.np(longOpt: "noprompt", args:0, "specify whether to ask for re-run confirmation or not - default is prompt", required: false )

		OptionAccessor opt = cli.parse(args)

		if(!opt) {
			System.exit(0)
		}

		runOption = opt.getInner().getOptionValue("r")
		envName = opt.getInner().getOptionValue("e")
		noPromptOption = opt.np?: false
		log.debug "noPromptOption: " + noPromptOption

		setEnvVariablesFromYaml(properties, envName)
	}

	private void setupLogging () {

		Properties loggingProperties = new Properties()

		loggingProperties.load(new FileInputStream(scriptDir + "properties/log4j.properties"))

		PropertyConfigurator.configure(loggingProperties)

		binding.setVariable("log", scriptLogger)
	}


	/*
	 *  Method runs a given task. It checks if it was executed and prompts a user if needed. It logs a task 
	 *  after execution into a special table 
	 */
	private void runTask(Sql sql, String taskName, Task task, String version) {
		String userChoice
		String runOption
		final String runExistingOption = "RUN_EXISTING"
		final String skipOption = "SKIP"
		final String runNewOption = "RUN_NEW"
		def console = System.console()
		def scriptExists = sql.firstRow """
											select count(1) cnt
											from RELEASE_REGISTER
											where script_name = ${taskName}
											 and version = ${version}
										   """

		if (scriptExists.cnt == 1 && !noPromptOption) {

			if (console) {
				userChoice = console.readLine("""> Script ${taskName} was already executed. Do you want to re-execute it? (Y,N): """)
				if (userChoice == "Y")
					runOption = runExistingOption
				else
					runOption = skipOption
			} else {
				log.error "Cannot get console."
				runOption = skipOption
			}
		} else if (scriptExists.cnt == 1 && noPromptOption)
			runOption = runExistingOption
		else
			runOption = runNewOption



		log.debug "runOption: " + runOption


		if (runOption == runExistingOption || runOption == runNewOption) {
			log.info "Running script ${taskName}..."
			task.run()
			log.info "Logging executed script ${taskName}..."
			if (runOption == runNewOption) {
				sql.execute """
						insert into RELEASE_REGISTER 
						values(SEQ_RELEASE_REGISTER_ID.nextVal, ${taskName}, ${version}, sysdate)
						"""
			}
			else if (runOption == runExistingOption) {
				sql.execute """
						update RELEASE_REGISTER 
						set update_date = sysdate
						where script_name = ${taskName}
						  and version = ${version}
						"""
			}
		} else log.info "Skipping script ${taskName}."
	}
	/*Method is used to add a task into tasks map*/
	void registerTask (Object obj) {
		String taskName = binding.getVariable("taskName")
		tasks.put(taskName, obj)
		log.info "Configured task " + taskName
	}

	/*Method determines the input option and executes the run task*/
	void run (Sql sql) {


		log.info "Running..."

		if (runOption == "all")
			tasks.keySet().each {
				runTask(sql, it, tasks[it], version)
			}
		else if (tasks[runOption])
			runTask(sql, runOption, tasks[runOption], version)
		else
			log.warn "Cannot find a task with '" + runOption + "' name"
	}

	/*
	 *  Method configures all necessary properties for release scripts execution:
	 *  - version, logging, connections, scriptPath, scriptsMap, binding.
	 *  Also checks if meta data tables exist and install them if needed.
	 */

	void setupContext (String[] args, Class<Object> installClass) {

		scriptDir = new File(installClass.protectionDomain.codeSource.location.path).parent + "/"

		setupLogging()

		setupInputOptions(args, installClass)

		tasks = new LinkedHashMap()
		version = binding.getVariable("version")
	}
}
