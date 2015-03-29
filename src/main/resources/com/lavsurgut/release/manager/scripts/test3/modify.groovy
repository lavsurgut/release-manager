

package com.lavsurgut.release.manager.scripts

import com.lavsurgut.release.manager.lib.task.SqlPlusTask
import com.lavsurgut.release.manager.lib.task.ParallelTask
import com.lavsurgut.release.manager.lib.task.Task
import groovy.sql.Sql

taskName = new File(getClass().protectionDomain.codeSource.location.path).getParentFile().getName()
taskPath = new File(getClass().protectionDomain.codeSource.location.path).getParent() + "/"

log.info "Configuring " + taskName + " ..."


Map tasks =
		["Task1" : new SqlPlusTask(user : "${relman.user}"
			, password : "${relman.password}"
			, tnsName : "${relman.databaseName}"
			, script : taskPath + "test2.sql"
			)
		
		,"Task2" : new SqlPlusTask(user : "${relman.user}"
			, password : "${relman.password}"
			, tnsName : "${relman.databaseName}"
			, script : taskPath + "test.sql")]

return new ParallelTask(runnables: tasks
					)


