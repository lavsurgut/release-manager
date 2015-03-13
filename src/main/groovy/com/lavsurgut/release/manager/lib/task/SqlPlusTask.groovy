package com.ubs.lem.release.manager.lib.task

import groovy.util.logging.Log4j



/**
 * @author Valery Lavrentiev, valeriy.lavrentev@ubs.com
 *
 */
@Log4j
class SqlPlusTask extends Task {


	final String sqlplusExecutable = "sqlplus"
	String user
	String password
	String tnsName
	String dir
	String script
	
	String sqlPlusOutBuffer
	
	String sqlPlusErrBuffer

	String logResultOutput 

	String negativePatternsList


	private void setServiceStmts() {
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

		def matcher
		logResultOutput = ""


		negativePatternsList ?:  ["ORA-", "SP2-", "EOF"].each { i->
			if ((matcher = sqlPlusOutBuffer =~ i))
				logResultOutput = logResultOutput + "\nNUMBER OF KEY WORD " + i + " is " + matcher.getCount()

		}

		//Check if negative output was not found, raise an exception instead
		assert(logResultOutput.isEmpty())

	}
	@Override
	void executeCommand()
	{

		AntBuilder ant

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

}
