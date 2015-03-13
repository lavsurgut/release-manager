/**
 * 
 */
package com.lavsurgut.release.manager.lib.task

import groovy.util.logging.Log4j

/**
 * @author Valery Lavrentiev, valeriy.lavrentev@ubs.com
 *
 */
@Log4j
class ShellScriptTask extends Task {

	String argumentsLine
	String script
	String scriptErrBuffer
	String scriptOutBuffer

	@Override
	void executeCommand() {

		AntBuilder ant


		ant = new AntBuilder()
		ant.exec(outputproperty: "cmdOut",
		errorproperty: "cmdErr",
		resultproperty:"cmdExit",
		failonerror: "true",
		executable: "${script}") { arg(line: "${argumentsLine}")}

		scriptErrBuffer = ant.project.properties.cmdErr
		scriptOutBuffer = ant.project.properties.cmdOut

		log.info "script output: " + "\n\n" + scriptOutBuffer
	}
}
