/**
 * 
 */
package com.lavsurgut.release.manager.lib.config

import groovy.util.logging.Log4j
//import oracle.jdbc.pool.OracleDataSource

import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import org.yaml.snakeyaml.Yaml

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
	String taskName
	String runOption
	String envName
	String version

	

	Closure registerTask
	Closure runTask

	ReleaseManagerContext() {

		this.binding = new Binding()
		this.scriptLogger = Logger.getLogger(this.class)
		this.groovyShell = new GroovyShell(binding)
		this.cli = new CliBuilder(usage: "groovy install.groovy -r <taskname> -e <envname>")
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

		defVars.keySet().each {setVars(it, defVars[it])}
		envVars.keySet().each {setVars(it, envVars[it])}
	}

	/*
	 * Db connection configuration
	 * */

/*	private void setupLemDbConnections () {
		
		Closure setupConnection = {obj, username, pass ->
			obj.user = username
			obj.password = pass
			obj.driverType = "thin"
			obj.serverName = binding.getVariable("lem_db_host")
			obj.portNumber = binding.getVariable("lem_db_port")
			obj.databaseName = binding.getVariable("lem_db")
		}

		lemStgDataSource = new OracleDataSource()
		lemExternalDataSource = new OracleDataSource()
		lemDboDataSource = new OracleDataSource()
		lemMdmDataSource = new OracleDataSource()
		lemRptDataSource = new OracleDataSource()

		lemStgDataSource.with {
			setupConnection(it, binding.getVariable("lem_stg_user"),binding.getVariable("lem_stg_user_pass"))
		}


		lemExternalDataSource.with {
			setupConnection(it, binding.getVariable("lem_ext_data_user"),binding.getVariable("lem_ext_data_user_pass"))
		}

		lemDboDataSource.with {
			setupConnection(it, binding.getVariable("lem_dbo_user"),binding.getVariable("lem_dbo_user_pass"))
		}

		lemMdmDataSource.with {
			setupConnection(it, binding.getVariable("lem_mdm_user"),binding.getVariable("lem_mdm_user_pass"))
		}

		lemRptDataSource.with {
			setupConnection(it, binding.getVariable("lem_rpt_user"),binding.getVariable("lem_rpt_user_pass"))
		}

		binding.setVariable("lemStgDataSource", lemStgDataSource)
		binding.setVariable("lemExternalDataSource", lemExternalDataSource)
		binding.setVariable("lemDboDataSource", lemDboDataSource)
		binding.setVariable("lemMdmDataSource", lemMdmDataSource)
		binding.setVariable("lemRptDataSource", lemRptDataSource)
	}
*/
	private void setupInputOptions (String[] args, Class<Object> installClass) {

		scriptDir = new File(installClass.protectionDomain.codeSource.location.path).parent + "/"

		File properties = new File(scriptDir + "properties/properties.yml")

		cli.r(longOpt: "run", args:1, argName:"taskname", "specify either 'all' or task name to run", required: true )
		cli.e(longOpt: "env", args:1, argName:"envname",  "specify env name to run on", required: true )

		OptionAccessor opt = cli.parse(args)

		if(!opt) {
			return
		}

		runOption = opt.getInner().getOptionValue("r")
		envName = opt.getInner().getOptionValue("e")

		setEnvVariablesFromYaml(properties, envName)
	}

	private void setupLogging () {

		Properties loggingProperties = new Properties()

		loggingProperties.load(new FileInputStream(scriptDir + "properties/log4j.properties"))

		PropertyConfigurator.configure(loggingProperties)

		binding.setVariable("log", scriptLogger)
	}

	private void setupUtilityFunctions() {
//TODO: clear up log <> scriptLogger convention 
		registerTask = { obj ->
			taskName = binding.getVariable("taskName")
			tasks.put(taskName, obj)
			log.info "Configured task " + taskName
		}
	}

	/*
	 *  Method configures all necessary properties for release scripts execution:
	 *  - version, logging, connections, scriptPath, scriptsMap, binding and 
	 *    necessary utility functions(closures)
	 */

	void setupContext (String[] args, Class<Object> installClass) {

		setupInputOptions(args, installClass)

		setupLogging()

//		setupLemDbConnections()
		
		setupUtilityFunctions()
		
		tasks = new LinkedHashMap()
		version = binding.getVariable("version")
	}
}
