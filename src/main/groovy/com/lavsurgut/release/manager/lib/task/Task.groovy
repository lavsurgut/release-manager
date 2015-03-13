
package com.ubs.lem.release.manager.lib.task


/**
 * @author Valery Lavrentiev, valeriy.lavrentev@ubs.com
 *
 */
abstract class Task {

	Closure executeBeforeChecks

	Closure executeAfterChecks

	abstract void executeCommand()

	void run () {
		executeBeforeChecks?.doCall()
		executeCommand()
		executeAfterChecks?.doCall()
	}
}

