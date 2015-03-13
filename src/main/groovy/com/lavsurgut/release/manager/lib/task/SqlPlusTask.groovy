package com.lavsurgut.release.manager.lib.task

import groovy.util.logging.Log4j
import com.lavsurgut.release.manager.lib.task.Task


/**
 * @author Valery Lavrentiev, lavsurgut@gmail.com
 *
 */
@Log4j
class SqlPlusTask extends Task {


	String sqlplusExecutable = "sqlplus"
	String user
	String password
	String tnsName
	String dir
	String script
	/*String buffer out output*/
	String sqlPlusOutBuffer
	/*String buffer err output*/
	String sqlPlusErrBuffer

	String logResultOutput = ""

	SqlPlusTask(String user, String password, String tnsName, String script) {
		this.user = user
		this.password = password
		this.tnsName = tnsName
		this.script = script
	}



	private setServiceStmts() {
		//TODO: add in front whenever sqlerror oserror exit rollback
		int exit_is_there = 0
		int whenever_clause_is_there = 0
		def file = new File(script)

		def matcher

		file.eachLine { line ->
			if (!(matcher = line =~ /exit/))
				exit_is_there = 1
			if ((matcher = line =~ /whenever/))
				whenever_clause_is_there = 1
		}

		//if (whenever_clause_is_there == 0)
		//	file.

		if (exit_is_there == 0)
			file.append("\nexit;")

	}

	private void analyzeLog()
	{
		def negativeArray = ["ORA-", "SP2-", "EOF"]
		def matcher

		logResultOutput = ""

		negativeArray.each { i->
			if ((matcher = sqlPlusOutBuffer =~ i))
				logResultOutput = logResultOutput + "\nNUMBER OF KEY WORD " + i + " is " + matcher.getCount()

		}
		
		//Check if negative output was not found, raise an exception instead
		assert(logResultOutput.isEmpty())

	}
	//@Override
	void executeCommand()
	{

		def ant
		
		setServiceStmts()

		ant = new AntBuilder()
		ant.exec(outputproperty: "cmdOut",
		errorproperty: "cmdErr",
		resultproperty:"cmdExit",
		failonerror: "true",
		executable: "${sqlplusExecutable}") { arg(line: "-s ${user}/${password}@${tnsName} @${script}")}

		sqlPlusErrBuffer = ant.project.properties.cmdErr
		sqlPlusOutBuffer = ant.project.properties.cmdOut		
		
		log.info sqlplusExecutable + " output: " + "\n\n" + sqlPlusOutBuffer
		
		analyzeLog()
	}



	//@Override
	public void executeBeforeChecks() {
		null
		
	}



	//@Override
	public void executeAfterChecks() {
		null
		
	}



}