/**
 * 
 */
package com.lavsurgut.release.manager.lib.config
import groovy.sql.Sql
import groovy.util.logging.Log4j

import javax.sql.DataSource

import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import org.yaml.snakeyaml.Yaml

import com.lavsurgut.release.manager.lib.task.ParallelTask
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

	Sql sql



	ReleaseManagerContext() {

		this.binding = new Binding()
		this.scriptLogger = Logger.getLogger(this.class)
		this.groovyShell = new GroovyShell(binding)
		this.cli = new CliBuilder()
	}

	private void installMetaDataTables () {
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

	private void setDefVariables(Object src) {

		LinkedHashMap defVars = src.getAt("default") as LinkedHashMap
		LinkedHashMap envDefVars = src.getAt("environments").getAt(envName).getAt("default") as LinkedHashMap
		LinkedHashMap dsDefVars = src.getAt("environments").getAt(envName).getAt("dataSources").getAt("default") as LinkedHashMap

		[defVars, envDefVars, dsDefVars].each { m ->
			m?.keySet().each {
				binding.setVariable(it, m[it])
			}
		}
	}

	private void setupDbConnections (Object src) {

		LinkedHashMap dataSources = src.getAt("environments").getAt(envName).getAt("dataSources") as LinkedHashMap

		dataSources.keySet().each { m->
			if (m != "default") {
				DataSource dataSource = this.class.classLoader.loadClass( dataSources[m].dataSourceDriverClass ?: binding.getVariable("dataSourceDriverClass")
						, true ).newInstance()
				dataSource.with { obj ->
					obj.user = dataSources[m].user ?: binding.getVariable("user")
					obj.password = dataSources[m].password ?: binding.getVariable("password")
					obj.driverType = dataSources[m].driverType ?: binding.getVariable("driverType")
					obj.serverName = dataSources[m].serverName ?: binding.getVariable("serverName")
					obj.portNumber = dataSources[m].portNumber ?: binding.getVariable("portNumber")
					obj.databaseName = dataSources[m].databaseName ?: binding.getVariable("databaseName")
				}
				binding.setVariable(m, dataSource)
			}
		}
	}

	/*
	 * Method sets up all necessary user variables from yaml properties file and populates them in
	 * binding.
	 * */
	private void setEnvVariablesFromYaml(File file, String envName) {
		Yaml yaml = new Yaml();
		InputStream input = new FileInputStream(file)

		def src = yaml.load(input)

		setDefVariables(src)

		setupDbConnections(src)
	}


	private void setupInputOptions (String[] args, Class<Object> installClass) {



		File properties = new File(scriptDir + "properties/properties.yml")
		cli.setUsage("groovy install.groovy -r <taskname> -e <envname> [-np]")
		cli.r(longOpt: "run", args:1, argName:"taskname","""specify either 'all' or task name to run
															task name can be provided in following forms:
															- "task" - simple one task execution
															- "task1,task2" -	run every task from task1 to task2
															- "task1,end"   -	run every task from task1 included till the end
															- "begin,task2" -	run every task from beginning to task2
															- "parallel_task(sub_task1[, sub_taskN])" - run sub_task1, sub_task2, ..., sub_taskN 
																										from parallel_task in parallel""", required: true )
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
	void registerTask (String scriptName) {
		groovyShell.evaluate(new File(scriptDir + scriptName)).with{
			String taskName = binding.getVariable("taskName")
			tasks.put(taskName, it)
			log.info "Configured task " + taskName
		}
	}

	/*Method determines the input option and executes the run task*/
	void run () {

		LinkedHashMap sbTasks = new LinkedHashMap()
		def matcher
		int lIndex, rIndex
		Map<String, Task> newRunnables = new HashMap<String, Task>()

		try {
			//if a range option specified
			if ((matcher = runOption =~ /^(.+)(,[^\(].+[^\)])$/)) {
				String firstOption = runOption.split(",")[0].trim()
				String secondOption = runOption.split(",")[1].trim()
				if (firstOption == "begin")
					lIndex = 0
				else
					lIndex = tasks.findIndexOf{it.key == firstOption}

				if (secondOption == "end")
					rIndex = tasks.size() - 1
				else
					rIndex = tasks.findIndexOf{it.key == secondOption}
				tasks.keySet().each { k ->
					if((tasks.findIndexOf{it.key == k} >= lIndex) &&
					(tasks.findIndexOf{it.key == k} <= rIndex))
						sbTasks.put(k, tasks[k])
				}
				if (sbTasks.size() == 0)
					throw new Exception("Cannot define a range for a task with name '" + taskName + "'")

			} else if ((matcher = runOption =~ /^(.+\()(.+)(\))$/)) {
				String taskName = matcher[0][1].replaceFirst(/\(/,"")
				Task task = tasks[taskName]
				if (task instanceof ParallelTask) {
					matcher[0][2].split(",").each {
						newRunnables.put(it, task.runnables[it])
					}
				} else
					throw new Exception("Cannot find a parallel task with name '" + taskName + "'")
				if (newRunnables.size() > 0)
					task.runnables = newRunnables
				else throw new Exception("Cannot find parallel task subtasks with defined names")

				sbTasks.put(taskName, task)
			}
			//if all tasks to run option
			else if (runOption == "all")
				sbTasks = tasks
			else
				//if exact task
				if (tasks[runOption])
					sbTasks.put(runOption, tasks[runOption])
				else
					throw new Exception("Cannot find a task with name '" + runOption + "'")

			if (sbTasks.size() > 0)
				sbTasks.keySet().each {
					runTask(sql, it, sbTasks[it], version)
				}
			
		}
		catch (Exception e) {
			log.error "Cannot run a task with '" + runOption + "' name because of errors"
			e.printStackTrace()
		}
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

		sql = new Sql(binding.getVariable("metaDataSource"))
		tasks = new LinkedHashMap()
		version = binding.getVariable("version")

		installMetaDataTables()
	}
}

