
package com.lavsurgut.release.manager.lib.task


/**
 * @author Valery Lavrentiev, lavsurgut@gmail.com
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

