
package com.ubs.lem.release.manager.lib.task


/**
 * @author Valery Lavrentiev, valeriy.lavrentev@ubs.com
 *
 */
abstract class Task {

	abstract void executeBeforeChecks()

	abstract void executeCommand()

	abstract void executeAfterChecks()


	void run () {
		executeBeforeChecks()
		executeCommand()
		executeAfterChecks()
	}
}
